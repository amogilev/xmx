package am.xmx.core;


import am.specr.SpeculativeProcessorFactory;
import am.xmx.boot.IXmxBootService;
import am.xmx.cfg.IAppPropertiesSource;
import am.xmx.cfg.IXmxConfig;
import am.xmx.cfg.Properties;
import am.xmx.core.jmx.JmxSupport;
import am.xmx.core.type.IMethodInfoService;
import am.xmx.core.type.MethodInfoServiceImpl;
import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxObjectDetails.FieldInfo;
import am.xmx.dto.XmxObjectDetails.MethodInfo;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;
import am.xmx.server.IXmxServerLauncher;
import am.xmx.service.IXmxService;
import com.gilecode.yagson.YaGson;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static am.xmx.service.XmxUtils.safeToString;

public final class XmxManager implements IXmxService, IXmxBootService {

	private final static Logger logger = LoggerFactory.getLogger(XmxManager.class);

	// how many milliseconds to wait before starting embedded web UI
	private static final int UI_START_DELAY = 10000;

	private static final String LAUNCHER_CLASS_ATTR = "XMX-Server-Launcher-Class";

	// initialized and overridden in the constructor
	private final IXmxConfig config;
	private MBeanServer jmxServer;

	XmxManager(IXmxConfig config) {
		this.config = config;
		if (isEnabled()) {
			startCleanerThreads();
			if (config.getSystemProperty(Properties.GLOBAL_JMX_ENABLED).asBool()) {
				// TODO maybe create a custom server instead, with custom connectors etc.
				jmxServer = ManagementFactory.getPlatformMBeanServer();
				logger.debug("JMX Bridge is started");
			}
			if (config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_ENABLED).asBool()) {
				startUI();
			}
		} else {
			logger.warn("XMX functionality is disabled by configuration");
		}
	}

	private static final YaGson jsonMapper = new YaGson();

	private SpeculativeProcessorFactory<IWebappNameExtractor> extractorsFactory =
			new SpeculativeProcessorFactory<>(IWebappNameExtractor.class);
	{
		extractorsFactory.registerProcessor(
				"am.xmx.core.Tomcat7WebappNameExtractor",
				"org.apache.catalina.loader.WebappClassLoader");
		extractorsFactory.registerProcessor(
				"am.xmx.core.Tomcat8WebappNameExtractor",
				"org.apache.catalina.loader.WebappClassLoaderBase");
		extractorsFactory.registerProcessor(
				true, // Jetty's WebAppClassLoader shadows all server classes, need to check its parent
				"am.xmx.core.JettyWebappNameExtractor",
				"org.eclipse.jetty.webapp.WebAppClassLoader");
	}

	private IMethodInfoService methodInfoService = new MethodInfoServiceImpl();
	
	/**
	 * Storage of weak references to each managed objects, mapped by object ID
	 */
	private Map<Integer, ManagedObjectWeakRef> objectsStorage = new HashMap<>(64*1024);
	
	/**
	 * Generator of unique IDs for managed objects.
	 */
	private AtomicInteger managedObjectsCounter = new AtomicInteger();
	
	/**
	 * Storage of classes info for managed objects
	 */
	private ConcurrentMap<Integer, ManagedClassInfo> classesInfoById = new ConcurrentHashMap<>(32*1024);
	
	/**
	 * Maps appName -> appInfo for each app with managed classes
	 */
	private ConcurrentMap<String, ManagedAppInfo> appInfosByName = new ConcurrentHashMap<>();
	
	/**
	 * Generator of unique IDs for classes of managed objects.
	 */
	private AtomicInteger managedClassesCounter = new AtomicInteger();
	
	/**
	 * Generator of unique IDs for managed applications.
	 */
	private AtomicInteger managedAppsCounter = new AtomicInteger();
	
	private ReferenceQueue<Object> managedObjectsRefQueue = new ReferenceQueue<>();
	
	ReferenceQueue<ClassLoader> managedClassLoadersRefQueue = new ReferenceQueue<>();
	
	private void startCleanerThreads() {
		Thread objCleanerThread = new Thread("XMX-ObjCleaner") {
			@Override
			public void run() {
				while (true) {
					try {
						ManagedObjectWeakRef objRef = (ManagedObjectWeakRef) managedObjectsRefQueue.remove();
						synchronized(this) {
							ManagedClassInfo classInfo = objRef.classInfo;
							Set<Integer> objectIds = classInfo.getObjectIds();
							int objectId = objRef.objectId;
							if (objectIds != null) {
								objectIds.remove(objectId);
							}
							objectsStorage.remove(objectId);
							logger.debug("Clean GC'ed object id={} of class {}", objectId, classInfo.getClassName());

							if (objRef.jmxObjectName != null) {
								JmxSupport.unregisterBean(jmxServer, objRef.jmxObjectName);
								logger.debug("Unregistered bean {}", objRef.jmxObjectName);
							}
							if (objectIds != null && objectIds.isEmpty()) {
								// reset and init are synchronized on classInfo, so there is no race 
								classInfo.reset();
								logger.debug("Reset class {} (classId={})", classInfo.getClassName(), classInfo.getId());
							}
						}
					} catch (InterruptedException e) {
					}
				}
			}
		};
		objCleanerThread.setDaemon(true);
		objCleanerThread.start();
		
		Thread classCleanerThread = new Thread("XMX-ClassCleaner") {
			@Override
			public void run() {
				while (true) {
					try {
						ManagedClassLoaderWeakRef loaderInfo = (ManagedClassLoaderWeakRef) managedClassLoadersRefQueue.remove();
						Collection<Integer> classIdsToRemove = loaderInfo.getClassIdsByName().values();
						classesInfoById.keySet().removeAll(classIdsToRemove);
						loaderInfo.getAppInfo().removeManagedClassIds(classIdsToRemove);
						if (logger.isDebugEnabled()) {
							for (Integer classId : classIdsToRemove) {
								logger.debug("Clean GC'ed classId={}", classId);
							}
						}

					} catch (InterruptedException e) {
					}
				}
			}
		};
		classCleanerThread.setDaemon(true);
		classCleanerThread.start();
	}
	
	// Inner Implementation of XmxServiceEx API

	@Override
	public boolean isEnabled() {
		return config.getSystemProperty(Properties.GLOBAL_ENABLED).asBool();
	}
	
	/**
	 * Registers a managed object into XMX system.
	 * A new unique ID is generated for an object, and a weak reference to the object is saved into the storage.
	 */
	@Override
	public void registerObject(Object obj, int classId) {
		try {
			_registerObject(obj, classId);
		} catch (RuntimeException e) {
			// not really expected. Try-catch used to make sure that XMX bugs do not break users' apps
			logger.error("Failed to register object, classId={}", classId, e);
		}
	}

	private void _registerObject(Object obj, int classId) {
		Class<?> objClass = obj.getClass();
		
		ManagedClassInfo classInfo = classesInfoById.get(classId);
		if (classInfo == null) {
			// not managed anymore
			return;
		}
		if (!classInfo.getClassName().equals(objClass.getName())) {
			// invoked from a constructor of some superclass
			// wait until invoked from actual class (or not invoked if it is not managed)
			return;
		}
		if (classInfo.isDisabled()) {
			// management temporarily disabled
			return;
		}
		// ok, class id corresponds to the actual class

		synchronized(this) {
			if (!classInfo.isInitialized()) {
				initClassInfo(objClass, classInfo);
			}
			Set<Integer> objectIds = classInfo.getObjectIds();
			
			/* - not required anymore, as potential sources if duplicate registration are eliminated;  
			// check if object is already registered, e.g. from another constructor
			for (Integer id : objectIds) {
				// check all registered objects of this class
				ManagedObjectWeakRef ref = objectsStorage.get(id);
				if (ref != null) {
					Object existingObj = ref.get();
					if (existingObj == obj) {
						// already registered, skip
						return;
					}
				}
			}*/
			
			int otherInstancesCount = objectIds.size();
			if (otherInstancesCount >= classInfo.getMaxInstances()) {
				// limit exceeded
				if (!classInfo.isDisabledByMaxInstances()) {
					classInfo.setDisabledByMaxInstances(true);
					logger.debug("Max instances exceeded for class {} (classId={})", classInfo.getClassName(), classId);
				}
				return;
			}
			
			// not registered yet, store internally and optionally register as JMX bean
			int objectId = managedObjectsCounter.getAndIncrement();
			ObjectName jmxObjectName = null;
			if (jmxServer != null && classInfo.getJmxClassModel() != null) {
				// register as JMX bean
				jmxObjectName = JmxSupport.registerBean(this, jmxServer, objectId, classInfo, otherInstancesCount == 0);

				if (otherInstancesCount == 1) {
					// check if another instance is registered in JMX as singleton. If so,
					// re-register it with id
					int anotherObjectId = objectIds.iterator().next();
					ManagedObjectWeakRef anotherObject = objectsStorage.get(anotherObjectId);
					ObjectName anotherObjectName = anotherObject.jmxObjectName; 
					if (anotherObjectName.getKeyProperty("id") == null) {
						// re-register as non-singleton
						JmxSupport.unregisterBean(jmxServer, anotherObjectName);
						anotherObject.jmxObjectName = JmxSupport.registerBean(this, jmxServer, anotherObjectId, classInfo, false);
					}
				}
			}
			
			// store internally
			objectsStorage.put(objectId, new ManagedObjectWeakRef(obj, managedObjectsRefQueue, 
					objectId, classInfo, jmxObjectName));
			objectIds.add(objectId);
			if (logger.isDebugEnabled()) {
				logger.debug("Registered new instance objId={} for class {} (classId={})", objectId,
						classInfo.getClassName(), classId);
			}
		}
	}

	private void initClassInfo(Class<?> objClass, ManagedClassInfo info) {
		if (!info.isInitialized()) {
			String appName = info.getAppInfo().getName();
			List<Method> managedMethods = getManagedMethods(objClass);
			List<Field> managedFields = getManagedFields(objClass);

			ModelMBeanInfoSupport jmxClassModel = null;
			if (jmxServer != null) {
				jmxClassModel = JmxSupport.createModelForClass(objClass, appName, managedMethods, managedFields, config);
			}

			info.init(managedMethods, managedFields, jmxClassModel);
			logger.debug("Initialized class info for class (classId={})", info.getClassName(), info.getId());
		}
	}

	private WeakHashMap<ClassLoader, String> appNameByLoader = new WeakHashMap<>();
	private String obtainAppNameByLoader(ClassLoader loader) {
		if (loader == null) {
			return "";
		}
		String name = appNameByLoader.get(loader);
		if (name == null) {
			name = extractAppNameByLoader(loader);
			appNameByLoader.put(loader, name);
		}
		return name;
	}

	private String extractAppNameByLoader(ClassLoader loader) {
		List<IWebappNameExtractor> extractors = extractorsFactory.getProcessorsFor(loader);
		if (extractors != null) {
			for (IWebappNameExtractor extractor : extractors) {
				String name = extractor.extract(loader);
				if (name != null) {
					return name.isEmpty() ? "ROOT" : name;
				}
			}
		}
			
		// no extractor found, or all failed
		return "";
	}
	
	private int getMaxInstances(IAppPropertiesSource appConfig, String className) {
		int maxInstances = appConfig.getClassProperty(className, Properties.CLASS_MAX_INSTANCES).asInt();
		if (maxInstances < 0) {
			maxInstances = Integer.MAX_VALUE;
		}
		return maxInstances;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transformClassIfInterested(ClassLoader classLoader, String bcClassName, 
			byte[] classBuffer, Class<?> classBeingRedefined) {
		try {
			return _transformClassIfInterested(classLoader, bcClassName, classBuffer, classBeingRedefined);
		} catch (RuntimeException e) {
			logger.error("Failed to register class {}", bcClassName, e);
			return classBuffer;
		}
	}

	private byte[] _transformClassIfInterested(ClassLoader classLoader, String bcClassName,
			byte[] classBuffer, Class<?> classBeingRedefined) {
		String appName = obtainAppNameByLoader(classLoader);
		IAppPropertiesSource appConfig = config.getAppConfig(appName);
		
		// convert names obtained from byte-code to Java format (one corresponding to Class.getName())
		String className = bcClassName.replace('/', '.');
		
		boolean isManaged = appConfig.getAppProperty(Properties.APP_ENABLED).asBool() &&
				config.getAppConfig(appName).getClassProperty(className, Properties.SP_MANAGED).asBool();

		if (!isManaged) {
			return classBuffer;
		}

		ClassReader cr = new ClassReader(classBuffer);
		int access = cr.getAccess();
		if ((access & (Opcodes.ACC_ENUM | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) > 0) {
			// only instrument non-abstract classes
			return classBuffer;
		}

		ManagedAppInfo appInfo = getOrInitAppInfo(appName);
		ManagedClassLoaderWeakRef classLoaderInfo = appInfo.getOrInitManagedClassLoaderInfo(classLoader, managedClassLoadersRefQueue);
		
		int classId;
		if (classBeingRedefined != null) {
			// currently the hot code replacement cannot add, remove or change signature of fields and methods
			// so, we can continue using existing ManagedClassInfo
			// May require re-visiting in Java 9 and further!
			
			ManagedClassInfo classInfo = getManagedClassInfo(classLoaderInfo, classBeingRedefined.getName());
			assert classInfo != null : "Should have been transformed already: " + classBeingRedefined;
			classId = classInfo.getId();
		} else {
			// initialize known properties of managed class, e.g. class ID and name
			// other properties may require Class itself, and will be initialized later
			classId = managedClassesCounter.getAndIncrement();
			int maxInstances = getMaxInstances(appConfig, className);
			String jmxObjectNamePart = null;
			if (jmxServer != null) {
				jmxObjectNamePart = JmxSupport.createClassObjectNamePart(className, appName);
				assert jmxObjectNamePart != null; 
			}
			ManagedClassInfo classInfo = new ManagedClassInfo(classId, className, classLoaderInfo, 
					appInfo, maxInstances, jmxObjectNamePart);
			
			classesInfoById.put(classId, classInfo);
			classLoaderInfo.getClassIdsByName().put(className, classId);
			appInfo.addManagedClassId(classId);
		}
		String action = classBeingRedefined == null ? "transformClass" : "re-transformClass";
		if (logger.isDebugEnabled()) {
			logger.info("{}: {} (classId={})", action, className, classId);
		} else {
			logger.info("{}: {}", action, className);
		}
		
		// actually transform the class - add registerObject to constructors
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		XmxInstructionsAdder xmxInstructionsAdder = new XmxInstructionsAdder(cw, classId, bcClassName);

		cr.accept(xmxInstructionsAdder, 0);
		return cw.toByteArray();
	}

	// Inner Implementation of XmxService API
	
	private ManagedAppInfo getOrInitAppInfo(String appName) {
		ManagedAppInfo appInfo = appInfosByName.get(appName);
		if (appInfo != null) {
			return appInfo;
		}
		
		// writes of new app info are rare, so synchronization is fine
		// we use it to avoid duplicate app info initialization and keep counter consistent
		synchronized(this) {
			appInfo = appInfosByName.get(appName);
			if (appInfo != null) {
				return appInfo;
			}
			
			int appId = managedAppsCounter.getAndIncrement();
			appInfo = new ManagedAppInfo(appId, appName);
			appInfosByName.put(appName, appInfo);

			logger.debug("Initialized AppInfo appId={} for appName={}", appId, appName);

			return appInfo;
		}
	}

	/**
	 * Returns names (contexts) of all recognized web applications.
	 */
	@Override
	synchronized public List<String> getApplicationNames() {
		List<String> result = new ArrayList<>(appInfosByName.size());
		for (ManagedAppInfo appInfo : appInfosByName.values()) {
			result.add(appInfo.getName());
		}
		return result;
	}
	
	/**
	 * Finds information about the classes of the managed objects by application name
	 * and class name pattern. If both parameters are null, returns all registered classes.
	 * 
	 * @param appNameOrNull the application name as returned by {@link #getApplicationNames()}, or null
	 * @param classNamePatternOrNull the class name, pattern, or null
	 * 
	 * @return matching classes information
	 */
	@Override
	synchronized public List<XmxClassInfo> findManagedClassInfos(String appNameOrNull, String classNamePatternOrNull) {
		List<XmxClassInfo> result = new ArrayList<>();
		Pattern classNamePattern = classNamePatternOrNull == null ? null : Pattern.compile(classNamePatternOrNull);
		if (appNameOrNull != null) {
			ManagedAppInfo appInfo = appInfosByName.get(appNameOrNull);
			if (appInfo != null) {
				fillXmxClassInfo(result, appInfo.getManagedClassIds(), classNamePattern);
			}
		} else {
			fillXmxClassInfo(result, classesInfoById.keySet(), classNamePattern);
		}
		
		return result;
	}
	
	/**
	 * Returns all 'live' objects info for the specified class ID, which may be obtained from classes info
	 * return by {@link #findManagedClassInfos(String, String)}.
	 *  
	 * @param classId unique class ID (or null to return all objects)
	 */
	@Override
	synchronized public List<XmxObjectInfo> getManagedObjects(Integer classId) {
		List<XmxObjectInfo> result = new ArrayList<>();
		
		if (classId != null) {
			fillLiveObjects(result, classId);
		} else {
			for (Integer id : classesInfoById.keySet()) {
				fillLiveObjects(result, id);
			}
		}
		
		return result;
	}
	
	/**
	 * Obtains the details of the object by its ID, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *  
	 * @param objectId the unique object ID
	 */
	@Override
	public XmxObjectDetails getObjectDetails(int objectId) {
		
		Object obj = getObjectById(objectId);
		if (obj == null) {
			return null;
		}
		
		List<String> classNames = new ArrayList<>();
		Map<String, List<FieldInfo>> fieldsByClass = new LinkedHashMap<>();
		Map<String, List<MethodInfo>> methodsByClass = new LinkedHashMap<>();
		
		
		Class<?> clazz = obj.getClass();
		ManagedClassInfo classInfo = getManagedClassInfo(clazz);

		// fill fields
		List<Field> managedFields = classInfo.getManagedFields();
		for (int fieldId = 0; fieldId < managedFields.size(); fieldId++) {
			Field f = managedFields.get(fieldId);
			String declaringClassName = f.getDeclaringClass().getName();
			
			List<FieldInfo> classFieldsInfo = fieldsByClass.get(declaringClassName);
			if (classFieldsInfo == null) {
				classFieldsInfo = new ArrayList<>();
				fieldsByClass.put(declaringClassName, classFieldsInfo);
			}
			
			String strValue = safeFieldValue(obj, f);
			FieldInfo fi = new FieldInfo(fieldId, f.getName(), strValue);
			classFieldsInfo.add(fi);
		}
		
		// fill methods
		List<Method> managedMethods = classInfo.getManagedMethods();
		for (int methodId = 0; methodId < managedMethods.size(); methodId++) {
			Method m = managedMethods.get(methodId);
			String declaringClassName = m.getDeclaringClass().getName();
			
			List<MethodInfo> classMethodsInfo = methodsByClass.get(declaringClassName);
			if (classMethodsInfo == null) {
				classMethodsInfo = new ArrayList<>();
				methodsByClass.put(declaringClassName, classMethodsInfo);
			}
			String methodNameTypeSignature = methodInfoService.getMethodNameTypeSignature(m);
			MethodInfo mi = new MethodInfo(methodId, m.getName(), methodNameTypeSignature,
					methodInfoService.getMethodParameters(m));
			classMethodsInfo.add(mi);
		}
		
		while (clazz != null) {
			classNames.add(clazz.getName());
			clazz = clazz.getSuperclass();
		}
		
		XmxObjectDetails details = new XmxObjectDetails(
				safeToString(obj),
				safeToJson(obj),
				classNames, fieldsByClass, methodsByClass);
		return details;
	}

	/**
	 * Returns "smart" string representation of the value, which is toString() if declared
	 * in the actual run-time type of the objct, and JSON otherwise.
	 */
	private static String safeFieldValue(Object obj, Field f) {
		try {
			Object val = f.get(obj);
			if (val == null) {
				return "null";
			}
			if (hasDeclaredToString(val.getClass())) {
				return val.toString();
			} else {
				return safeToJson(val);
			}
		} catch (Exception e) {
			return e.toString();
		}
	}

	private static boolean hasDeclaredToString(Class<?> c) {
		try {
			return c == c.getMethod("toString").getDeclaringClass();
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static String safeToJson(Object obj) {
		try {
			return jsonMapper.toJson(obj, Object.class);
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public Method getObjectMethodById(Object obj, int methodId)
			throws XmxRuntimeException {
		
		ManagedClassInfo classInfo = getManagedClassInfo(obj.getClass());
		return classInfo.getMethodById(methodId);
	}
	
	@Override
	public Field getObjectFieldById(Object obj, int fieldId) {
		ManagedClassInfo classInfo = getManagedClassInfo(obj.getClass());
		return classInfo.getFieldById(fieldId);
	}
	
	public ManagedClassInfo getManagedClassInfo(ManagedClassLoaderWeakRef loaderInfo, String className) {
		Integer classId = loaderInfo.getClassIdsByName().get(className);
		
		if (classId == null) {
			throw new XmxRuntimeException("Class is not managed: " + className); 
		}
		
		ManagedClassInfo classInfo = classesInfoById.get(classId);
		assert classInfo != null;
		return classInfo;
	}
	
	@Override
	public ManagedClassInfo getManagedClassInfo(Class<?> clazz) {
		String appName = obtainAppNameByLoader(clazz.getClassLoader());
		ManagedAppInfo appInfo = appInfosByName.get(appName);
		if (appInfo == null) {
			throw new XmxRuntimeException("App is not managed: " + appName); 
		}
		
		
		ManagedClassLoaderWeakRef loaderInfo = appInfo.getOrInitManagedClassLoaderInfo(clazz.getClassLoader(), managedClassLoadersRefQueue);
		return getManagedClassInfo(loaderInfo, clazz.getName());
	}
	
	@Override
	synchronized public Object getObjectById(int objectId) {
		WeakReference<Object> ref = objectsStorage.get(objectId);
		return ref == null ? null : ref.get(); 
	}

	private List<Method> getManagedMethods(Class<?> clazz) {
		List<Method> methods = new ArrayList<>(20);
		while (clazz != null) {
			Method[] declaredMethods = clazz.getDeclaredMethods();
			
			// sort methods declared in one class
			Arrays.sort(declaredMethods, ReflectionUtils.METHOD_COMPARATOR);
			
			for (Method m : declaredMethods) {
				// TODO: check if managed (e.g. by level or pattern)
				// TODO: skip overridden methods
				methods.add(m);
			}
			clazz = clazz.getSuperclass();
		}
		return methods;
	}
	
	private List<Field> getManagedFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>(20);
		while (clazz != null) {
			Field[] declaredFields = clazz.getDeclaredFields();
			if (config.getSystemProperty(Properties.GLOBAL_SORT_FIELDS).asBool()) {
				// optionally sort fields declared in one class
				Arrays.sort(declaredFields, ReflectionUtils.FIELD_COMPARATOR);
			}
			
			for (Field f : declaredFields) {
				// TODO: check if managed (e.g. by level or pattern)
				// TODO: skip overridden methods
				f.setAccessible(true);
				fields.add(f);
			}
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	private void fillLiveObjects(List<XmxObjectInfo> result, Integer classId) {
		ManagedClassInfo classInfo = classesInfoById.get(classId);
		if (classInfo == null || !classInfo.isInitialized()) {
			// no objects registered yet
			return;
		}
		Set<Integer> objectIds = classInfo.getObjectIds();
		for (Integer id : objectIds) {
			ManagedObjectWeakRef ref = objectsStorage.get(id);
			if (ref != null) {
				Object obj = ref.get();
				if (obj != null) {
					result.add(convertToObjectInfo(id, obj, classInfo));
				}
			}
		}
	}

	private static XmxObjectInfo convertToObjectInfo(int id, Object obj, ManagedClassInfo ci) {
		return new XmxObjectInfo(id, toDto(ci), safeToString(obj), safeToJson(obj));
	}
	
	private static XmxClassInfo toDto(ManagedClassInfo ci) {
		return new XmxClassInfo(ci.getId(), ci.getClassName());
	}

	private void fillXmxClassInfo(List<XmxClassInfo> result,
			Collection<Integer> classIds, Pattern classNamePattern) {
		for (Integer classId : classIds) {
			ManagedClassInfo classInfo = classesInfoById.get(classId);
			if (classInfo.isInitialized()) {
				if (classNamePattern != null) {
					if (classInfo.isInitialized()) {
						String className = classInfo.getClassName();
						if (!classNamePattern.matcher(className).matches()) {
							continue;
						}
					}
				}
				result.add(toDto(classInfo));
			}
		}
	}
	
	// TODO: skip if run from xmx-webui-all.war (i.e. from target app server)
	/**
	 * Starts Embedded Jetty Server to serve xmx-webui.war
	 */
	public void startUI() {
		logger.debug("Starting XMX Web UI..,");

		File xmxHomeDir = new File(System.getProperty(XMX_HOME_PROP));
		final File uiWarFile = new File(xmxHomeDir, "bin" + File.separator + "xmx-webui.war");
		File xmxLibDir = new File(xmxHomeDir, "lib");
		
		String serverImpl = config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_IMPL).asString();
		final String serverImplJarName = "xmx-server-" + serverImpl + ".jar";
		
		File[] serverImpls = xmxLibDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.equalsIgnoreCase(serverImplJarName); 
			}
		});
		if (serverImpls.length == 0) {
			throw new XmxRuntimeException("Missing file XMX_HOME/lib/" + serverImplJarName);
		}
		
		File implFile = serverImpls[0];
		URL[] urls;
		String launcherClassName;
		try(JarFile implJar = new JarFile(implFile)) {
			launcherClassName = implJar.getManifest().getMainAttributes().getValue(LAUNCHER_CLASS_ATTR);
			urls = new URL[]{serverImpls[0].toURI().toURL()};
		} catch (IOException e) {
			throw new XmxRuntimeException("Failed to read " + implFile.getAbsolutePath(), e);
		}
		
		if (launcherClassName == null || launcherClassName.isEmpty()) {
			throw new XmxRuntimeException("Invalid server implementation in " + implFile.getAbsolutePath() + 
					": no " + LAUNCHER_CLASS_ATTR + " in manifest");
		}

		logger.debug("XMX Web UI LauncherClass found: {} in {}", launcherClassName, implFile);

		final IXmxServerLauncher launcher;
		try {
			// use classloader of xmx-core as parent
			ClassLoader serverCL = new URLClassLoader(urls, XmxManager.class.getClassLoader());
			Class<? extends IXmxServerLauncher> launcherClass = 
					Class.forName(launcherClassName, true, serverCL).asSubclass(IXmxServerLauncher.class);
			launcher = launcherClass.getConstructor().newInstance();
		} catch(Exception e) {
			throw new XmxRuntimeException("Failed to instantiate XMX server launcher (" + launcherClassName + ")", e);
		}
	
		// start asynchronously so main initialization is less affected
		Thread startupThread = new Thread(new Runnable() {
			@Override
			public void run() {
				// wait a bit before starting UI to let short-living apps exit fast
				try {
					Thread.sleep(UI_START_DELAY);
				} catch (InterruptedException e) {
				}
				int port = config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asInt();
				launcher.launchServer(uiWarFile, port);
			}
		}, "XMX Embedded Server Startup Thread");
		startupThread.setDaemon(true);
		startupThread.start();

		logger.debug("XMX Web UI StartupThread is started with delay {} ms", UI_START_DELAY);
	}
}

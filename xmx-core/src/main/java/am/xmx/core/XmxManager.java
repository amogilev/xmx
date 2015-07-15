package am.xmx.core;


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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfoSupport;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import am.specr.SpeculativeProcessorFactory;
import am.xmx.cfg.IAppPropertiesSource;
import am.xmx.cfg.IXmxConfig;
import am.xmx.cfg.Properties;
import am.xmx.cfg.impl.XmxIniConfig;
import am.xmx.core.jmx.JmxSupport;
import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxObjectDetails.FieldInfo;
import am.xmx.dto.XmxObjectDetails.MethodInfo;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;
import am.xmx.server.IXmxServerLauncher;
import am.xmx.service.IXmxService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class XmxManager implements IXmxCoreService {

	public static IXmxConfig config = XmxIniConfig.getDefault();
	
	private static Gson gson;
	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Class.class, new TypeAdapter<Class<?>>() {
			@Override
			public void write(JsonWriter out, Class<?> value) throws IOException {
				out.value(value.getName());
			}
			@Override
			public Class<?> read(JsonReader in) throws IOException {
				return null;
			}
		});
		gson = builder.create();
	}
	
	private static SpeculativeProcessorFactory<IWebappNameExtractor> extractorsFactory = 
			new SpeculativeProcessorFactory<>(IWebappNameExtractor.class);
	static {
		extractorsFactory.registerProcessor(
				"am.xmx.core.CatalinaWebappNameExtractor", 
				"org.apache.catalina.loader.WebappClassLoader");
		extractorsFactory.registerProcessor(
				true, // Jetty's WebAppClassLoader shadows all server classes, need to check its parent 
				"am.xmx.core.JettyWebappNameExtractor", 
				"org.eclipse.jetty.webapp.WebAppClassLoader");
	}
	
	/**
	 * Storage of weak references to each managed objects, mapped by object ID
	 */
	private static Map<Integer, ManagedObjectWeakRef> objectsStorage = new HashMap<>(64*1024);
	
	/**
	 * Generator of unique IDs for managed objects.
	 */
	private static AtomicInteger managedObjectsCounter = new AtomicInteger();
	
	// TODO: currently no support for same classes in one app loaded by different class loaders
	// need to change appName in maps to IDXs of class loaders
	
	/**
	 * Storage of classes info for managed objects
	 */
	private static Map<Integer, XmxClassInfo> classesInfoById = new HashMap<>(32*1024);
	
	/**
	 * All class IDs by class object.
	 */
	private static Map<Class<?>, Integer> classIdsByClass = new HashMap<>(32*1024);
	
	/**
	 * Maps appName -> {classInfoIdx}, i.e. provides all class IDs for each app
	 */
	private static Map<String, List<Integer>> classIdsByApp = new HashMap<>();
	
	// classInfoIdx -> [objectInfoIdx]
	private static Map<Integer, Set<Integer>> objectIdsByClassIds = new HashMap<>();
	
	/**
	 * Generator of unique IDs for classes of managed objects.
	 */
	private static AtomicInteger managedClassesCounter = new AtomicInteger();
	
	private static ReferenceQueue<Object> managedObjectsRefQueue = new ReferenceQueue<>();
	
	static {
		Thread cleanerThread = new Thread("XMX-Cleaner-Thread") {
			@Override
			public void run() {
				while (true) {
					try {
						ManagedObjectWeakRef objRef = (ManagedObjectWeakRef) managedObjectsRefQueue.remove();
						synchronized(instance) {
							Set<Integer> objectIds = objectIdsByClassIds.get(objRef.classId);
							if (objectIds != null) {
								objectIds.remove(objRef.objectId);
							}
							objectsStorage.remove(objRef.objectId);
							
							if (objRef.jmxObjectName != null) {
								JmxSupport.unregisterBean(jmxServer, objRef.jmxObjectName);
							}
						}
					} catch (InterruptedException e) {
					}
				}
			}
		};
		cleanerThread.setDaemon(true);
		cleanerThread.start();
	}
	
	private static MBeanServer jmxServer = null;
	static {
		if (config.getSystemProperty(Properties.GLOBAL_JMX_ENABLED).asBool()) {
			// TODO maybe create a custom server instead, with custom connectors etc.
			jmxServer = ManagementFactory.getPlatformMBeanServer();
		}
	}
	
	private static final XmxManager instance = new XmxManager();
	
	private static final String LAUNCHER_CLASS_ATTR = "XMX-Server-Launcher-Class";
	
	// Non-public static API, used through reflection 
	
	public static IXmxCoreService getService() {
		return instance;
	}
	
	
	// Inner Implementation of XmxServiceEx API
	
	/**
	 * Registers a managed object into XMX system.
	 * A new unique ID is generated for an object, and a weak reference to the object is saved into the storage.
	 */
	@Override
	synchronized public void registerObject(Object obj) {
		Class<?> objClass = obj.getClass();
		String appName = obtainAppName(obj);
		
		Integer classId = classIdsByClass.get(objClass);
		XmxClassInfo classInfo;
		if (classId == null) {
			// first occurrence of the class
			// TODO: use classLoader in class ID
			classInfo = makeNewClassInfo(objClass, appName);
			classId = classInfo.getId();
			
			classesInfoById.put(classId, classInfo);
			classIdsByClass.put(objClass, classId);
			
			List<Integer> appClassIds = classIdsByApp.get(appName);
			if (appClassIds == null) {
				appClassIds = new ArrayList<>(1024);
				classIdsByApp.put(appName, appClassIds);
			}
			appClassIds.add(classId);
		} else {
			classInfo = classesInfoById.get(classId);
			assert classInfo != null;
		}
		
		Set<Integer> objectIds = objectIdsByClassIds.get(classId);
		if (objectIds == null) {
			objectIds = new HashSet<>(2);
			objectIdsByClassIds.put(classId, objectIds);
		}
		
		// check if object is already registered, e.g. from superclass
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
		}
		int otherInstancesCount = objectIds.size(); 
		
		// not registered yet, store internally and optionally register as JMX bean
		int objectId = managedObjectsCounter.getAndIncrement();
		ObjectName jmxObjectName = null;
		if (jmxServer != null && classInfo.getJmxClassModel() != null) {
			// register as JMX bean
			jmxObjectName = JmxSupport.registerBean(jmxServer, objectId, classInfo, otherInstancesCount == 0);
			
			if (otherInstancesCount == 1) {
				// check if another instance is registered in JMX as singleton. If so,
				// re-register it with id
				int anotherObjectId = objectIds.iterator().next();
				ManagedObjectWeakRef anotherObject = objectsStorage.get(anotherObjectId);
				ObjectName anotherObjectName = anotherObject.jmxObjectName; 
				if (anotherObjectName.getKeyProperty("id") == null) {
					// re-register as non-singleton
					JmxSupport.unregisterBean(jmxServer, anotherObjectName);
					anotherObject.jmxObjectName = JmxSupport.registerBean(jmxServer, anotherObjectId, classInfo, false); 
				}
			}
		}
		
		// store internally
		objectsStorage.put(objectId, new ManagedObjectWeakRef(obj, managedObjectsRefQueue, 
				objectId, classId, jmxObjectName));
		objectIds.add(objectId);
		
	}

	private XmxClassInfo makeNewClassInfo(Class<?> objClass, String appName) {
		int classId = managedClassesCounter.getAndIncrement();
		String className = objClass.getName();
		List<Method> managedMethods = getManagedMethods(objClass);
		List<Field> managedFields = getManagedFields(objClass);
		
		ModelMBeanInfoSupport jmxClassModel = null;
		String jmxObjectNamePart = null;
		if (jmxServer != null) {
			jmxClassModel = JmxSupport.createModelForClass(objClass, appName, managedMethods, managedFields, config);
			if (jmxClassModel != null) {
				jmxObjectNamePart = JmxSupport.createClassObjectNamePart(objClass, appName);
				assert jmxObjectNamePart != null; 
			}
		}
		
		return new XmxClassInfo(classId, className, managedMethods, managedFields, jmxClassModel, jmxObjectNamePart);
	}


	private static String obtainAppName(Object obj) {
		return obtainAppNameByLoader(obj.getClass().getClassLoader());
	}
	
	private static String obtainAppNameByLoader(ClassLoader loader) {
		List<IWebappNameExtractor> extractors = extractorsFactory.getProcessorsFor(loader);
		if (extractors != null) {
			for (IWebappNameExtractor extractor : extractors) {
				String name = extractor.extract(loader);
				if (name != null) {
					return name;
				}
			}
		}
			
		// no extractor found, or all failed
		return "";
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] transformClassIfInterested(ClassLoader classLoader, String className, byte[] classBuffer) {
		if (!isClassManaged(classLoader, className)) {
			return classBuffer;
		}
		
		System.err.println("transformClass: " + className);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		XmxInstructionsAdder xMXInstructionsAdder = new XmxInstructionsAdder(cw);

		ClassReader cr = new ClassReader(classBuffer);
		
		// TODO: decide whether to instrument abstract classes, or only "leafs"
		int access = cr.getAccess();
		if ((access & (Opcodes.ACC_ENUM | Opcodes.ACC_INTERFACE)) > 0) {
			// do not instrument enums and interfaces
			return classBuffer;
		}
		
		
		cr.accept(xMXInstructionsAdder, 0);
		return cw.toByteArray();
	}
	
	private static boolean isClassManaged(ClassLoader classLoader,
			String className) {
		String appName = obtainAppNameByLoader(classLoader);
		IAppPropertiesSource appConfig = config.getAppConfig(appName);
		
		
		boolean managed = appConfig.getAppProperty(Properties.APP_ENABLED).asBool() &&
				config.getAppConfig(appName).getClassProperty(className, Properties.SP_MANAGED).asBool();
		return managed;
	}
	
	
	// Inner Implementation of XmxService API
	
	/**
	 * Returns names (contexts) of all recognized web applications.
	 */
	@Override
	synchronized public List<String> getApplicationNames() {
		return new ArrayList<>(classIdsByApp.keySet());
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
			List<Integer> classIds = classIdsByApp.get(appNameOrNull);
			fillXmxClassInfo(result, classIds, classNamePattern);
		} else {
			for (List<Integer> classIds : classIdsByApp.values()) {
				fillXmxClassInfo(result, classIds, classNamePattern);
			}
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
			for (Integer id : objectIdsByClassIds.keySet()) {
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
		XmxClassInfo classInfo = getManagedClassInfo(clazz);

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
			
			String strValue = null;
			try {
				Object val = f.get(obj);
				strValue = val.toString();
			} catch (Exception e) {
				strValue = e.toString();
			}
			
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
			MethodInfo mi = new MethodInfo(methodId, m.getName(), m.toString());
			classMethodsInfo.add(mi);
		}
		
		while (clazz != null) {
			classNames.add(clazz.getName());
			clazz = clazz.getSuperclass();
		}
		
		XmxObjectDetails details = new XmxObjectDetails(classNames, fieldsByClass, methodsByClass);
		return details;
	}
	
	/**
	 * Sets new value of the specified object field.
	 * 
	 * @param objectId the ID of the object, obtained from {@link XmxObjectInfo#getObjectId()}
	 * @param fieldId the ID of the field to set, obtained from {@link XmxObjectDetails.FieldInfo#getId()}
	 * @param newValue the string representation of the value to assign to the new field
	 *  
	 * @return the new state of the object, after the field is set, or {@code null} if object is GC'ed
	 *  
	 * @throws XmxRuntimeException if failed to assign field
	 */
	@Override
	synchronized public XmxObjectDetails setObjectField(int objectId, 
			int fieldId, String newValue) {
		
		Object obj = getObjectById(objectId);
		if (obj == null) {
			return null;
		}
		
		Field f = getObjectFieldById(obj, fieldId);
		if (f == null) {
			throw new XmxRuntimeException("Field not found in " + obj.getClass().getName() + " by ID=" + fieldId);
		}
		Object deserializedValue = null;
		if (f.getType().equals(String.class)) {
			deserializedValue = newValue;
		} else {
			try {
				deserializedValue = gson.fromJson(newValue, f.getType());
			} catch (Exception e) {
				throw new XmxRuntimeException("Failed to deserialize the value; class=" + f.getType() +"; value=" + newValue, e);
			}
		}
		try {
			f.setAccessible(true);
			f.set(obj, deserializedValue);
		} catch (Exception e) {
			throw new XmxRuntimeException("Failed to set field", e);
		}
		
		return getObjectDetails(objectId);
	}
	
	@Override
	public Method getObjectMethodById(Object obj, int methodId)
			throws XmxRuntimeException {
		
		XmxClassInfo classInfo = getManagedClassInfo(obj.getClass());
		return classInfo.getMethodById(methodId);
	}
	
	@Override
	public Field getObjectFieldById(Object obj, int fieldId) {
		XmxClassInfo classInfo = getManagedClassInfo(obj.getClass());
		return classInfo.getFieldById(fieldId);
	}
	
	@Override
	public XmxClassInfo getManagedClassInfo(Class<?> clazz) {
		Integer classId = classIdsByClass.get(clazz);
		if (classId == null) {
			throw new XmxRuntimeException("Class is not managed: " + clazz); 
		}
		
		XmxClassInfo classInfo = classesInfoById.get(classId);
		assert classInfo != null;
		return classInfo;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object invokeObjectMethod(Object obj, Method m, Object... args) throws XmxRuntimeException {
		Class<?> clazz = obj.getClass();
		try {
			m.setAccessible(true);

			// set context class loader to enable functionality which depends on it, like JNDI
			ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
			try {
				Object returnValue = m.invoke(obj, args);
				if (returnValue == null) {
					return "null";
				} else {
					// TODO support JSON
					return returnValue.toString();
				}
			} finally {
				Thread.currentThread().setContextClassLoader(prevClassLoader);
			}
		} catch (Exception e) {
			throw new XmxRuntimeException("Failed to invoke method", e);
		}
	}
	
	@Override
	synchronized public Object getObjectById(int objectId) {
		WeakReference<Object> ref = objectsStorage.get(objectId);
		return ref == null ? null : ref.get(); 
	}
	
	private static List<Method> getManagedMethods(Class<?> clazz) {
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
	
	private static List<Field> getManagedFields(Class<?> clazz) {
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

	private static void fillLiveObjects(List<XmxObjectInfo> result, Integer classId) {
		XmxClassInfo xmxClassInfo = classesInfoById.get(classId);
		Set<Integer> objectIds = objectIdsByClassIds.get(classId);
		for (Integer id : objectIds) {
			ManagedObjectWeakRef ref = objectsStorage.get(id);
			if (ref != null) {
				Object obj = ref.get();
				if (obj != null) {
					result.add(convertToObjectInfo(id, obj, xmxClassInfo));
				}
			}
		}
	}

	private static XmxObjectInfo convertToObjectInfo(int id, Object obj, XmxClassInfo xmxClassInfo) {
		String json = "";
/*
		try {
			json = gson.toJson(obj);
		} catch (Throwable e) {
			json = "N/A: " + e;
			
			// TODO there are lot of StackOverflowError in Gson serialization
			// better switch to impl which supports recursion limit
			System.err.println(e);
		}
*/
		return new XmxObjectInfo(id, xmxClassInfo, obj.toString(), json);
	}

	private static void fillXmxClassInfo(List<XmxClassInfo> result,
			List<Integer> classIds, Pattern classNamePattern) {
		for (Integer classId : classIds) {
			if (classNamePattern != null) {
				XmxClassInfo classInfo = classesInfoById.get(classId);
				String className = classInfo.getClassName();
				if (!classNamePattern.matcher(className).matches()) {
					continue;
				}
			}
			result.add(classesInfoById.get(classId));
		}
	}
	
	// TODO: skip if run from xmx-webui-all.war (i.e. from target app server)
	/**
	 * Starts Embedded Jetty Server to serve xmx-webui.war
	 */
	public static void startUI() {
		if (!config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_ENABLED).asBool()) {
			// do nothing
			return;
		}
		
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
		
		final IXmxServerLauncher launcher;
		try {
			// use classloader of xmx-api as parent; xmx-core itself is not visible for the server
			ClassLoader serverCL = new URLClassLoader(urls, IXmxService.class.getClassLoader());
			Class<? extends IXmxServerLauncher> launcherClass = 
					Class.forName(launcherClassName, true, serverCL).asSubclass(IXmxServerLauncher.class);
			launcher = launcherClass.getConstructor().newInstance();
		} catch(Exception e) {
			throw new XmxRuntimeException("Failed to instantiate XMX server launcher (" + launcherClassName + ")", e);
		}
	
		// start asynchronously so that main initialization is less affected
		new Thread(new Runnable() {
			@Override
			public void run() {
				int port = config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asInt();
				launcher.launchServer(uiWarFile, port);
			}
		}, "XMX Embedded Server Startup Thread").start();
	}
	
}

package am.xmx.core;


import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import am.specr.SpeculativeProcessorFactory;
import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxService;
import am.xmx.dto.XmxObjectDetails.FieldInfo;
import am.xmx.dto.XmxObjectDetails.MethodInfo;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class XmxManager implements XmxService {

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
	}
	
	
	/**
	 * Storage of weak references to each managed objects, mapped by object ID
	 */
	// TODO: maybe store ObjectInfo, but needs a cleaning thread & ReferenceQueue
	private static Map<Integer, WeakReference<Object>> objectsStorage = new HashMap<>(64*1024);
	
	/**
	 * Generator of unique IDs for managed objects.
	 */
	private static AtomicInteger managedObjectsCounter = new AtomicInteger();
	
	/**
	 * Storage of classes info for managed objects
	 */
	private static Map<Integer, XmxClassInfo> classesInfoById = new HashMap<>(32*1024);
	
	// TODO: currently no support for same classes in one app loaded by different class loaders
	// need to change appName in maps to IDXs of class loaders
	
	// appName -> {className -> classInfoIdx}
	private static Map<String, Map<String, Integer>> classIdsByAppAndName = new HashMap<>();
	
	// classInfoIdx -> [objectInfoIdx]
	private static Map<Integer, List<Integer>> objectIdsByClassIds = new HashMap<>();
	
	/**
	 * Generator of unique IDs for classes of managed objects.
	 */
	private static AtomicInteger managedClassesCounter = new AtomicInteger();
	
	private static ReferenceQueue<Object> managedObjectsRefQueue = new ReferenceQueue<>();
	
	private static final XmxManager instance = new XmxManager();
	
	// Non-public static API, used through reflection 
	
	public static XmxService getService() {
		return instance;
	}
	
	public static byte[] transformClass(ClassLoader classLoader, String className, byte[] classBuffer) {
		System.err.println("transformClass: " + className);
		return instrument(classBuffer);
	}
	
	/**
	 * Registers a managed object into XMX system.
	 * A new unique ID is generated for an object, and a weak reference to the object is saved into the storage.
	 */
	synchronized public static void registerObject(Object obj) {
		int idx = managedObjectsCounter.getAndIncrement();
		objectsStorage.put(idx, new WeakReference<>(obj, managedObjectsRefQueue));
	
		Class<?> objClass = obj.getClass();
		String className = objClass.getName();
		String appName = obtainWebAppName(obj);
		
		Map<String, Integer> classIdsByName = classIdsByAppAndName.get(appName);
		if (classIdsByName == null) {
			classIdsByName = new HashMap<>();
			classIdsByAppAndName.put(appName, classIdsByName);
		}
		Integer classInfoIdx = classIdsByName.get(className);
		if (classInfoIdx == null) {
			classInfoIdx = managedClassesCounter.getAndIncrement();
			classIdsByName.put(className, classInfoIdx);
			
			// TODO: classLoader ID
			XmxClassInfo classInfo = new XmxClassInfo(classInfoIdx, className, 0, appName);
			classesInfoById.put(classInfoIdx, classInfo);
		}
		
		List<Integer> objectIds = objectIdsByClassIds.get(classInfoIdx);
		if (objectIds == null) {
			objectIds = new ArrayList<>(2);
			objectIdsByClassIds.put(classInfoIdx, objectIds);
		}
		objectIds.add(idx);
	}

	private static String obtainWebAppName(Object obj) {
		List<IWebappNameExtractor> extractors = extractorsFactory.getProcessorsFor(obj);
		if (extractors != null) {
			for (IWebappNameExtractor extractor : extractors) {
				String name = extractor.extract(obj);
				if (name != null) {
					return name;
				}
			}
		}
			
		// no extractor found, or all failed
		return "";
	}

	public static byte[] instrument(byte[] classBytes) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		XmxInstructionsAdder xMXInstructionsAdder = new XmxInstructionsAdder(cw);

		ClassReader cr = new ClassReader(classBytes);
		cr.accept(xMXInstructionsAdder, 0);
		return cw.toByteArray();
	}
	
	// Inner Implementation of XmxService API
	
	/**
	 * Returns names (contexts) of all recognized web applications.
	 */
	@Override
	synchronized public List<String> getApplicationNames() {
		return new ArrayList<>(classIdsByAppAndName.keySet());
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
			Map<String, Integer> classIdsByName = classIdsByAppAndName.get(appNameOrNull);
			fillXmxClassInfo(result, classIdsByName, classNamePattern);
		} else {
			for (Map<String, Integer> classIdsByName : classIdsByAppAndName.values()) {
				fillXmxClassInfo(result, classIdsByName, classNamePattern);
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
		fillDetails(obj, clazz, classNames, fieldsByClass, methodsByClass, 0, 0);
		
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
		
		Class<?> clazz = obj.getClass();
		Field f = getFieldById(clazz, fieldId);
		if (f == null) {
			throw new XmxRuntimeException("Field not found in " + clazz.getName() + " by ID=" + fieldId);
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
	
	/**
	 * Invokes the specified method of the object, and returns the returned value as JSON string.
	 * 
	 * @param objectId the ID of the object, obtained from {@link XmxObjectInfo#getObjectId()}
	 * @param methodId the ID of the method to invoke, obtained from {@link XmxObjectDetails.MethodInfo#getId()}
	 * @param args string representation of the arguments to pass, if any
	 *  
	 * @return the method's returned value serialized to JSON
	 *  
	 * @throws XmxRuntimeException if failed to invoke the method, or the method throws exception
	 */
	@Override
	public String invokeObjectMethod(int objectId, 
			int methodId, String...args) {
		
		Object obj = getObjectById(objectId);
		if (obj == null) {
			return null;
		}
		
		Class<?> clazz = obj.getClass();
		Method m = getMethodById(clazz, methodId);
		if (m == null) {
			throw new XmxRuntimeException("Method not found in " + clazz.getName() + " by ID=" + methodId);
		}
		try {
			m.setAccessible(true);
			Class<?>[] parameterTypes = m.getParameterTypes();
			Object[] methodArgs = new Object[parameterTypes.length];
			if (parameterTypes.length < args.length) {
				throw new XmxRuntimeException("Too many arguments for the method " + clazz.getName() + "#" + m);
			}
			// less or equal number of args is supported
			for (int i = 0; i < args.length; i++) {
				Class<?> type = parameterTypes[i];
				if (!type.equals(String.class)) {
					// TODO: support json
					throw new XmxRuntimeException("NOT IMPLEMENTED: Only String's args are supported now for invokeObjectMethod()");
				} else {
					// TODO support JSON
					methodArgs[i] = args[i];
				}
			}
			
			Object returnValue = m.invoke(obj, methodArgs);
			if (returnValue == null) {
				return "null";
			} else {
				// TODO support JSON
				return returnValue.toString();
			}
		} catch (Exception e) {
			throw new XmxRuntimeException("Failed to invoke method", e);
		}
	}
	
	synchronized private static Object getObjectById(int objectId) {
		WeakReference<Object> ref = objectsStorage.get(objectId);
		return ref == null ? null : ref.get(); 
	}
	
	private static void fillDetails(Object obj, Class<?> clazz,
			List<String> classNames,
			Map<String, List<FieldInfo>> fieldsByClass,
			Map<String, List<MethodInfo>> methodsByClass, int fieldsCount, int methodsCount) {
		
		String className = clazz.getName();
		classNames.add(className);
		
		Field[] fields = clazz.getDeclaredFields();
		List<FieldInfo> fieldsInfo = new ArrayList<>(fields.length);
		for (Field f : fields) {
			f.setAccessible(true);
			String name = f.getName();
			String strValue = null;
			try {
				Object val = f.get(obj);
				strValue = val.toString();
			} catch (Exception e) {
				strValue = e.toString();
			}
			fieldsInfo.add(new FieldInfo(fieldsCount++, name, strValue));
		}
		fieldsByClass.put(className, fieldsInfo);
		
		Method[] methods = clazz.getDeclaredMethods();
		List<MethodInfo> methodsInfo = new ArrayList<>(methods.length);
		for (Method m : methods) {
			String name = m.getName();
			String signature = m.toString();
			methodsInfo.add(new MethodInfo(methodsCount++, name, signature));
		}
		methodsByClass.put(className, methodsInfo);
		
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			// call fillDetails recursively for superclass
			fillDetails(obj, superclass, classNames, fieldsByClass, methodsByClass, fieldsCount, methodsCount);
		}
	}

	private static Field getFieldById(Class<?> clazz, int fieldId) {
		Field[] fields = clazz.getDeclaredFields();
		if (fieldId < fields.length) {
			return fields[fieldId];
		}
		
		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null) {
			return null;
		}
		
		return getFieldById(superclass, fieldId - fields.length);
	}
	
	private static Method getMethodById(Class<?> clazz, int methodId) {
		Method[] methods = clazz.getDeclaredMethods();
		if (methodId < methods.length) {
			return methods[methodId];
		}
		
		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null) {
			return null;
		}
		
		return getMethodById(superclass, methodId - methods.length);
	}

	
	
	private static void fillLiveObjects(List<XmxObjectInfo> result, Integer classId) {
		XmxClassInfo xmxClassInfo = classesInfoById.get(classId);
		List<Integer> objectIds = objectIdsByClassIds.get(classId);
		for (Integer id : objectIds) {
			WeakReference<Object> ref = objectsStorage.get(id);
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
		try {
			json = gson.toJson(obj);
		} catch (Throwable e) {
			json = "N/A: " + e;
			
			// TODO there are lot of StackOverflowError in Gson serialization
			// better switch to impl which supports recursion limit
			System.err.println(e);
		}
		return new XmxObjectInfo(id, xmxClassInfo, obj.toString(), json);
	}

	private static void fillXmxClassInfo(List<XmxClassInfo> result,
			Map<String, Integer> classIdsByName, Pattern classNamePattern) {
		for (Entry<String, Integer> e : classIdsByName.entrySet()) {
			String className = e.getKey();
			Integer classId = e.getValue();
			if (classNamePattern == null || classNamePattern.matcher(className).matches()) {
				result.add(classesInfoById.get(classId));
			}
		}
	}


}

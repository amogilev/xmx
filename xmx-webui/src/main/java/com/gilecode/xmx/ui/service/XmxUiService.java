// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

import com.gilecode.xmx.core.type.IMethodInfoService;
import com.gilecode.xmx.dto.XmxClassInfo;
import com.gilecode.xmx.dto.XmxObjectDetails;
import com.gilecode.xmx.dto.XmxObjectInfo;
import com.gilecode.xmx.dto.XmxRuntimeException;
import com.gilecode.xmx.service.IMapperService;
import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.ui.UIConstants;
import com.gilecode.xmx.ui.dto.ExtendedXmxClassInfo;
import com.gilecode.xmx.ui.dto.ExtendedXmxObjectDetails;
import com.gilecode.xmx.ui.dto.ExtendedXmxObjectInfo;
import com.gilecode.xmx.ui.dto.XmxObjectTextRepresentation;
import com.gilecode.yagson.YaGson;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class XmxUiService implements IXmxUiService, UIConstants {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(XmxUiService.class);

	private static YaGson jsonMapper = new YaGson();

	private IMethodInfoService methodInfoService;
	private IXmxService xmxService;
	private IMapperService mapperService;

	public XmxUiService(IMethodInfoService methodInfoService, IXmxService xmxService, IMapperService mapperService) {
		this.methodInfoService = methodInfoService;
		this.xmxService = xmxService;
		this.mapperService = mapperService;
	}

	@Override
	public Map<String, Collection<ExtendedXmxClassInfo>> getAppsAndClasses() {
		Map<String, Collection<ExtendedXmxClassInfo>> appsClassesMap = new LinkedHashMap<>();
		List<String> applicationNames = xmxService.getApplicationNames();
		Collections.sort(applicationNames);
		if (CollectionUtils.isNotEmpty(applicationNames)) {
			for (String applicationName : applicationNames) {
				List<XmxClassInfo> managedClassInfos = xmxService.findManagedClassInfos(applicationName, null);
				Collections.sort(managedClassInfos, new Comparator<XmxClassInfo>() {
					@Override
					public int compare(XmxClassInfo o1, XmxClassInfo o2) {
						return o1.getClassName().compareTo(o2.getClassName());
					}
				});
				if (CollectionUtils.isNotEmpty(managedClassInfos)) {
					List<ExtendedXmxClassInfo> extendedXmxClassInfos = new ArrayList<>(managedClassInfos.size());
					for (XmxClassInfo managedClassInfo : managedClassInfos) {
						ExtendedXmxClassInfo extendedXmxClassInfo = new ExtendedXmxClassInfo(
								managedClassInfo.getId(),
								managedClassInfo.getClassName());
						extendedXmxClassInfo.setNumberOfObjects(CollectionUtils.size(
								xmxService.getManagedObjects(extendedXmxClassInfo.getId()))
						);
						extendedXmxClassInfos.add(extendedXmxClassInfo);
					}

					appsClassesMap.put(applicationName, extendedXmxClassInfos);
				}
			}
		}
		return appsClassesMap;
	}

	@Override
	public Integer getManagedClassSingleInstanceId(int classId) {
		List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(classId);
		if (managedObjects.size() == 1) {
			return managedObjects.get(0).getObjectId();
		} else {
			return null;
		}
	}

	@Override
	public List<ExtendedXmxObjectInfo> getManagedClassInstancesInfo(Integer classId) {
		List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(classId);
		List<ExtendedXmxObjectInfo> extObjectsInfo = new ArrayList<>(managedObjects.size());
		for (XmxObjectInfo o : managedObjects) {
			extObjectsInfo.add(toExtendedInfo(o, CLASS_OBJS_JSON_CHARS_LIMIT));
		}
		return extObjectsInfo;
	}

	/**
	 * Obtains the details of the object by its ID, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *
	 * @param objectId the unique object ID
	 */
	@Override
	public ExtendedXmxObjectDetails getExtendedObjectDetails(int objectId) throws MissingObjectException {
		XmxObjectDetails objectDetails = xmxService.getObjectDetails(objectId);
		if (objectDetails == null) {
			throw new MissingObjectException(objectId);
		}
		Object obj = objectDetails.getValue();

		List<String> classNames = new ArrayList<>();
		Map<String, List<ExtendedXmxObjectDetails.FieldInfo>> fieldsByClass = new LinkedHashMap<>();
		Map<String, List<ExtendedXmxObjectDetails.MethodInfo>> methodsByClass = new LinkedHashMap<>();


		Class<?> clazz = obj.getClass();

		// fill fields
		Map<Integer, Field> managedFields = objectDetails.getManagedFields();
		for (Map.Entry<Integer, Field> e : managedFields.entrySet()) {
			Integer fieldId = e.getKey();
			Field f = e.getValue();

			String declaringClassName = f.getDeclaringClass().getName();

			List<ExtendedXmxObjectDetails.FieldInfo> classFieldsInfo = fieldsByClass.get(declaringClassName);
			if (classFieldsInfo == null) {
				classFieldsInfo = new ArrayList<>();
				fieldsByClass.put(declaringClassName, classFieldsInfo);
			}

			XmxObjectTextRepresentation textValue = safeFieldToText(obj, f, OBJ_FIELDS_JSON_CHARS_LIMIT);
			ExtendedXmxObjectDetails.FieldInfo fi = new ExtendedXmxObjectDetails.FieldInfo(fieldId, f.getName(), textValue);
			classFieldsInfo.add(fi);
		}

		// fill methods
		Map<Integer, Method> managedMethods = objectDetails.getManagedMethods();
		for (Map.Entry<Integer, Method> e : managedMethods.entrySet()) {
			Integer methodId = e.getKey();
			Method m = e.getValue();
			String declaringClassName = m.getDeclaringClass().getName();

			List<ExtendedXmxObjectDetails.MethodInfo> classMethodsInfo = methodsByClass.get(declaringClassName);
			if (classMethodsInfo == null) {
				classMethodsInfo = new ArrayList<>();
				methodsByClass.put(declaringClassName, classMethodsInfo);
			}
			String methodNameTypeSignature = methodInfoService.getMethodNameTypeSignature(m);
			ExtendedXmxObjectDetails.MethodInfo mi = new ExtendedXmxObjectDetails.MethodInfo(methodId, m.getName(), methodNameTypeSignature,
					methodInfoService.getMethodParameters(m));
			classMethodsInfo.add(mi);
		}

		while (clazz != null) {
			classNames.add(clazz.getName());
			clazz = clazz.getSuperclass();
		}
		return new ExtendedXmxObjectDetails(objectId, objectDetails.getClassInfo(), obj,
				toText(obj, OBJ_JSON_CHARS_LIMIT),
				classNames, fieldsByClass, methodsByClass);
	}

	@Override
	public XmxObjectTextRepresentation invokeObjectMethod(Integer objectId, int methodId, String[] argsArr) throws Throwable {
		final XmxObjectInfo objInfo = getXmxObjectInfo(objectId);

		final Object obj = objInfo.getValue();
		final Method m = xmxService.getObjectMethodById(objInfo.getObjectId(), methodId);
		if (m == null) {
			throw new XmxRuntimeException("Method not found in " + objInfo.getClassInfo().getClassName() +
					" by ID=" + methodId);
		}

		// set context class loader to enable functionality which depends on it, like JNDI
		ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(obj.getClass().getClassLoader());

		XmxObjectTextRepresentation resultText;
		try {
			Object result = m.invoke(obj, translateArgs(argsArr, m, obj));
			resultText = toText(result, 0);
		} catch (InvocationTargetException e) {
			// re-throw cause
			// alternatively, a special page may be created for exception result, but it seems unnecessary now
			throw e.getCause();
		} finally {
			Thread.currentThread().setContextClassLoader(prevClassLoader);
		}
		return resultText;
	}

	@Override
	public void setObjectField(Integer objectId, Integer fieldId, String value) throws MissingObjectException {
		final XmxObjectInfo objInfo = getXmxObjectInfo(objectId);

		final Object obj = objInfo.getValue();
		final Field f = xmxService.getObjectFieldById(objectId, fieldId);
		if (f == null) {
			throw new XmxRuntimeException("Field not found in " + objInfo.getClassInfo().getClassName() +
					" by ID=" + fieldId);
		}

		Object deserializedValue = deserializeValue(value, f.getType(), obj);
		try {
			f.set(obj, deserializedValue);
		} catch (Exception e) {
			throw new XmxRuntimeException("Failed to set field", e);
		}
	}

	@Override
	public void printAllObjectsReport(PrintWriter out) {
		List<String> applicationNames = xmxService.getApplicationNames();
		Collections.sort(applicationNames);
		for (String applicationName : applicationNames) {
			out.println("Application: " + applicationName);
			List<XmxClassInfo> managedClassInfos = xmxService.findManagedClassInfos(applicationName, null);
			Collections.sort(managedClassInfos, new Comparator<XmxClassInfo>() {
				@Override
				public int compare(XmxClassInfo o1, XmxClassInfo o2) {
					return o1.getClassName().compareTo(o2.getClassName());
				}
			});
			for (XmxClassInfo managedClassInfo : managedClassInfos) {
				List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(managedClassInfo.getId());
				out.println("Class: " + managedClassInfo.getClassName() + " (" + managedObjects.size() + " instances)");
				for (XmxObjectInfo objectInfo : managedObjects) {
					out.println("  id=" + objectInfo.getObjectId() + ", json=" + mapperService.safeToJson(objectInfo.getValue()));
				}
				out.println("-------------");
			}
			out.println("=============\n");
		}
	}

	@Override
	public void printFullObjectJson(int objectId, Integer fieldId, PrintWriter out) throws IOException {
		XmxObjectDetails objectDetails = xmxService.getObjectDetails(objectId);
		if (objectDetails == null) {
			out.println("Error: the object is missing!");
			return;
		}
		Object jsonSourceObject;
		Object obj = objectDetails.getValue();
		if (fieldId != null) {
			Map<Integer, Field> managedFields = objectDetails.getManagedFields();
			Field f = managedFields.get(fieldId);
			if (f == null) {
				out.println("Error: the field is missing!");
				return;
			}
			try {
				jsonSourceObject = f.get(obj);
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else {
			jsonSourceObject = obj;
		}

		// do not use safeToJson here, force toJson attempt even if recently failed
		jsonMapper.toJson(jsonSourceObject, out);
	}

	private XmxObjectInfo getXmxObjectInfo(Integer objectId) throws MissingObjectException {
		final XmxObjectInfo objInfo = xmxService.getManagedObject(objectId);
		if (objInfo == null) {
			throw new MissingObjectException(objectId);
		}
		return objInfo;
	}

	private ExtendedXmxObjectInfo toExtendedInfo(XmxObjectInfo info, long jsonCharsLimit) {
		return new ExtendedXmxObjectInfo(info, toText(info.getValue(), jsonCharsLimit));
	}

	/**
	 * Returns "smart" string representation of the value, which is toString() if declared
	 * in the actual run-time type of the objct, and JSON otherwise.
	 */
	private XmxObjectTextRepresentation safeFieldToText(Object obj, Field f, long jsonCharsLimit) {
		Object val;
		try {
			val = f.get(obj);
		} catch (Exception e) {
			String err = e.toString();
			return new XmxObjectTextRepresentation(err, err, 0, true);
		}
		String strValue = mapperService.safeToString(val);
		String jsonValue = mapperService.safeToJson(val, jsonCharsLimit);
		return new XmxObjectTextRepresentation(strValue, jsonValue, jsonCharsLimit,
				val == null || hasDeclaredToString(val.getClass()));
	}

	/**
	 * Converts arguments from Strings to Objects. Formal types of the arguments are taken
	 * from the method's reflection info.
	 *
	 * @param args the arguments as Strings
	 * @param m the method to be invoked with the arguments
	 * @param obj the object which method is invoked; used to obtain class loader to use
	 *
	 * @return the array of objects which may be used to invoke the method
	 */
	private Object[] translateArgs(String[] args, Method m, Object obj) {
		if (args == null) {
			args = new String[0];
		}
		Class<?>[] parameterTypes = m.getParameterTypes();
		if (parameterTypes.length != args.length) {
			throw new XmxRuntimeException("Expected " + parameterTypes.length + " arguments, but only got " +
					args.length);
		}
		Object[] methodArgs = new Object[parameterTypes.length];
		for (int i = 0; i < args.length; i++) {
			Class<?> type = parameterTypes[i];
			methodArgs[i] = deserializeValue(args[i], type, obj);
		}
		return methodArgs;
	}


	private static boolean hasDeclaredToString(Class<?> c) {
		try {
			return c == c.getMethod("toString").getDeclaringClass();
		} catch (NoSuchMethodException e) {
			return false;
		}
	}


	private Object deserializeValue(String value, Type formalType, Object contextObj) {
		final ClassLoader prevContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader clToUse = contextObj.getClass().getClassLoader();
			// TODO: if setting through refschain will be implemented, then we'll probably need to use the classloader of
			//       the first object. Or maybe multiple class loaders... (svc -> Object[] -> SpecialObj)
			Thread.currentThread().setContextClassLoader(clToUse);
			return jsonMapper.fromJson(value, formalType);
		} catch (Exception e) {
			throw new XmxRuntimeException("Failed to deserialize the value; class=" + formalType + "; value=" + value, e);
		} finally {
			Thread.currentThread().setContextClassLoader(prevContextClassLoader);
		}
	}

	private XmxObjectTextRepresentation toText(Object obj, long jsonCharsLimit) {
		return new XmxObjectTextRepresentation(mapperService.safeToString(obj),
				mapperService.safeToJson(obj, jsonCharsLimit), jsonCharsLimit,
				obj == null || hasDeclaredToString(obj.getClass()));
	}
}

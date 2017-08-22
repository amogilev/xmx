// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.core.type.IMethodInfoService;
import com.gilecode.xmx.dto.XmxClassInfo;
import com.gilecode.xmx.dto.XmxObjectDetails;
import com.gilecode.xmx.dto.XmxObjectInfo;
import com.gilecode.xmx.dto.XmxRuntimeException;
import com.gilecode.xmx.service.IMapperService;
import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.ui.ExtendedXmxObjectDetails.FieldInfo;
import com.gilecode.xmx.ui.ExtendedXmxObjectDetails.MethodInfo;
import com.gilecode.yagson.YaGson;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

@Controller
@RequestMapping("/")
public class RestController {

	// default JSON chars limit for object on 'details' page
	static final long OBJ_JSON_CHARS_LIMIT = 2_000_000;

	// default JSON chars limit for object's fields on 'details' page
	static final long OBJ_FIELDS_JSON_CHARS_LIMIT = 10_000;

	// default JSON chars limit for object selection on 'classObjects' page
	static final long CLASS_OBJS_JSON_CHARS_LIMIT = 2_000;

	private static YaGson jsonMapper = new YaGson();

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(RestController.class);

	@Autowired
	private IXmxService xmxService;

	@Autowired
	private IMapperService mapperService;

	@Autowired
	private IMethodInfoService methodInfoService;

	@RequestMapping(method = RequestMethod.GET)
	public String getAppsAndClasses(ModelMap model) {
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
		model.addAttribute("managedAppsClassesMap", appsClassesMap);
		return "appsClasses";
	}

	@RequestMapping(value = "getClassObjects", method = RequestMethod.GET)
	public String getClassObjects(ModelMap model, @RequestParam Integer classId, @RequestParam String className) {
		List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(classId);
		if (managedObjects.size() == 1) {
			// fast path for singletons
			 return "redirect:/getObjectDetails?objectId=" + managedObjects.get(0).getObjectId();
		}
		List<ExtendedXmxObjectInfo> extObjectsInfo = new ArrayList<>(managedObjects.size());
		for (XmxObjectInfo o : managedObjects) {
			extObjectsInfo.add(toExtendedInfo(o, CLASS_OBJS_JSON_CHARS_LIMIT));
		}
		model.addAttribute("className", className);
		model.addAttribute("objects", extObjectsInfo);
		return "classObjects";
	}

	private ExtendedXmxObjectInfo toExtendedInfo(XmxObjectInfo info, long jsonCharsLimit) {
		return new ExtendedXmxObjectInfo(info, toText(info.getValue(), jsonCharsLimit));
	}

	@RequestMapping(value = "getObjectDetails", method = RequestMethod.GET)
	public String getObjectDetails(ModelMap model,
               @RequestParam Integer objectId,
               @RequestParam(required = false, defaultValue = "SMART") ValuesDisplayKind valKind) {
		model.addAttribute("objectId", objectId);
		ExtendedXmxObjectDetails details = getExtendedObjectDetails(objectId);
		if (details == null) {
			return "missingObject";
		} 
		
		String className = details.getClassesNames().get(0);
		model.addAttribute("className", className);
		model.addAttribute("details", details);
		model.addAttribute("valKind", valKind);

		return "objectDetails";
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

	@RequestMapping(value = "setObjectField", method = RequestMethod.GET)
	public String setObjectField(ModelMap model, @RequestParam Integer objectId,
			@RequestParam Integer fieldId, @RequestParam String value) {
		model.addAttribute("objectId", objectId);

		final XmxObjectInfo objInfo = xmxService.getManagedObject(objectId);
		if (objInfo == null) {
			return "missingObject";
		}
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

		ExtendedXmxObjectDetails updatedDetails = getExtendedObjectDetails(objectId);
		if (updatedDetails == null) {
			return "missingObject";
		}
		model.addAttribute("details", updatedDetails);

		return "redirect:/getObjectDetails?objectId=" + objectId;
	}

	// not used in web, but convenient for manual invocation, like
	//  curl "http://localhost:8081/invokeMethod?objectId=18&methodId=1&arg=1&arg=2&arg=3"
	@RequestMapping(value = "invokeMethod", method = RequestMethod.GET)
	public String invokeObjectMethodTest(ModelMap model, @RequestParam int objectId,
									 @RequestParam int methodId,
									 @RequestParam("arg") String[] args) throws Throwable {
		return "foo";
	}

	@RequestMapping(value = "invokeMethod", method = RequestMethod.POST)
	public String invokeObjectMethod(ModelMap model, @RequestParam int objectId,
			@RequestParam int methodId,
			@RequestParam(value = "arg", required = false) String[] argsArr) throws Throwable {

		final XmxObjectInfo objInfo = xmxService.getManagedObject(objectId);
		if (objInfo == null) {
			return "missingObject";
		}
		model.addAttribute("objectId", objectId);

		final Object obj = objInfo.getValue();
		final Method m = xmxService.getObjectMethodById(objectId, methodId);
		if (m == null) {
			throw new XmxRuntimeException("Method not found in " + objInfo.getClassInfo().getClassName() +
					" by ID=" + methodId);
		}

		// set context class loader to enable functionality which depends on it, like JNDI
		ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(obj.getClass().getClassLoader());

		Object result;
		try {
			result = m.invoke(obj, translateArgs(argsArr, m, obj));
			model.addAttribute("result", toText(result, 0));
		} catch (InvocationTargetException e) {
			// re-throw cause
			// alternatively, a special page may be created for exception result, but it seems unnecessary now
			throw e.getCause();
		} finally {
			Thread.currentThread().setContextClassLoader(prevClassLoader);
		}
		
		return "methodResult";
	}

	@RequestMapping(value = "reportAllObjects", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void getAllObjectsReport(HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();

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

	@RequestMapping(value = "getFullJson", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void loadFullJson(HttpServletResponse resp,
							 @RequestParam(value = "objectId") int objectId,
							 @RequestParam(value = "fieldId", required = false) Integer fieldId) throws IOException {

		PrintWriter out = resp.getWriter();

		XmxObjectDetails objectDetails = xmxService.getObjectDetails(objectId);
		if (objectDetails == null) {
			out.println("Error: the object is missing!");
			return;
		}
		Object jsonSourceObject = null;
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

	private XmxObjectTextRepresentation toText(Object obj, long jsonCharsLimit) {
		return new XmxObjectTextRepresentation(mapperService.safeToString(obj),
				mapperService.safeToJson(obj, jsonCharsLimit), jsonCharsLimit,
				obj == null || hasDeclaredToString(obj.getClass()));
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

	/**
	 * Obtains the details of the object by its ID, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *
	 * @param objectId the unique object ID
	 */
	public ExtendedXmxObjectDetails getExtendedObjectDetails(int objectId) {
		XmxObjectDetails objectDetails = xmxService.getObjectDetails(objectId);
		if (objectDetails == null) {
			return null;
		}
		Object obj = objectDetails.getValue();

		List<String> classNames = new ArrayList<>();
		Map<String, List<FieldInfo>> fieldsByClass = new LinkedHashMap<>();
		Map<String, List<MethodInfo>> methodsByClass = new LinkedHashMap<>();


		Class<?> clazz = obj.getClass();

		// fill fields
		Map<Integer, Field> managedFields = objectDetails.getManagedFields();
		for (Map.Entry<Integer, Field> e : managedFields.entrySet()) {
			Integer fieldId = e.getKey();
			Field f = e.getValue();

			String declaringClassName = f.getDeclaringClass().getName();

			List<FieldInfo> classFieldsInfo = fieldsByClass.get(declaringClassName);
			if (classFieldsInfo == null) {
				classFieldsInfo = new ArrayList<>();
				fieldsByClass.put(declaringClassName, classFieldsInfo);
			}

			XmxObjectTextRepresentation textValue = safeFieldToText(obj, f, OBJ_FIELDS_JSON_CHARS_LIMIT);
			FieldInfo fi = new FieldInfo(fieldId, f.getName(), textValue);
			classFieldsInfo.add(fi);
		}

		// fill methods
		Map<Integer, Method> managedMethods = objectDetails.getManagedMethods();
		for (Map.Entry<Integer, Method> e : managedMethods.entrySet()) {
			Integer methodId = e.getKey();
			Method m = e.getValue();
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
		return new ExtendedXmxObjectDetails(objectId, objectDetails.getClassInfo(), obj,
				toText(obj, OBJ_JSON_CHARS_LIMIT),
				classNames, fieldsByClass, methodsByClass);
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

	private static boolean hasDeclaredToString(Class<?> c) {
		try {
			return c == c.getMethod("toString").getDeclaringClass();
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
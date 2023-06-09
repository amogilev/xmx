// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

import com.gilecode.reflection.ReflectionAccessUtils;
import com.gilecode.reflection.ReflectionAccessor;
import com.gilecode.xmx.core.type.IMethodInfoService;
import com.gilecode.xmx.model.*;
import com.gilecode.xmx.service.IMapperService;
import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.ui.UIConstants;
import com.gilecode.xmx.ui.dto.*;
import com.gilecode.xmx.ui.refpath.*;
import com.gilecode.xmx.ui.smx.context.ContextDataExtractor;
import com.gilecode.xmx.util.ReflectionUtils;
import com.gilecode.yagson.YaGson;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.gilecode.xmx.util.ReflectionUtils.safeFindMethod;

public class XmxUiService implements IXmxUiService, UIConstants {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(XmxUiService.class);

	private static YaGson jsonMapper = new YaGson();
	private static final ReflectionAccessor reflAccessor = ReflectionAccessUtils.getReflectionAccessor();
	private static final String sessionId = initSessionId();

	private final IMethodInfoService methodInfoService;
	private final IXmxService xmxService;
	private final IMapperService mapperService;
	private final RefPathParser refPathParser;
	private final ContextDataExtractor contextDataExtractor;

	public XmxUiService(IMethodInfoService methodInfoService, IXmxService xmxService, IMapperService mapperService,
			RefPathParser refPathParser, ContextDataExtractor contextDataExtractor) {
		this.methodInfoService = methodInfoService;
		this.xmxService = xmxService;
		this.mapperService = mapperService;
		this.refPathParser = refPathParser;
		this.contextDataExtractor = contextDataExtractor;
	}

	@Override
	public Map<String, Collection<ExtendedClassInfoDto>> getAppsAndClasses() {
		Map<String, Collection<ExtendedClassInfoDto>> appsClassesMap = new LinkedHashMap<>();
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
					List<ExtendedClassInfoDto> extendedXmxClassInfos = new ArrayList<>(managedClassInfos.size());
					for (XmxClassInfo managedClassInfo : managedClassInfos) {
						List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(managedClassInfo.getId());
						int numObjects = managedObjects.size();
						String proxyClass = null;
						if (numObjects == 1) {
							// special case - check if proxy is detected
							Object proxy = managedObjects.get(0).getProxy();
							proxyClass = proxy == null ? null : proxy.getClass().getName();
						}
						ExtendedClassInfoDto extendedXmxClassInfo = new ExtendedClassInfoDto(managedClassInfo, numObjects, proxyClass);
						extendedXmxClassInfos.add(extendedXmxClassInfo);
					}

					appsClassesMap.put(applicationName, extendedXmxClassInfos);
				}
			}
		}
		return appsClassesMap;
	}

	@Override
	public XmxObjectInfo getManagedClassSingleInstance(int classId) {
		List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(classId);
		if (managedObjects.size() == 1) {
			return managedObjects.get(0);
		} else {
			return null;
		}
	}

	@Override
	public List<ObjectInfoDto> getManagedClassInstancesInfo(Integer classId) {
		List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(classId);
		if (managedObjects.isEmpty()) {
			return Collections.emptyList();
		}
		List<ObjectInfoDto> objectsInfo = new ArrayList<>(managedObjects.size());
		ClassInfoDto classDto = new ClassInfoDto(managedObjects.get(0).getClassInfo());
		for (XmxObjectInfo o : managedObjects) {
			objectsInfo.add(toObjectInfoDto(o, classDto, CLASS_OBJS_JSON_CHARS_LIMIT));
		}
		return objectsInfo;
	}

	@Override
	public ExtendedObjectInfoDto getExtendedObjectDetails(String refpath, int arrPageNum) throws MissingObjectException,
			MissingProxyException, RefPathSyntaxException, NotSingletonException {
		XmxObjectInfo objectInfo = findObject(refpath).foundObjectInfo;
		Object obj = objectInfo.getValue();

		ExtendedObjectInfoDto.ArrayPageDetails arrayPage = getArrayPageDetails(obj, arrPageNum);
		Map<String, List<XmxFieldInfo>> fieldsByClass = fillFieldsInfoByClass(objectInfo);
		Map<String, List<XmxMethodInfo>> methodsByClass = fillMethodsInfoByClass(objectInfo);

		List<String> classNames = new ArrayList<>();
		if (obj != null) {
			Class<?> clazz = obj.getClass();
			while (clazz != null) {
				classNames.add(clazz.getName());
				clazz = clazz.getSuperclass();
			}
		} else {
			classNames.add("null");
		}
		return new ExtendedObjectInfoDto(objectInfo.getObjectId(), new ClassInfoDto(objectInfo.getClassInfo()),
				toText(obj, OBJ_JSON_CHARS_LIMIT),
				classNames, fieldsByClass, methodsByClass, arrayPage,
				getPermaRefPath(objectInfo, refpath), getProxyClass(objectInfo));
	}

	private String getPermaRefPath(XmxObjectInfo objectInfo, String refpath) {
		if (objectInfo.getObjectId() > 0 && objectInfo.getValue() != null && !refpath.startsWith(PERMA_PATH_PREFIX)) {
			SingletonPermanentId permaId = xmxService.getSingletonPermanentId(objectInfo.getObjectId());
			if (permaId != null) {
				int n = refpath.indexOf('.');
				return PERMA_PATH_PREFIX + permaId + ':' + (n < 0 ? "" : refpath.substring(n + 1));
			}
		}
		return null;
	}

	private Map<String, List<XmxMethodInfo>> fillMethodsInfoByClass(XmxObjectInfo objectInfo) {
		Map<String, List<XmxMethodInfo>> methodsByClass = new LinkedHashMap<>();
		Map<String, Method> managedMethods = objectInfo.getMembersLookup().listManagedMethods();
		for (Map.Entry<String, Method> e : managedMethods.entrySet()) {
			String methodId = e.getKey();
			Method m = e.getValue();
			String declaringClassName = m.getDeclaringClass().getName();

			List<XmxMethodInfo> classMethodsInfo = methodsByClass.get(declaringClassName);
			if (classMethodsInfo == null) {
				classMethodsInfo = new ArrayList<>();
				methodsByClass.put(declaringClassName, classMethodsInfo);
			}
			classMethodsInfo.add(toMethodInfo(methodId, m));
		}
		return methodsByClass;
	}

	private XmxMethodInfo toMethodInfo(String methodId, Method m) {
		String methodNameTypeSignature = methodInfoService.getMethodNameTypeSignature(m);
		return new XmxMethodInfo(methodId, m.getName(), methodNameTypeSignature,
				methodInfoService.getMethodParameterDescriptions(m), m.getModifiers());
	}

	private Map<String, List<XmxFieldInfo>> fillFieldsInfoByClass(XmxObjectInfo objectInfo) {
		Object obj = objectInfo.getValue();
		Map<String, List<XmxFieldInfo>> fieldsByClass = new LinkedHashMap<>();
		if (obj != null) {
			Map<String, Field> managedFields = objectInfo.getMembersLookup().listManagedFields();
			for (Map.Entry<String, Field> e : managedFields.entrySet()) {
				String fid = e.getKey();
				Field f = e.getValue();
				reflAccessor.makeAccessible(f);

				String declaringClassName = f.getDeclaringClass().getName();

				List<XmxFieldInfo> classFieldsInfo = fieldsByClass.get(declaringClassName);
				if (classFieldsInfo == null) {
					classFieldsInfo = new ArrayList<>();
					fieldsByClass.put(declaringClassName, classFieldsInfo);
				}

				XmxObjectTextRepresentation textValue = safeFieldToText(obj, f, OBJ_FIELDS_JSON_CHARS_LIMIT);
				XmxFieldInfo fi = new XmxFieldInfo(fid, f.getName(),
						f.getModifiers(), textValue);
				classFieldsInfo.add(fi);
			}
		}
		return fieldsByClass;
	}

	private ExtendedObjectInfoDto.ArrayPageDetails getArrayPageDetails(Object obj, int arrPageNum) {
		if (obj != null && obj.getClass().isArray()) {
			Object[] objArr = (Object[]) obj;
			int pageNum = arrPageNum > 0 && arrPageNum * ARRAY_PAGE_LENGTH < objArr.length ? arrPageNum : 0;
			int pageStart = ARRAY_PAGE_LENGTH * arrPageNum;
			int pageLength = Math.min(ARRAY_PAGE_LENGTH, objArr.length - pageStart);
			XmxObjectTextRepresentation[] pageElements = new XmxObjectTextRepresentation[pageLength];
			for (int i = 0; i < pageLength; i++) {
				pageElements[i] = toText(objArr[pageStart + i], OBJ_FIELDS_JSON_CHARS_LIMIT);
			}
			return new ExtendedObjectInfoDto.ArrayPageDetails(objArr.length, pageNum, pageElements);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchObjectResult findObject(String refpath) throws MissingObjectException, MissingProxyException,
			RefPathSyntaxException, NotSingletonException {

		RefPath refPathInfo = refPathParser.parse(refpath);
		RefPathRoot root = refPathInfo.getRoot();
		List<RefPathSuffix> suffixes = refPathInfo.getSuffixes();

		XmxObjectInfo rootObjectInfo;
		if (root instanceof RefPathSingletonRoot) {
			// permanent path like "$:'APP':CLASS:" or "$:'APP':CLASS:.f1.f2"
			RefPathSingletonRoot sr = (RefPathSingletonRoot) root;
			rootObjectInfo = xmxService.getSingletonObject(new SingletonPermanentId(sr.getAppName(), sr.getClassName()));
		} else {
			assert root instanceof RefPathIdRoot : "Unknown RefPathRoot class";
			// general refpath like "$123" or "$123.f1.f2"
			int objectId = ((RefPathIdRoot) root).getObjectId();
			rootObjectInfo = getXmxObject(objectId);
		}

		if (refPathInfo.isRequireProxy()) {
			Object proxy = rootObjectInfo.getProxy();
			if (proxy == null) {
				throw new MissingProxyException(rootObjectInfo.getObjectId());
			}
			rootObjectInfo = getUnmanagedObjectInfo(proxy);
		}

		if (suffixes.isEmpty()) {
			return new SearchObjectResult(rootObjectInfo.getValue(), rootObjectInfo);
		}

		Object curObj = rootObjectInfo.getValue();
		for (int curLevel = 0; curLevel < suffixes.size(); curLevel++) {
			curObj = getNextLevelElement(refPathInfo, curObj, curLevel);
		}
		XmxObjectInfo foundObjectInfo = getUnmanagedObjectInfo(curObj);
		return new SearchObjectResult(rootObjectInfo.getValue(), foundObjectInfo);
	}

	private Object getNextLevelElement(RefPath refPath, Object source, int level) throws RefPathSyntaxException {
//		String rootPath, List<String> parts;
		RefPathSuffix suffix = refPath.getSuffixes().get(level);
		if (source == null) {
			throw new RefPathSyntaxException("Null object for path", refPath.buildPath(level));
		}
		if (suffix instanceof RefPathArrayElementSuffix) {
			if (!source.getClass().isArray()) {
				throw new RefPathSyntaxException("Expected an array, but got " + source.getClass(),
						refPath.buildPath(level));
			}
			try {
				source = Array.get(source, ((RefPathArrayElementSuffix) suffix).getElementIndex());
			} catch (IndexOutOfBoundsException e) {
				throw new RefPathSyntaxException("Invalid array index '" + suffix + "'", refPath.buildPath(level));
			}
		} else if (suffix instanceof RefPathBeanSuffix) {
			// expect a Spring bean name
			Class<?> c = source.getClass();
			Object beanFactory = contextDataExtractor.getBeanFactory(source);
			if (beanFactory == null) {
				throw new RefPathSyntaxException("Expects a refreshed Spring ApplicationContext as parent object but got " + c + " instead",
						refPath.buildPath(level));
			}
			String beanName = ((RefPathBeanSuffix) suffix).getBeanName();
			if (((RefPathBeanSuffix) suffix).isUseDefinition()) {
				// look for a bean definition
				Method mGetBeanDefinition = safeFindMethod(beanFactory, "org.springframework.beans.factory.support.DefaultListableBeanFactory", "getBeanDefinition", String.class);
				Object bd = ReflectionUtils.safeInvokeMethod(mGetBeanDefinition, beanFactory, beanName);
				if (bd == null) {
					throw new RefPathSyntaxException("Failed to get bean definition named '" + beanName + "' in " + source,
							refPath.buildPath(level));
				}
				source = bd;
			} else {
				// look for a bean
				Method mGetBean = ReflectionUtils.safeFindMethod(beanFactory, "org.springframework.beans.factory.support.AbstractBeanFactory", "getBean", String.class);
				Object bean = ReflectionUtils.safeInvokeMethod(mGetBean, beanFactory, beanName);
				if (bean == null) {
					throw new RefPathSyntaxException("Failed to get bean named '" + beanName + "' in " + source,
							refPath.buildPath(level));
				}
				source = bean;
			}

		} else {
			assert suffix instanceof RefPathFieldSuffix : "Unknown suffix kind " + suffix;
			String fieldName = ((RefPathFieldSuffix) suffix).getFieldName();
			// expect a path part to indicate a field
			Class<?> c = source.getClass();
			Field f = getField(c, fieldName, refPath.buildPath(level));
			try {
				reflAccessor.makeAccessible(f);
				source = f.get(source);
			} catch (IllegalAccessException e) {
				throw new RefPathSyntaxException("Failed to get field '" + fieldName + "' in class " + c,
						refPath.buildPath(level));
			}
		}
		return source;
	}

	private Field getField(Class<?> origClass, String fid, String refpath) throws RefPathSyntaxException {
		Class<?> c = origClass;
		int n = fid.indexOf('^');
		String fname;
		int superLevel = 0;
		if (n >= 0) {
			fname = fid.substring(0, n);
			String levelStr = fid.substring(n + 1);
			try {
				superLevel = Integer.parseInt(levelStr);
			} catch (NumberFormatException e) {
				throw new RefPathSyntaxException("Invalid refpath field part '" + fid + "'", refpath);
			}
		} else {
			fname = fid;
		}

		if (superLevel > 0) {
			for (int i = 0; i < superLevel; i++) {
				c = c.getSuperclass();
				if (c == null) {
					throw new RefPathSyntaxException("Invalid ^superLevel for refpath field part '" +
							fid + "' and class " + origClass, refpath);
				}
			}
			try {
				return c.getDeclaredField(fname);
			} catch (NoSuchFieldException e) {
				// give up
			}
		} else {
			while (c != null) {
				try {
					return c.getDeclaredField(fname);
				} catch (NoSuchFieldException e) {
					// try search in superclass
					c = c.getSuperclass();
				}
			}
		}
		throw new RefPathSyntaxException("Field '" + fid + "' is not found in " + origClass, refpath);
	}

	private XmxObjectInfo getUnmanagedObjectInfo(Object obj) {
		XmxClassInfo ci;
		if (obj == null) {
			ci = new XmxClassInfo(null, "null", new EmptyMembersLookup());
		} else {
			ci = xmxService.getClassInfo(obj.getClass());
		}
		return new XmxObjectInfo(ID_UNMANAGED, ci, obj, null);
	}

	@Override
	public XmxMethodResult invokeObjectMethod(String refpath, String methodId, String[] argsArr) throws Throwable {
		SearchObjectResult searchResult = findObject(refpath);
		XmxObjectInfo objectInfo = searchResult.foundObjectInfo;
		Object obj = objectInfo.getValue();

		final Method m = objectInfo.getMembersLookup().getManagedMethod(methodId);
		if (m == null) {
			throw new XmxRuntimeException("Method not found in " + obj.getClass() +
					" by ID=" + methodId);
		}
		reflAccessor.makeAccessible(m);

		XmxMethodInfo methodInfo = toMethodInfo(methodId, m);
		XmxMethodResult resultInfo = new XmxMethodResult(m.getDeclaringClass().getName(), methodInfo);

		// set context class loader to enable functionality which depends on it, like JNDI
		ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(chooseContextClassLoader(searchResult));

		XmxObjectTextRepresentation resultText;
		try {
			Object[] args = translateArgs(argsArr, m, chooseContextClassLoader(searchResult));
			Object result = m.invoke(obj, args);
			resultText = toText(result, 0);
			resultInfo.setResult(resultText);
		} catch (InvocationTargetException e) {
			resultInfo.setException(e.getCause());
		} finally {
			Thread.currentThread().setContextClassLoader(prevClassLoader);
		}
		return resultInfo;
	}

	/**
	 * Choose the most specific class loader from class loaders of the root and the found object.
	 * The resulting class loader will be used as the context class loader for de-serializing
	 * the JSON values related to the found object (e.g. new field value or a method argument)
	 * <p/>
	 * Note that it is not enough to just get the class loader of the found object, as it may be a List of
	 * custom objects, so its class loader is a system one.
	 * <p/>
	 * In future, it may be changed to a Set of class loaders for all intermediate objects traversed during
	 * the search.
	 */
	private ClassLoader chooseContextClassLoader(SearchObjectResult result) {
		// prefer CL of "found" object; use "root" CL only if the found object is null, or its CL is bootstrap or system
		ClassLoader rootObjectCL = result.rootObject.getClass().getClassLoader();
		if (result.foundObjectInfo.getValue() == null || result.foundObjectInfo.getValue() == result.rootObject) {
			return rootObjectCL;
		}
		ClassLoader foundObjectCL = result.foundObjectInfo.getValue().getClass().getClassLoader();

		if (foundObjectCL == null || foundObjectCL == ClassLoader.getSystemClassLoader()) {
			return rootObjectCL;
		} else {
			return foundObjectCL;
		}
	}

	@Override
	public void setObjectFieldOrElement(String refpath, String elementId, String value)
			throws MissingObjectException, RefPathSyntaxException, NotSingletonException, MissingProxyException {
		SearchObjectResult searchResult = findObject(refpath);
		XmxObjectInfo objectInfo = searchResult.foundObjectInfo;
		Object obj = objectInfo.getValue();

		Class<?> objClass = obj.getClass();
		if (objClass.isArray()) {
			Class<?> componentType = objClass.getComponentType();
			Object deserializedValue = deserializeValue(value, componentType, chooseContextClassLoader(searchResult));
			try {
				int idx = Integer.parseInt(elementId);
				Array.set(obj, idx, deserializedValue);
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				throw new RefPathSyntaxException("Invalid array index '" + elementId + "'", refpath);
			} catch (Exception e) {
				// e.g. catches IllegalArgumentException for wrong component type
				throw new XmxRuntimeException("Failed to set array element", e);
			}
		} else {
			Field f = objectInfo.getMembersLookup().getManagedField(elementId);
			if (f == null) {
				throw new XmxRuntimeException("Field not found in " + objClass +
						" by ID=" + elementId);
			}
			reflAccessor.makeAccessible(f);

			Object deserializedValue = deserializeValue(value, f.getType(), chooseContextClassLoader(searchResult));
			try {
				f.set(obj, deserializedValue);
			} catch (Exception e) {
				throw new XmxRuntimeException("Failed to set field", e);
			}
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
	public void printFullObjectJson(String refpath, String fid, PrintWriter out) throws IOException, RefPathSyntaxException, MissingObjectException, NotSingletonException, MissingProxyException {
		XmxObjectInfo objectInfo = findObject(refpath).foundObjectInfo;
		Object jsonSourceObject;
		Object obj = objectInfo.getValue();
		if (fid != null) {
			Field f = objectInfo.getMembersLookup().getManagedField(fid);
			if (f == null) {
				out.println("Error: the field is missing!");
				return;
			}
			reflAccessor.makeAccessible(f);
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

	private XmxObjectInfo getXmxObject(Integer objectId) throws MissingObjectException {
		final XmxObjectInfo objInfo = xmxService.getManagedObject(objectId);
		if (objInfo == null) {
			throw new MissingObjectException(objectId);
		}
		return objInfo;
	}

	private ObjectInfoDto toObjectInfoDto(XmxObjectInfo info, ClassInfoDto classDto, long jsonCharsLimit) {
		if (classDto == null) {
			classDto = new ClassInfoDto(info.getClassInfo());
		}
		return new ObjectInfoDto(info.getObjectId(), classDto,
				toText(info.getValue(), jsonCharsLimit),
				getProxyClass(info));
	}

	private String getProxyClass(XmxObjectInfo info) {
		if (info.getProxy() == null) {
			return null;
		}
		return info.getProxy().getClass().getName();
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
	 * @param contextCL the context class loader to be used for de-serialization
	 *
	 * @return the array of objects which may be used to invoke the method
	 */
	private Object[] translateArgs(String[] args, Method m, ClassLoader contextCL) throws RefPathSyntaxException, MissingObjectException, NotSingletonException, MissingProxyException {
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
			methodArgs[i] = deserializeValue(args[i], type, contextCL);
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


	private Object deserializeValue(String value, Class<?> formalType, ClassLoader contextCL)
			throws RefPathSyntaxException, MissingObjectException, NotSingletonException, MissingProxyException {
		if (value.startsWith("$")) {
			// value is refpath of the actual object
			return findObject(value).foundObjectInfo.getValue();
		}
		final ClassLoader prevContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(contextCL);
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

	private static String initSessionId() {
		return Long.toHexString(System.currentTimeMillis() & 0xFFFFFFFFL);
	}

	@Override
	public String getCurrentSessionId() {
		return sessionId;
	}
}

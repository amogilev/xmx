package am.xmx.ui;

import am.xmx.dto.*;
import am.xmx.service.IMapperService;
import am.xmx.service.IXmxService;
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

	private static YaGson jsonMapper = new YaGson();

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(RestController.class);

	@Autowired
	private IXmxService xmxService;

	@Autowired
	private IMapperService mapperService;

	@RequestMapping(method = RequestMethod.GET)
	public String getAppsAndClasses(ModelMap model) {
		Map<String, Collection<AdvancedXmxClassInfo>> appsClassesMap = new LinkedHashMap<>();
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
					List<AdvancedXmxClassInfo> advancedXmxClassInfos = new ArrayList<>(managedClassInfos.size());
					for (XmxClassInfo managedClassInfo : managedClassInfos) {
						AdvancedXmxClassInfo advancedXmxClassInfo = new AdvancedXmxClassInfo(
								managedClassInfo.getId(),
								managedClassInfo.getClassName());
						advancedXmxClassInfo.setNumberOfObjects(CollectionUtils.size(
										xmxService.getManagedObjects(advancedXmxClassInfo.getId()))
						);
						advancedXmxClassInfos.add(advancedXmxClassInfo);
					}

					appsClassesMap.put(applicationName, advancedXmxClassInfos);
				}
			}
		}
		model.addAttribute("managedAppsClassesMap", appsClassesMap);
		return "appsClasses";
	}

	@RequestMapping(value = "getClassObjects", method = RequestMethod.GET)
	public String getClassObjects(ModelMap model, @RequestParam(required = true) Integer classId, @RequestParam(required = true) String className) {
		List<XmxObjectInfo> managedObjects = xmxService.getManagedObjects(classId);
		if (managedObjects.size() == 1) {
			// fast path for singletons
			return getObjectDetails(model, managedObjects.get(0).getObjectId());
			
			// alternative way to change URL 
			// return "redirect:/getObjectDetails?objectId=" + managedObjects.get(0).getObjectId();
		}
		model.addAttribute("className", className);
		model.addAttribute("objects", managedObjects);
		return "classObjects";
	}

	@RequestMapping(value = "getObjectDetails", method = RequestMethod.GET)
	public String getObjectDetails(ModelMap model, @RequestParam(required = true) Integer objectId) {
		model.addAttribute("objectId", objectId);
		XmxObjectDetails details = xmxService.getObjectDetails(objectId);
		if (details == null) {
			return "missingObject";
		} 
		
		String className = details.getClassesNames().get(0);
		model.addAttribute("className", className);
		model.addAttribute("details", details);
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
	public String setObjectField(ModelMap model, @RequestParam(required = true) Integer objectId, 
			@RequestParam(required = true) Integer fieldId, @RequestParam(required = true) String value) {
		model.addAttribute("objectId", objectId);

		final Object obj = xmxService.getObjectById(objectId);
		if (obj == null) {
			return "missingObject";
		}
		final Field f = xmxService.getObjectFieldById(obj, fieldId);
		if (f == null) {
			throw new XmxRuntimeException("Field not found in " + obj.getClass().getName() + " by ID=" + fieldId);
		}

		Object deserializedValue = deserializeValue(value, f.getType(), obj);
		try {
			f.set(obj, deserializedValue);
		} catch (Exception e) {
			throw new XmxRuntimeException("Failed to set field", e);
		}

		XmxObjectDetails updatedDetails = xmxService.getObjectDetails(objectId);
		if (updatedDetails == null) {
			return "missingObject";
		} 
		model.addAttribute("details", updatedDetails);
		return "objectDetails";
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
		
		model.addAttribute("objectId", objectId);
		Object obj = xmxService.getObjectById(objectId);
		if (obj == null) {
			return "missingObject";
		}

		Method m = xmxService.getObjectMethodById(obj, methodId);
		// set context class loader to enable functionality which depends on it, like JNDI
		ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(obj.getClass().getClassLoader());

		Object result;
		try {
			result = m.invoke(obj, translateArgs(argsArr, m, obj));
			model.addAttribute("result", toText(result));
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
					out.println("  id=" + objectInfo.getObjectId() + ", json=" + objectInfo.getJsonRepresentation());
				}
				out.println("-------------");
			}
			out.println("=============\n");
		}
	}

	private XmxObjectTextRepresentation toText(Object obj) {
		return new XmxObjectTextRepresentation(mapperService.safeToString(obj), mapperService.safeToJson(obj));
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
}
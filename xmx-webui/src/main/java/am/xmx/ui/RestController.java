package am.xmx.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;
import am.xmx.service.IXmxService;

@Controller
@RequestMapping("/")
public class RestController {


	@Autowired
	private IXmxService xmxService;

	@RequestMapping(method = RequestMethod.GET)
	public String getAppsAndClasses(ModelMap model) {
		Map<String, Collection<AdvancedXmxClassInfo>> appsClassesMap = new HashMap<>();
		List<String> applicationNames = xmxService.getApplicationNames();
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
		// TODO: support missing objects
		model.addAttribute("objectId", objectId);
		XmxObjectDetails details = xmxService.getObjectDetails(objectId);
		String className = details.getClassesNames().get(0);
		model.addAttribute("className", className);
		model.addAttribute("details", details);
		return "objectDetails";
	}


	@RequestMapping(value = "setObjectField", method = RequestMethod.GET)
	public String setObjectField(ModelMap model, @RequestParam(required = true) Integer objectId, 
			@RequestParam(required = true) Integer fieldId, @RequestParam(required = true) String value) {
		// TODO: support missing objects
		model.addAttribute("objectId", objectId);
		XmxObjectDetails updatedDetails = xmxService.setObjectField(objectId, fieldId, value);
		model.addAttribute("details", updatedDetails);
		return "objectDetails";
	}
	
	@RequestMapping(value = "invokeMethod", method = RequestMethod.GET)
	public String invokeObjectMethod(ModelMap model, @RequestParam(required = true) Integer objectId, 
			@RequestParam(required = true) Integer methodId) {
		// TODO: support missing objects
		model.addAttribute("objectId", objectId);
		// TODO obtain args from request
		String[] args = new String[0];
		
		// TODO support exceptions
		
		Object obj = xmxService.getObjectById(objectId);
		if (obj != null) {
			Method m = xmxService.getObjectMethodById(obj, methodId);
			Object result = xmxService.invokeObjectMethod(obj, m, translateArgs(args, m));
			model.addAttribute("result", Objects.toString(result));
		} else {
			model.addAttribute("result", "<<Object not found, probably it is GC'ed>>");
		}
		
		return "methodResult";
	}

	/**
	 * Converts arguments from Strings to Objects. Formal types of the arguments are taken
	 * from the method's reflection info.
	 * 
	 * @param args the arguments as Strings
	 * @param m the method to be invoked with the arguments
	 * 
	 * @return the array of objects which may be used to invoke the method
	 */
	private Object[] translateArgs(String[] args, Method m) {
		
		Class<?>[] parameterTypes = m.getParameterTypes();
		// less or equal number of args are supported
		if (parameterTypes.length < args.length) {
			throw new XmxRuntimeException("Too many arguments for the method " + m);
		}
		Object[] methodArgs = new Object[parameterTypes.length]; 
		for (int i = 0; i < args.length; i++) {
			Class<?> type = parameterTypes[i];
			if (!type.equals(String.class)) {
				// TODO: support json
				throw new XmxRuntimeException("NOT IMPLEMENTED: Only String's args are supported now for invokeObjectMethod()");
			} else {
				methodArgs[i] = args[i];
			}
		}
		
		return methodArgs;
	}

}
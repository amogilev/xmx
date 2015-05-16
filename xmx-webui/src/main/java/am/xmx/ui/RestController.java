package am.xmx.ui;

import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.service.IXmxService;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		model.addAttribute("className", className);
		model.addAttribute("objects", xmxService.getManagedObjects(classId));
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
		// TODO support args
		// TODO support exceptions
		String result = xmxService.invokeObjectMethod(objectId, methodId);
		model.addAttribute("result", result);
		return "methodResult";
	}

}
// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.ui.dto.ExtendedXmxClassInfo;
import com.gilecode.xmx.ui.dto.ExtendedXmxObjectDetails;
import com.gilecode.xmx.ui.dto.ExtendedXmxObjectInfo;
import com.gilecode.xmx.ui.dto.XmxObjectTextRepresentation;
import com.gilecode.xmx.ui.service.IXmxUiService;
import com.gilecode.xmx.ui.service.MissingObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class RestController implements UIConstants {

	private IXmxUiService xmxUiService;

	@Autowired
	public RestController(IXmxUiService xmxUiService) {
		this.xmxUiService = xmxUiService;
	}

	@ExceptionHandler(MissingObjectException.class)
	public String missingObject() {
		return "missingObject";
	}

	@RequestMapping(method = RequestMethod.GET)
	public String handleGetAppsAndClasses(ModelMap model) {
		Map<String, Collection<ExtendedXmxClassInfo>> appsClassesMap = xmxUiService.getAppsAndClasses();
		model.addAttribute("managedAppsClassesMap", appsClassesMap);
		return "appsClasses";
	}

	@RequestMapping(value = "getClassObjects", method = RequestMethod.GET)
	public String handleGetClassObjects(ModelMap model, @RequestParam Integer classId, @RequestParam String className) {
		Integer singletonId = xmxUiService.getManagedClassSingleInstanceId(classId);
		if (singletonId != null) {
			// fast path for singletons
			 return "redirect:/getObjectDetails?objectId=" + singletonId;
		}
		List<ExtendedXmxObjectInfo> extObjectsInfo = xmxUiService.getManagedClassInstancesInfo(classId);
		model.addAttribute("className", className);
		model.addAttribute("objects", extObjectsInfo);
		return "classObjects";
	}

	@RequestMapping(value = "getObjectDetails", method = RequestMethod.GET)
	public String getObjectDetails(ModelMap model,
               @RequestParam Integer objectId,
               @RequestParam(required = false, defaultValue = "SMART") ValuesDisplayKind valKind) throws MissingObjectException {
		model.addAttribute("objectId", objectId);
		ExtendedXmxObjectDetails details = xmxUiService.getExtendedObjectDetails(objectId);
		String className = details.getClassesNames().get(0);
		model.addAttribute("className", className);
		model.addAttribute("details", details);
		model.addAttribute("valKind", valKind);

		return "objectDetails";
	}

	@RequestMapping(value = "setObjectField", method = RequestMethod.GET)
	public String handleSetObjectField(ModelMap model, @RequestParam Integer objectId,
			@RequestParam Integer fieldId, @RequestParam String value) throws MissingObjectException {
		model.addAttribute("objectId", objectId);

		xmxUiService.setObjectField(objectId, fieldId, value);

		ExtendedXmxObjectDetails updatedDetails = xmxUiService.getExtendedObjectDetails(objectId);
		model.addAttribute("details", updatedDetails);

		return "redirect:/getObjectDetails";
	}

	@RequestMapping(value = "invokeMethod", method = RequestMethod.POST)
	public String handleInvokeObjectMethod(
			ModelMap model,
			@RequestParam int objectId,
			@RequestParam int methodId,
			@RequestParam(value = "arg", required = false) String[] argsArr) throws Throwable {

		XmxObjectTextRepresentation resultText = xmxUiService.invokeObjectMethod(objectId, methodId, argsArr);

		model.addAttribute("objectId", objectId);
		model.addAttribute("result", resultText);
		return "methodResult";
	}

	@RequestMapping(value = "reportAllObjects", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void getAllObjectsReport(HttpServletResponse resp) throws IOException {
		xmxUiService.printAllObjectsReport(resp.getWriter());
	}

	@RequestMapping(value = "getFullJson", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void loadFullJson(HttpServletResponse resp,
							 @RequestParam(value = "objectId") int objectId,
							 @RequestParam(value = "fieldId", required = false) Integer fieldId) throws IOException {
		xmxUiService.printFullObjectJson(objectId, fieldId, resp.getWriter());
	}
}
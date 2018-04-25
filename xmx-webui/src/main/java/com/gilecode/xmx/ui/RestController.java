// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.ui.dto.ExtendedClassInfoDto;
import com.gilecode.xmx.ui.dto.ExtendedObjectInfoDto;
import com.gilecode.xmx.ui.dto.ObjectInfoDto;
import com.gilecode.xmx.ui.dto.XmxObjectTextRepresentation;
import com.gilecode.xmx.ui.service.IXmxUiService;
import com.gilecode.xmx.ui.service.MissingObjectException;
import com.gilecode.xmx.ui.service.RefPathSyntaxException;
import com.gilecode.xmx.ui.service.XmxSessionExpiredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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
	public ModelAndView missingObject(MissingObjectException ex) {
		ModelAndView mav = new ModelAndView("missingObject");
		mav.addObject("refpath", "$" + ex.getMissingObjectId());
		return mav;
	}

	@ExceptionHandler(RefPathSyntaxException.class)
	public ModelAndView badRefpath(RefPathSyntaxException ex) {
		ModelAndView mav = new ModelAndView("badRefpath");
		mav.addObject("refpath", ex.getRefpath());
		mav.addObject("cause", ex.getMessage());
		return mav;
	}

	@ExceptionHandler(XmxSessionExpiredException.class)
	public String sessionExpired(XmxSessionExpiredException ex) {
		return "errSessionExpired";
	}

	void checkSessionId(String sid) {
		if (!xmxUiService.getCurrentSessionId().equals(sid)) {
			throw new XmxSessionExpiredException();
		}
	}

	@RequestMapping(method = RequestMethod.GET)
	public String handleGetAppsAndClasses(ModelMap model) {
		Map<String, Collection<ExtendedClassInfoDto>> appsClassesMap = xmxUiService.getAppsAndClasses();
		model.addAttribute("managedAppsClassesMap", appsClassesMap);
		model.addAttribute(ATTR_SESSION_ID, xmxUiService.getCurrentSessionId());
		return "appsClasses";
	}

	@RequestMapping(value = "getClassObjects", method = RequestMethod.GET)
	public String handleGetClassObjects(ModelMap model, @RequestParam Integer classId, @RequestParam String className,
	                                    @RequestParam(ATTR_SESSION_ID) String sessionId) {
		checkSessionId(sessionId);
		Integer singletonId = xmxUiService.getManagedClassSingleInstanceId(classId);
		if (singletonId != null) {
			// fast path for singletons
			return "redirect:/getObjectDetails/$" + singletonId + "?" + ATTR_SESSION_ID + "=" + sessionId;
		}
		List<ObjectInfoDto> extObjectsInfo = xmxUiService.getManagedClassInstancesInfo(classId);
		model.addAttribute("className", className);
		model.addAttribute("objects", extObjectsInfo);
		model.addAttribute(ATTR_SESSION_ID, sessionId);
		return "classObjects";
	}

	@RequestMapping(value = "getObjectDetails/{refpath:.+}", method = RequestMethod.GET)
	public String getObjectDetails(ModelMap model,
				@PathVariable String refpath,
				@RequestParam(ATTR_SESSION_ID) String sessionId,
				@RequestParam(required = false, defaultValue = "SMART") ValuesDisplayKind valKind,
				@RequestParam(required = false, defaultValue = "0") int arrPage)
			throws MissingObjectException, RefPathSyntaxException {
		checkSessionId(sessionId);
		ExtendedObjectInfoDto details = xmxUiService.getExtendedObjectDetails(refpath, arrPage);
		String className = details.getClassesNames().get(0);
		model.addAttribute("refpath", refpath);
		model.addAttribute("className", className);
		model.addAttribute("details", details);
		model.addAttribute("valKind", valKind);
		model.addAttribute(ATTR_SESSION_ID, sessionId);

		return "objectDetails";
	}

	@RequestMapping(value = "setObjectElement/{refpath:.+}", method = RequestMethod.GET)
	public String handleSetObjectField(ModelMap model,
				@PathVariable String refpath,
				@RequestParam String elementId, @RequestParam String value,
				@RequestParam(ATTR_SESSION_ID) String sessionId)
			throws MissingObjectException, RefPathSyntaxException {

		checkSessionId(sessionId);
		xmxUiService.setObjectFieldOrElement(refpath, elementId, value);

		ExtendedObjectInfoDto updatedDetails = xmxUiService.getExtendedObjectDetails(refpath, 0);
		model.addAttribute("details", updatedDetails);

		return "redirect:/getObjectDetails/" + refpath + "?" + ATTR_SESSION_ID + "=" + sessionId;
	}

	@RequestMapping(value = "invokeMethod/{refpath:.+}", method = RequestMethod.POST)
	public String handleInvokeObjectMethod(
			ModelMap model,
			@PathVariable String refpath,
			@RequestParam String methodId,
			@RequestParam(value = "arg", required = false) String[] argsArr,
			@RequestParam(ATTR_SESSION_ID) String sessionId) throws Throwable {

		checkSessionId(sessionId);
		XmxObjectTextRepresentation resultText = xmxUiService.invokeObjectMethod(refpath, methodId, argsArr);

		model.addAttribute("refpath", refpath);
		model.addAttribute("result", resultText);
		return "methodResult";
	}

	@RequestMapping(value = "reportAllObjects", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void getAllObjectsReport(HttpServletResponse resp) throws IOException {
		xmxUiService.printAllObjectsReport(resp.getWriter());
	}

	@RequestMapping(value = "getFullJson/{refpath:.+}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public void loadFullJson(HttpServletResponse resp,
							 @PathVariable String refpath,
							 @RequestParam(ATTR_SESSION_ID) String sessionId,
							 @RequestParam(value = "fid", required = false) String fid)
			throws IOException, RefPathSyntaxException, MissingObjectException {
		checkSessionId(sessionId);
		xmxUiService.printFullObjectJson(refpath, fid, resp.getWriter());
	}

	// prevents splitting String by commas for @RequestParams of type String[]
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor(null));
	}
}
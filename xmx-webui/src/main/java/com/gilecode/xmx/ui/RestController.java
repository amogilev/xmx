// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.model.NotSingletonException;
import com.gilecode.xmx.model.XmxObjectInfo;
import com.gilecode.xmx.ui.dto.ExtendedClassInfoDto;
import com.gilecode.xmx.ui.dto.ExtendedObjectInfoDto;
import com.gilecode.xmx.ui.dto.ObjectInfoDto;
import com.gilecode.xmx.ui.dto.XmxMethodResult;
import com.gilecode.xmx.ui.refpath.RefPathSyntaxException;
import com.gilecode.xmx.ui.service.IXmxUiService;
import com.gilecode.xmx.ui.service.MissingObjectException;
import com.gilecode.xmx.ui.service.MissingProxyException;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

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

	@ExceptionHandler(MissingProxyException.class)
	public ModelAndView missingProxy(MissingProxyException ex) {
		ModelAndView mav = new ModelAndView("missingProxy");
		mav.addObject("refpath", "$" + ex.getObjectId());
		mav.addObject("sid", xmxUiService.getCurrentSessionId());
		return mav;
	}

	@ExceptionHandler(RefPathSyntaxException.class)
	public ModelAndView badRefpath(RefPathSyntaxException ex) {
		ModelAndView mav = new ModelAndView("badRefpath");
		mav.addObject("refpath", ex.getRefpath());
		mav.addObject("cause", ex.getMessage());
		return mav;
	}

	@ExceptionHandler(NotSingletonException.class)
	public ModelAndView notSingleton(NotSingletonException ex) {
		ModelAndView mav = new ModelAndView("notSingleton");
		mav.addObject("permaId", ex.getPermanentId());
		mav.addObject("reason", ex.getReason());
		return mav;
	}

	@ExceptionHandler(XmxSessionExpiredException.class)
	public String sessionExpired(XmxSessionExpiredException ex) {
		return "errSessionExpired";
	}

	@RequestMapping(method = RequestMethod.GET)
	public String handleGetAppsAndClasses(ModelMap model) {
		Map<String, Collection<ExtendedClassInfoDto>> appsClassesMap = xmxUiService.getAppsAndClasses();
		model.addAttribute("managedAppsClassesMap", appsClassesMap);
		model.addAttribute(ATTR_SESSION_ID, xmxUiService.getCurrentSessionId());
		return "appsClasses";
	}

	@RequestMapping(value = "getClassObjects", method = RequestMethod.GET)
	public String handleGetClassObjects(ModelMap model,
			@RequestParam Integer classId,
			@RequestParam String className,
	        @RequestParam(ATTR_SESSION_ID) String sessionId,
			@RequestParam(required = false) boolean proxyInformed) {
		checkSessionId(sessionId, "");
		XmxObjectInfo singletonObj = xmxUiService.getManagedClassSingleInstance(classId);

		if (singletonObj != null && (proxyInformed || singletonObj.getProxy() == null)) {
			// fast path for singletons
			String refpath = (singletonObj.getProxy() == null ? "" : PROXY_PATH_PREFIX)
					+ "$" + singletonObj.getObjectId();
			return "redirect:/getObjectDetails/" + refpath + "?" + ATTR_SESSION_ID + "=" + sessionId;
		}
		List<ObjectInfoDto> extObjectsInfo = xmxUiService.getManagedClassInstancesInfo(classId);

		Map<String, List<ObjectInfoDto>> proxiedObjects = new TreeMap<>();
		extObjectsInfo = groupProxiedObjects(extObjectsInfo, proxiedObjects);

		model.addAttribute("className", className);
		model.addAttribute("objects", extObjectsInfo);
		model.addAttribute("proxiedObjects", proxiedObjects);
		model.addAttribute(ATTR_SESSION_ID, sessionId);
		return "classObjects";
	}

	private List<ObjectInfoDto> groupProxiedObjects(List<ObjectInfoDto> objects, Map<String, List<ObjectInfoDto>> outProxiedObjects) {
		// fast path/common case = no proxied objects
		if (!hasProxiedObjects(objects)) {
			return objects;
		}
		List<ObjectInfoDto> nonProxiedObjects = new ArrayList<>(objects.size() - 1);
		for (ObjectInfoDto obj : objects) {
			String proxyClass = obj.getProxyClass();
			if (proxyClass != null) {
				List<ObjectInfoDto> proxiedObjects = outProxiedObjects.get(proxyClass);
				if (proxiedObjects == null) {
					proxiedObjects = new ArrayList<>();
					outProxiedObjects.put(proxyClass, proxiedObjects);
				}
				proxiedObjects.add(obj);
			} else {
				nonProxiedObjects.add(obj);
			}
		}

		return nonProxiedObjects;
	}

	private boolean hasProxiedObjects(List<ObjectInfoDto> objects) {
		for (ObjectInfoDto obj : objects) {
			if (obj.getProxyClass() != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the specified session ID and refpath correspnd to each other and the current session.
	 * <p/>
	 * Two cases allowed: permanent refpath with empty session, and valid current session ID. If neither of these
	 * conditions are met, throws {@link XmxSessionExpiredException}
	 *
	 * @return whether the permanent refpath is used
	 */
	private boolean checkSessionId(String sessionId, String refpath) {
		if (refpath.startsWith("$:")) {
			return true;
		} else if (!xmxUiService.getCurrentSessionId().equals(sessionId)) {
			throw new XmxSessionExpiredException();
		}
		return false;
	}

	private String decode(String refpath) {
		try {
			return URLDecoder.decode(refpath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// not really expected
			throw new RuntimeException(e);
		}
	}

	private String encode(String refpath) {
		try {
			return URLEncoder.encode(refpath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// not really expected
			throw new RuntimeException(e);
		}
	}

	@RequestMapping(value = "getObjectDetails/{refpath:.+}", method = RequestMethod.GET)
	public String getObjectDetails(ModelMap model,
				@PathVariable String refpath,
				@RequestParam(ATTR_SESSION_ID) String sessionId,
				@RequestParam(required = false, defaultValue = "SMART") ValuesDisplayKind valKind,
				@RequestParam(required = false, defaultValue = "0") int arrPage)
			throws MissingObjectException, RefPathSyntaxException, NotSingletonException, MissingProxyException {

		refpath = decode(refpath);
		boolean permanent = checkSessionId(sessionId, refpath);

		ExtendedObjectInfoDto details = xmxUiService.getExtendedObjectDetails(refpath, arrPage);
		String className = details.getClassesNames().get(0);
		model.addAttribute("refpath", refpath);
		model.addAttribute("className", className);
		model.addAttribute("details", details);
		model.addAttribute("valKind", valKind);
		model.addAttribute(ATTR_SESSION_ID, permanent ? "" : sessionId);

		return "objectDetails";
	}

	@RequestMapping(value = "setObjectElement/{refpath:.+}", method = RequestMethod.GET)
	public String handleSetObjectField(ModelMap model,
				@PathVariable String refpath,
				@RequestParam String elementId, @RequestParam String value,
				@RequestParam(ATTR_SESSION_ID) String sessionId)
			throws MissingObjectException, RefPathSyntaxException, NotSingletonException, MissingProxyException {

		refpath = decode(refpath);
		boolean permanent = checkSessionId(sessionId, refpath);

		xmxUiService.setObjectFieldOrElement(refpath, elementId, value);

		ExtendedObjectInfoDto updatedDetails = xmxUiService.getExtendedObjectDetails(refpath, 0);
		model.addAttribute("details", updatedDetails);

		return "redirect:/getObjectDetails/" + encode(refpath) +
				"?" + ATTR_SESSION_ID + "=" + (permanent ? "" : sessionId);
	}

	@RequestMapping(value = "invokeMethod/{refpath:.+}", method = RequestMethod.POST)
	public String handleInvokeObjectMethod(
			ModelMap model,
			@PathVariable String refpath,
			@RequestParam String methodId,
			@RequestParam(value = "arg", required = false) String[] argsArr,
			@RequestParam(ATTR_SESSION_ID) String sessionId) throws Throwable {

		refpath = decode(refpath);
		boolean permanent = checkSessionId(sessionId, refpath);

		XmxMethodResult resultInfo = xmxUiService.invokeObjectMethod(refpath, methodId, argsArr);

		model.addAttribute("refpath", refpath);
		model.addAttribute("methodAndResult", resultInfo);
		model.addAttribute(ATTR_SESSION_ID, permanent ? "" : sessionId);
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
			throws IOException, RefPathSyntaxException, MissingObjectException, NotSingletonException, MissingProxyException {
		refpath = decode(refpath);
		checkSessionId(sessionId, refpath);

		xmxUiService.printFullObjectJson(refpath, fid, resp.getWriter());
	}

	// prevents splitting String by commas for @RequestParams of type String[]
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor(null));
	}
}
// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.ui.smx.dto.BeanNameDto;
import com.gilecode.xmx.ui.smx.dto.VisData;
import com.gilecode.xmx.ui.smx.service.ISmxUiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.util.List;

@Controller
@RequestMapping("/smxs")
public class SpringMXController implements UIConstants {

    private ISmxUiService smxUiService;

    @Autowired
    public SpringMXController(ISmxUiService smxUiService) {
        this.smxUiService = smxUiService;
    }

    @GetMapping(path="visdata", produces = "application/json")
    @ResponseBody
    public VisData data(@RequestParam(name="appName", required = false) String appName,
                        @RequestParam(name="beanName", required = false) String beanName,
                        @RequestParam(name="expandContextId", required = false) String expandContextId) {
        return smxUiService.getVisData(appName, beanName, expandContextId);
    }

    @GetMapping(path="apps", produces = "application/json")
    @ResponseBody
    public List<String> apps() {
        return smxUiService.getAppNames();
    }

    @GetMapping(path="beanNames", produces = "application/json")
    @ResponseBody
    public List<BeanNameDto> beanNames(@RequestParam(name="appName", required = false) String appName) {
        return smxUiService.getBeans("null".equals(appName) ? null : appName);
    }

    @GetMapping("bean/{beanId:.+}")
    public String beanDetailsPage(@PathVariable("beanId") String beanId,
            @RequestParam(name="mode", required = false) String mode) {
        final String refpath;
        if ("def".equalsIgnoreCase(mode)) {
            refpath = beanId.replaceFirst("#", "##");
        } else if ("factory".equalsIgnoreCase(mode)) {
            refpath = beanId + ".beanFactory";
        } else {
            refpath = beanId;
        }
        return "redirect:/getObjectDetails/" + UriUtils.encodePathSegment(refpath, "UTF-8") + "?sid=" + smxUiService.getCurrentSessionId();
    }

}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.ui.smx.dto.BeanInfoDto;
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
                        @RequestParam(name="beanId", required = false) String beanId) {
        return smxUiService.getVisData(appName, beanId);
    }

    @GetMapping(path="apps", produces = "application/json")
    @ResponseBody
    public List<String> apps() {
        return smxUiService.getAppNames();
    }

    @GetMapping(path="beanNames", produces = "application/json")
    @ResponseBody
    public List<BeanInfoDto> beanNames(@RequestParam(name="appName", required = false) String appName) {
        return smxUiService.getBeans("null".equals(appName) ? null : appName);
    }

    @GetMapping("bean/{beanId:.+}")
    public String beanDetailsPage(@PathVariable("beanId") String beanId) {
        return "forward:/getObjectDetails/" + UriUtils.encodePathSegment(beanId, "UTF-8") + "?sid=" + smxUiService.getCurrentSessionId();
    }

}

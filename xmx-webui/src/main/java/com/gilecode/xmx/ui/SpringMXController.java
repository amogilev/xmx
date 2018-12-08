// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.ui.smx.dto.BeanInfoDto;
import com.gilecode.xmx.ui.smx.dto.VisData;
import com.gilecode.xmx.ui.smx.service.ISmxUiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public VisData data(@RequestParam(name="showAllBeans", required = false, defaultValue = "false") boolean showAllBeans,
                        @RequestParam(name="showBeansContextId", required = false) Integer showBeansContextId,
                        @RequestParam(name="filter", required = false) String filter) {
        return smxUiService.getVisData(showAllBeans, showBeansContextId, filter);
    }

    @GetMapping(path="apps", produces = "application/json")
    @ResponseBody
    public List<String> apps() {
        return smxUiService.getAppNames();
    }

    @GetMapping(path="beanNames", produces = "application/json")
    @ResponseBody
    public List<BeanInfoDto> beanNames(@RequestParam(name="app", required = false) String appName) {
        return smxUiService.getBeans("null".equals(appName) ? null : appName);
    }


}

// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.service;

import com.gilecode.xmx.model.XmxObjectInfo;
import com.gilecode.xmx.ui.smx.context.ContextBeanDisplayPredicateProvider;
import com.gilecode.xmx.ui.smx.dto.VisData;

import java.util.List;

public interface IVisDataService {
    void fillVisData(VisData data, String appName, List<XmxObjectInfo> ctxObjectInfos,
            ContextBeanDisplayPredicateProvider pp, SmxUiService smxUiService);
}

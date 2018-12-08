// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.service;

import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.ui.smx.dto.BeanInfoDto;
import com.gilecode.xmx.ui.smx.dto.VisData;

import java.util.Collection;
import java.util.List;

/**
 * Handles all accesses to {@link IXmxService} from XMX UI, wraps them with additional logic
 * and transformations required for UI.
 */
public interface ISmxUiService {

	Collection<String> getAppsAndClasses();

	String getCurrentSessionId();

	VisData getVisData(boolean showAllBeans, Integer showBeansContextId, String filter);

    List<String> getAppNames();

	List<BeanInfoDto> getBeans(String appName);
}

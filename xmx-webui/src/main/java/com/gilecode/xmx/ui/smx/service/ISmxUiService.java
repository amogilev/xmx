// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.service;

import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.spring.ResolvedValueKind;
import com.gilecode.xmx.ui.smx.dto.BeanNameDto;
import com.gilecode.xmx.ui.smx.dto.VisData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles all accesses to {@link IXmxService} from XMX UI, wraps them with additional logic
 * and transformations required for UI.
 */
public interface ISmxUiService {

	Collection<String> getAppsAndClasses();

	String getCurrentSessionId();

    List<String> getAppNames();

	List<BeanNameDto> getBeans(String appName);

	VisData getVisData(String appNameOrNull, String beanNameOrNull, String expandContextIdOrNull);

	Map<String, Set<String>> getResolvedValues(ResolvedValueKind kind, String s);
}

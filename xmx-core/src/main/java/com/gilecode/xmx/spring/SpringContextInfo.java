// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.spring;

import com.gilecode.xmx.plugin.AbstractPluginManagedObjectInfo;
import com.gilecode.xmx.plugin.IPluginsIds;
import com.gilecode.xmx.util.Pair;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpringContextInfo extends AbstractPluginManagedObjectInfo {

    private Set<Pair<String, String>> resolvedProperties = newConcurrentSet();
    private Set<Pair<String, String>> resolvedPlaceholders = newConcurrentSet();

    @Override
    public String getPluginId() {
        return IPluginsIds.PLUGIN_SPRING;
    }

    @Override
    public String getType() {
        return SpringPluginConstants.INFO_CONTEXT;
    }

    public boolean addResolvedValue(ResolvedValueKind kind, String key, String value) {
        return getResolvedValues(kind).add(Pair.of(key, value));
    }

    public Set<Pair<String, String>> getResolvedValues(ResolvedValueKind kind) {
        return kind == ResolvedValueKind.PLACEHOLDER ? resolvedPlaceholders : resolvedProperties;
    }

    private static Set<Pair<String, String>> newConcurrentSet() {
        return Collections.newSetFromMap(
                new ConcurrentHashMap<Pair<String, String>, Boolean>());
    }
}

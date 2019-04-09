// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.spring;

import com.gilecode.xmx.core.ManagedObjectWeakRef;
import com.gilecode.xmx.core.XmxManager;
import com.gilecode.xmx.plugin.IPluginsIds;
import com.gilecode.xmx.plugin.IXmxPlugin;
import com.gilecode.xmx.service.IXmxCoreService;
import com.gilecode.xmx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

public class XmxSpringManager implements IXmxPlugin, IXmxSpringService {

    private final static Logger logger = LoggerFactory.getLogger(XmxManager.class);

    private final IXmxCoreService xmxManager;

    private final ThreadLocal<SpringContextInfo> contextInRefresh = new ThreadLocal<>();

    public XmxSpringManager(IXmxCoreService xmxManager) {
        this.xmxManager = xmxManager;
    }

    @Override
    public String getPluginId() {
        return IPluginsIds.PLUGIN_SPRING;
    }

    private SpringContextInfo findOrCreateContextInfo(Object ctx) {
        ManagedObjectWeakRef ref = xmxManager.findManagedObjectRef(ctx);
        if (ref == null) {
            logger.error("Unexpected: failed to find managed ref for Spring context: {}", ctx);
            return null;
        }
        return ref.findOrAddExtraInfo(new SpringContextInfo());
    }

    @Override
    public boolean processEvent(String eventName, Object arg) {
        switch (eventName) {
        case "CtxRefreshStart":
            contextInRefresh.set(findOrCreateContextInfo(arg));
            return true;
        case "CtxRefreshEnd":
            contextInRefresh.set(null);
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean processEvent(String eventName, Object arg1, Object arg2) {
        switch (eventName) {
        case "ResolvedProperty":
            storeResolvedValueToContext(ResolvedValueKind.PROPERTY, contextInRefresh.get(), (String)arg1, (String)arg2);
            return true;
        case "ResolvedPlaceholder":
            storeResolvedValueToContext(ResolvedValueKind.PLACEHOLDER, contextInRefresh.get(), (String)arg1, (String)arg2);
            return true;
        default:
            return false;
        }
    }

    private void storeResolvedValueToContext(ResolvedValueKind kind, SpringContextInfo contextInfo,
            String key, String value) {
        if (contextInfo == null) {
            logger.error("No current Spring context found; resolved {} not stored: {} -> {}", kind, key, value);
        } else {
            boolean isNew = contextInfo.addResolvedValue(kind, key, value);
            if (isNew) {
                logger.debug("Detected resolved Spring {}: {} -> {}", kind, key, value);
            }
        }
    }

    @Override
    public Set<Pair<String, String>> getContextResolvedValues(ResolvedValueKind kind, int objectId) {
        ManagedObjectWeakRef ref = xmxManager.getManagedObjectRef(objectId);
        if (ref != null) {
            SpringContextInfo ctxInfo = (SpringContextInfo) ref.getExtraInfo(IPluginsIds.PLUGIN_SPRING,
                    SpringPluginConstants.INFO_CONTEXT);
            if (ctxInfo != null) {
                return Collections.unmodifiableSet(ctxInfo.getResolvedValues(kind));
            }
        }
        return Collections.emptySet();
    }
}

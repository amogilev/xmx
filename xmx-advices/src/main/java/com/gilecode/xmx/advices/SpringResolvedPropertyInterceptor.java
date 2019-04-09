// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.Advice;
import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.Argument;
import com.gilecode.xmx.aop.RetVal;
import com.gilecode.xmx.aop.log.IAdviceLogger;
import com.gilecode.xmx.aop.log.LoggerFactory;
import com.gilecode.xmx.boot.XmxProxy;
import org.springframework.core.env.PropertySourcesPropertyResolver;

public class SpringResolvedPropertyInterceptor {

    private static final IAdviceLogger logger = LoggerFactory.getLogger(SpringResolvedPropertyInterceptor.class);

//    private static final IXmxSpringProxyAware proxyRegistrator = XmxProxy.getSpringProxyRegistrator();

    private String lastKey, lastResolved;

    /**
     * This advice is supposed to intercept
     * {@link PropertySourcesPropertyResolver#getProperty(String, Class, boolean)}
     * to track resolved properties.
     */
    @Advice(AdviceKind.AFTER_RETURN)
    public void intercept(@Argument(0) String key, @RetVal Object resolved) {
        if (resolved != null && key != null && !resolved.equals(key)) {
            String resolvedStr = resolved.toString();
            // synchronization issues are theoretically possible but with no critical consequences, so we ignore them
            // NOTE: key "==" comparison instead of equals() is intentional here
            if (lastKey != key || !resolvedStr.equals(lastResolved)) {
                lastKey = key;
                lastResolved = resolvedStr;
                XmxProxy.fireAdviceEvent(AdvicesConstants.PLUGIN_SPRING, "ResolvedProperty", key, resolvedStr);
            }
        }
    }
}

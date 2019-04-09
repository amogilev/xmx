// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.Advice;
import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.This;
import com.gilecode.xmx.boot.XmxProxy;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * This advice is supposed to intercept Spring context refreshes
 * to track which context is active for a property being resolved.
 */
public class SpringContextRefreshInterceptor {

//    private static final IAdviceLogger logger = LoggerFactory.getLogger(SpringContextRefreshInterceptor.class);

    @Advice(AdviceKind.BEFORE)
    public void before(@This AbstractApplicationContext ctx) {
        XmxProxy.fireAdviceEvent(AdvicesConstants.PLUGIN_SPRING, "CtxRefreshStart", ctx);
    }

    @Advice(AdviceKind.AFTER_RETURN)
    public void afterRet(@This AbstractApplicationContext ctx) {
        XmxProxy.fireAdviceEvent(AdvicesConstants.PLUGIN_SPRING, "CtxRefreshEnd", ctx);
    }

    @Advice(AdviceKind.AFTER_THROW)
    public void afterThrow(@This AbstractApplicationContext ctx) {
        XmxProxy.fireAdviceEvent(AdvicesConstants.PLUGIN_SPRING, "CtxRefreshEnd", ctx);
    }
}

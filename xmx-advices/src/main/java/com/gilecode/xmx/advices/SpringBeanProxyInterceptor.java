// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.Advice;
import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.Argument;
import com.gilecode.xmx.aop.RetVal;
import com.gilecode.xmx.boot.IXmxSpringProxyAware;
import com.gilecode.xmx.boot.XmxProxy;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class SpringBeanProxyInterceptor {

    private static final IXmxSpringProxyAware proxyRegistrator = XmxProxy.getSpringProxyRegistrator();

    /**
     * This advice is supposed to intercept
     * {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean(String, Object, RootBeanDefinition)}
     * to check whether an original bean is replaced with a proxy.
     */
    @Advice(AdviceKind.AFTER_RETURN)
    public void interceptInitializeBean(@Argument(1) Object originalBean, @RetVal Object finalBean) {
        if (originalBean != finalBean) {
            // TODO: maybe verify that the bean is actually proxy
            proxyRegistrator.registerProxy(originalBean, finalBean);
        }
    }
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.aop.log.IAdviceLogger;
import com.gilecode.xmx.aop.log.LoggerFactory;
import com.gilecode.xmx.boot.IXmxSpringProxyAware;
import com.gilecode.xmx.boot.XmxProxy;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.lang.reflect.Method;

public class SpringBeanProxyInterceptor {

    private static final IAdviceLogger logger = LoggerFactory.getLogger(SpringBeanProxyInterceptor.class);

    private static final IXmxSpringProxyAware proxyRegistrator = XmxProxy.getSpringProxyRegistrator();
    private static Method mGetEarlySingleton;
    static {
        try {
            mGetEarlySingleton = DefaultSingletonBeanRegistry.class.getDeclaredMethod("getSingleton", String.class, boolean.class);
            mGetEarlySingleton.setAccessible(true);
        } catch (Exception e) {
            logger.error("XMX Error: failed to get DefaultSingletonBeanRegistry.getSingleton(String, boolean)", e);
        }
    }

    /**
     * This advice is supposed to intercept
     * {@link org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#initializeBean(String, Object, RootBeanDefinition)}
     * to check whether an original bean is replaced with a proxy.
     */
    @Advice(AdviceKind.AFTER_RETURN)
    public void interceptInitializeBean(@This AbstractAutowireCapableBeanFactory factory,
            @Argument(0) String beanName,
            @Argument(1) Object originalBean,
            @RetVal Object finalBean) {
        if (originalBean != finalBean) {
            // TODO: maybe verify that the bean is actually proxy
            proxyRegistrator.registerProxy(originalBean, finalBean);
            logger.debug("Detected Spring proxy {} for bean {}", finalBean.getClass(), beanName);
        } else if (factory.isSingletonCurrentlyInCreation(beanName) && mGetEarlySingleton != null) {
            Object earlySingletonReference = null;
            try {
                earlySingletonReference = mGetEarlySingleton.invoke(factory, beanName, false);
            } catch (Exception e) {
                logger.warn("Failed to query early singleton", e);
            }
            if (earlySingletonReference != null && originalBean != earlySingletonReference) {
                logger.debug("Detected Spring early singleton proxy {} for bean {}", earlySingletonReference.getClass(), beanName);
                proxyRegistrator.registerProxy(originalBean, earlySingletonReference);
            }
        }
    }
}

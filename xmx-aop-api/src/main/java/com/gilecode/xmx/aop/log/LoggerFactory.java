// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides {@link IAdviceLogger} instance.
 */
public class LoggerFactory {

    private static final Method mGetLogger;
    static {
        try {
            Class<?> cXmxProxy = Class.forName("com.gilecode.xmx.boot.XmxProxy");
            mGetLogger = cXmxProxy.getDeclaredMethod("getAdviceLogger", String.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to intialize advices LoggerFactory", e);
        }
    }

    /**
     * Obtains a logger instance for the specified class.
     */
    public static IAdviceLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Obtains a logger instance with the specified name.
     */
    public static IAdviceLogger getLogger(String name) {
        try {
            return (IAdviceLogger) mGetLogger.invoke(null, name);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to obtain IAdviceLogger", e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else {
                throw new IllegalStateException("Failed to obtain IAdviceLogger", e);
            }
        }
    }
}

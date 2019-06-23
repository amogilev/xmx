// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.context;

import com.gilecode.xmx.model.XmxObjectInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.gilecode.xmx.util.ReflectionUtils.*;

public class ContextDataExtractor {

    private static final String JAVA_WEB_CTX_CLASSNAME = "org.springframework.web.context.support.AnnotationConfigWebApplicationContext";
    private static final String JAVA_WEB_BOOT_CTX_CLASSNAME = "org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext";

    /**
     * Extracts the bean factory from a Spring context object, silently suppresses IllegalStateException for
     * non-refreshed or otherwise invalid contexts.
     *
     * @return the bean factory, or {@code null} if invalid or unsupported context object is passed
     */
    public Object getBeanFactory(Object ctxObj) {
        return safeFindInvokeMethodWithIgnoredExceptions(ctxObj,
                "org.springframework.context.support.AbstractApplicationContext",
                "getBeanFactory",
                Collections.singleton(IllegalStateException.class));
    }

    /**
     * Extracts all bean definition names from the given factory.
     */
    public String[] getFactoryBeanDefinitionNames(Object beanFactory) {
        return (String[]) safeFindInvokeMethod(beanFactory, "org.springframework.beans.factory.support.DefaultListableBeanFactory", "getBeanDefinitionNames");
    }

    /**
     * Extracts all bean definition names from the specified context object.
     */
    public String[] getBeanDefinitionNames(XmxObjectInfo ctxObjInfo) {
        Object beanFactory = getBeanFactory(ctxObjInfo.getValue());
        if (beanFactory == null) {
            return new String[0];
        }
        return getFactoryBeanDefinitionNames(beanFactory);
    }

    /**
     * Extracts teh parent context from a context object, if any.
     */
    public Object getParentContext(Object ctxObj) {
        ClassLoader classLoader = ctxObj.getClass().getClassLoader();
        try {
            Class<?> abstractAppContextClass = Class.forName("org.springframework.context.support.AbstractApplicationContext", false, classLoader);
            Method mGetParent = abstractAppContextClass.getDeclaredMethod("getParent");
            return mGetParent.invoke(ctxObj);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Spring is not loaded for the application", e);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Unexpected failure on invocation of AbstractApplicationContext.getParent()", e);
        }
    }

    /**
     * Extracts additional details about the context, such as the initial configuration.
     */
    public Map<String, Object> extractContextDetails(Object ctxObj, String className) {
        final Map<String, Object> details = new LinkedHashMap<>();
        if (className.endsWith("WebApplicationContext")) {
            Object configLocations = safeFindInvokeMethod(ctxObj, "org.springframework.web.context.support.AbstractRefreshableWebApplicationContext", "getConfigLocations");
            if (configLocations != null) {
                details.put("configLocations", configLocations);
            }
            if (className.equals(JAVA_WEB_CTX_CLASSNAME)) {
                details.putAll(extractAnnotationConfigDetailsFields(ctxObj, JAVA_WEB_CTX_CLASSNAME));
            }
        } else if (className.equals("org.springframework.context.support.ClassPathXmlApplicationContext")) {
            Object configLocations = safeFindInvokeMethod(ctxObj, "org.springframework.context.support.AbstractRefreshableConfigApplicationContext", "getConfigLocations");
            if (configLocations != null) {
                details.put("configLocations", configLocations);
            }
            Object configResources = safeFindInvokeMethod(ctxObj, "org.springframework.context.support.ClassPathXmlApplicationContext", "getConfigResources");
            if (configResources != null) {
                details.put("configResources", configResources);
            }
        } else if (className.equals(JAVA_WEB_BOOT_CTX_CLASSNAME)) {
            details.putAll(extractAnnotationConfigDetailsFields(ctxObj, JAVA_WEB_BOOT_CTX_CLASSNAME));
//        } else if (className.equals("org.springframework.boot.web.servlet.context.XmlServletWebServerApplicationContext")) {
//            // TODO: use advices to catch initial resources provided
//
        }
        // TODO: extract details for annotationDriven generic: AnnotationConfigApplicationContext (how? Advice or get from reader!)
        // TODO: also support GenericGroovyApplicationContext, reactibe SpringBoot contexts etc.
        return details;
    }

    private Map<String,Object> extractAnnotationConfigDetailsFields(Object ctxObj, String fieldsOwnerClass) {
        Map<String, Object> details = new TreeMap<>();
        Collection<?> annoClasses = safeFindGetField(ctxObj, fieldsOwnerClass, "annotatedClasses", Collection.class);
        if (annoClasses != null && !annoClasses.isEmpty()) {
            details.put("annotatedClasses", annoClasses);
        }
        Collection<?> basePackages = safeFindGetField(ctxObj, fieldsOwnerClass, "basePackages", Collection.class);
        if (basePackages != null && !basePackages.isEmpty()) {
            details.put("basePackages", basePackages);
        }
        return details;
    }

    private void extractAnnotationConfigDetailsFields(Map<String, Object> details, Object ctxObj, String javaWebCtxClassname) {

    }
}

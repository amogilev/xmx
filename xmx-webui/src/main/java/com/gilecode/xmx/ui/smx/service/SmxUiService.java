// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.service;

import com.gilecode.xmx.model.XmxClassInfo;
import com.gilecode.xmx.model.XmxObjectInfo;
import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.ui.service.IXmxUiService;
import com.gilecode.xmx.ui.smx.dto.BeanInfoDto;
import com.gilecode.xmx.ui.smx.dto.VisData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SmxUiService implements ISmxUiService {

    private final static Logger logger = LoggerFactory.getLogger(SmxUiService.class);

    private final IXmxUiService xmxUiService;
    private final IXmxService xmxService;

    @Autowired
    public SmxUiService(IXmxUiService xmxUiService, IXmxService xmxService) {
        this.xmxUiService = xmxUiService;
        this.xmxService = xmxService;
    }

    @Override
    public Collection<String> getAppsAndClasses() {
        return null;
    }

    @Override
    public String getCurrentSessionId() {
        return xmxUiService.getCurrentSessionId();
    }

    static class ContextInfo {
        final String id;
        final String parentId;
        final String text;
        final String contextClassName;
        final Map<String, Object> details = new LinkedHashMap<>();
        final List<BeanInfo> beans = new ArrayList<>();

        public ContextInfo(String id, String parentId, String text, String contextClassName) {
            this.id = id;
            this.parentId = parentId;
            this.text = text;
            this.contextClassName = contextClassName;
        }
    }

    static class BeanInfo {
        final String name;
        final String scope;
//        final String text;
//        final String className;

        public BeanInfo(String name, String scope) {
            this.name = name;
            this.scope = scope;
        }
    }

    @Override
    public VisData getVisData(String appNameOrNull, String beanIdOrNull) {
        VisData data = new VisData();
        if (appNameOrNull == null) {
            for (String appName2 : xmxService.getApplicationNames()) {
                fillVisData(data, appName2, beanIdOrNull);
            }
        } else {
            fillVisData(data, appNameOrNull, beanIdOrNull);
        }
        return data;
    }

    private void fillVisData(VisData data, String appName, String beanIdOrNull) {
        // TODO: maybe provide more restrictive better pattern
        List<XmxClassInfo> contextClassInfos = getContextClasses(appName);
        if (contextClassInfos.size() > 0) {
            data.addApp(appName);

            List<XmxObjectInfo> ctxObjectInfos = new LinkedList<>();
            for (XmxClassInfo classInfo : contextClassInfos) {
                ctxObjectInfos.addAll(xmxService.getManagedObjects(classInfo.getId()));
            }

            // transform all contexts to ContextInfo starting from root ones
            Collection<ContextInfo> contextInfos = transformContexts(ctxObjectInfos);
            for (ContextInfo ci : contextInfos) {
                // TODO: make multi-line label
                if (ci.parentId == null) {
                    data.addRootContext(appName, ci.id, makeContextLabel(ci), ci.text);
                } else {
                    data.addChildContext(ci.parentId, ci.id, makeContextLabel(ci), ci.text);
                }

                if (beanIdOrNull == null || beanIdOrNull.startsWith(ci.id)) {
                    for (BeanInfo bi : ci.beans) {
                        // TODO: maybe add custom title (e.g. toString(), not sure yet)
                        if (beanIdOrNull == null || beanIdOrNull.equals(makeBeanPath(ci, bi))) {
                            // FIXME: also add all "sourcepath" beans! Probably dynamic beans set is needed...
                            data.addBean(ci.id, makeBeanPath(ci, bi), makeBeanLabel(bi));
                        }
                    }
                }
            }
        }
    }

    private String makeBeanPath(ContextInfo ci, BeanInfo bi) {
        return ci.id + ".#" + bi.name;
    }

    public List<XmxClassInfo> getContextClasses(String appName) {
        return xmxService.findManagedClassInfos(appName, ".*ApplicationContext");
    }

    @Override
    public List<String> getAppNames() {
        // TODO: skip apps with no contexts
        return xmxService.getApplicationNames();
    }

    @Override
    public List<BeanInfoDto> getBeans(String appNameOrNull) {
        List<BeanInfoDto> result = new ArrayList<>();
        if (appNameOrNull == null) {
            for (String appName : getAppNames()) {
                fillBeans(appName, result);
            }
        } else {
            fillBeans(appNameOrNull, result);
        }
        Collections.sort(result, new Comparator<BeanInfoDto>() {
            @Override
            public int compare(BeanInfoDto o1, BeanInfoDto o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return result;
    }

    private void fillBeans(String appName, List<BeanInfoDto> result) {
        for (XmxClassInfo classInfo : getContextClasses(appName)) {
            for (XmxObjectInfo ctxObjInfo : xmxService.getManagedObjects(classInfo.getId())) {
                // TODO: refactor - we only need bean infos and ctx id, nothing else
                ContextInfo ci = transformContext(ctxObjInfo, null);
                for (BeanInfo bi : ci.beans) {
                    result.add(new BeanInfoDto(makeBeanPath(ci, bi), bi.name));
                }
            }
        }
    }

    private String makeBeanLabel(BeanInfo bi) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("<b>").append(bi.name).append("</b>\n\n");
        sb.append("<b>Scope:</b>").append(bi.scope).append("\n");
        // TODO role, class etc.
        return sb.toString();
    }

    private String makeContextLabel(ContextInfo ci) {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<b>").append(ci.contextClassName).append("</b>\n");
        for (Map.Entry<String, Object> e : ci.details.entrySet()) {
            sb.append("\n<b>").append(e.getKey()).append("</b>: ");

            Object value = e.getValue();
            if (value.getClass().isArray()) {
                Object[] arr = (Object[])value;
                for (Object o : arr) {
                    sb.append("\n").append(o);
                }
            } else {
                sb.append(value);
            }
        }
        return sb.toString();
    }

    private Collection<ContextInfo> transformContexts(List<XmxObjectInfo> ctxObjectInfos) {
        Map<Object, ContextInfo> contextInfosMap = new IdentityHashMap<>();
        List<ContextInfo> contextInfos = new ArrayList<>(); // ordered parent-first
        while (!ctxObjectInfos.isEmpty()) {
            boolean foundNext = false;
            Iterator<XmxObjectInfo> it = ctxObjectInfos.iterator();
            while (it.hasNext()) {
                XmxObjectInfo ctxObjInfo = it.next();
                Object ctxObj = ctxObjInfo.getValue();

                Object ctxParentObj = findParent(ctxObj);
                ContextInfo ctxParent = null;
                if (ctxParentObj == null || (ctxParent = contextInfosMap.get(ctxParentObj)) != null) {
                    foundNext = true;
                    it.remove();


                    ContextInfo ci = transformContext(ctxObjInfo, ctxParent);
                    contextInfosMap.put(ctxObj, ci);
                    contextInfos.add(ci);
                }
            }

            if (!foundNext) {
                throw new IllegalStateException("Found contexts with unknown parents, [0]=" + ctxObjectInfos.get(0).getValue());
            }
        }
        return contextInfos;
    }

    private Object findParent(Object ctxObj) {
        ClassLoader classLoader = ctxObj.getClass().getClassLoader();
        // TODO: cache Class/Method for last used CL? (weak!)
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

    private Object safeFindInvokeMethod(Object obj, String className, String methodName) {
        Method m = safeFindMethod(obj, className, methodName);
        return safeInvokeMethod(m, obj);
    }

    private Object safeInvokeMethod(Method m, Object obj, Object...args) {
        if (m == null) {
            return null;
        }
        try {
            return m.invoke(obj, args);
        } catch (Exception e) {
            logger.warn("Failed to invoke method " + m + " on obj " + obj, e);
            return null;
        }
    }

    private Method safeFindMethod(Object obj, String className, String methodName, Class<?>...parameterTypes) {
        if (obj == null) {
            return null;
        }
        Class<?> c = obj.getClass();
        while (c != null && !c.getName().equals(className)) {
            c = c.getSuperclass();
        }

        if (c == null) {
//            logger.warn("Failed to find superclass " + className + " on obj " + obj);
            return null;
        }

        try {
            return c.getDeclaredMethod(methodName, parameterTypes);
        } catch (Exception e) {
            logger.warn("Failed to find method " + className + "::" + methodName + " on obj " + obj, e);
            return null;
        }
    }

    private ContextInfo transformContext(XmxObjectInfo ctxObjInfo, ContextInfo ctxParent) {
        int objectId = ctxObjInfo.getObjectId();
        Object ctxObj = ctxObjInfo.getValue();
        String className = ctxObjInfo.getClassInfo().getClassName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

        ContextInfo ci = new ContextInfo("$" + objectId, ctxParent == null ? null : ctxParent.id, ctxObj.toString(), simpleClassName);

        if (className.endsWith("WebApplicationContext")) {
            // TODO move class names to constants?
            Object configLocations = safeFindInvokeMethod(ctxObj, "org.springframework.web.context.support.AbstractRefreshableWebApplicationContext", "getConfigLocations");
            if (configLocations != null) {
                ci.details.put("configLocations", configLocations);
            }
        }
        // TODO: extract details for ClassPathXmlApplicationContext (configResources)
        // TODO: extract details for annotationDriven (annotatedClasses + basePackages)

        Object beanFactory = safeFindInvokeMethod(ctxObj, "org.springframework.context.support.AbstractApplicationContext", "getBeanFactory");
        String[] bdNames = (String[])safeFindInvokeMethod(beanFactory, "org.springframework.beans.factory.support.DefaultListableBeanFactory", "getBeanDefinitionNames");

        Method mGetBeanDefinition = safeFindMethod(beanFactory, "org.springframework.beans.factory.support.DefaultListableBeanFactory", "getBeanDefinition", String.class);
        Method mGetSingleton = safeFindMethod(beanFactory, "org.springframework.beans.factory.support.DefaultSingletonBeanRegistry", "getSingleton", String.class);

        for (String name : bdNames) {
            Object bd = safeInvokeMethod(mGetBeanDefinition, beanFactory, name);

            String scope = (String) safeFindInvokeMethod(bd, "org.springframework.beans.factory.support.AbstractBeanDefinition", "getScope");
            if (scope == null) {
                scope = "ERROR";
            }
            boolean isSingleton = ConfigurableBeanFactory.SCOPE_SINGLETON.equals(scope) || AbstractBeanDefinition.SCOPE_DEFAULT.equals(scope);
            if (isSingleton) {
                // TODO check isAbstract?
                // TODO: do we need bean here? Why? (maybe class name?)
//                Object bean = safeInvokeMethod(mGetSingleton, beanFactory, name);
//                    safeInvokeMethod()
            }
            ci.beans.add(new BeanInfo(name, scope));
        }

        return ci;
    }
}

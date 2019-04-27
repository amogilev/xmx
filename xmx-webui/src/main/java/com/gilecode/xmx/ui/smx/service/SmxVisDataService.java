// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.service;

import com.gilecode.xmx.model.XmxObjectInfo;
import com.gilecode.xmx.ui.refpath.RefPathUtils;
import com.gilecode.xmx.ui.smx.context.ContextBeanDisplayPredicateProvider;
import com.gilecode.xmx.ui.smx.context.ContextBeansDisplayMode;
import com.gilecode.xmx.ui.smx.context.ContextBeansDisplayPredicate;
import com.gilecode.xmx.ui.smx.context.ContextDataExtractor;
import com.gilecode.xmx.ui.smx.dto.VisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

import java.lang.reflect.Method;
import java.util.*;

import static com.gilecode.xmx.util.ReflectionUtils.*;
import static org.springframework.beans.factory.config.BeanDefinition.*;

public class SmxVisDataService implements IVisDataService {

    private static final String ABSTRACT_BEAN_DEFINITION_CLASSNAME = "org.springframework.beans.factory.support.AbstractBeanDefinition";

    private final ContextDataExtractor contextDataExtractor;

    @Autowired
    public SmxVisDataService(ContextDataExtractor contextDataExtractor) {
        this.contextDataExtractor = contextDataExtractor;
    }

    private static class ContextInfo {
        final String id;
        final String parentId;
        final String text;
        final String contextClassName;
        final Map<String, Object> details;

        // either beans or beansCount is used, depending on whether the context is expanded
        List<BeanInfo> beans;
        int beansCount;

        public ContextInfo(String id, String parentId, String text, String contextClassName, Map<String, Object> details) {
            this.id = id;
            this.parentId = parentId;
            this.text = text;
            this.contextClassName = contextClassName;
            this.details = details;
        }
    }

    private static class BeanInfo {
        final String name;
        final String scope;
        final Integer role;
        final String beanClassName;
//        final String text;

        public BeanInfo(String name, String scope, Integer role, String beanClassName) {
            this.name = name;
            this.scope = scope;
            this.role = role;
            this.beanClassName = beanClassName;
        }
    }

    @Override
    public void fillVisData(VisData data, String appName, List<XmxObjectInfo> ctxObjectInfos,
            ContextBeanDisplayPredicateProvider pp, SmxUiService smxUiService) {
        if (!ctxObjectInfos.isEmpty()) {
            data.addApp(appName);

            Collection<ContextInfo> contextInfos = transformContexts(ctxObjectInfos, pp);
            for (ContextInfo ci : contextInfos) {
                if (ci.parentId == null) {
                    data.addRootContext(appName, ci.id, makeContextLabel(ci), ci.text);
                } else {
                    data.addChildContext(ci.parentId, ci.id, makeContextLabel(ci), ci.text);
                }

                if (ci.beansCount > 0) {
                    data.addBeansCluster(ci.id, ci.beansCount);
                } else if (ci.beans != null) {
                    for (BeanInfo bi : ci.beans) {
                        // TODO: maybe add custom title (e.g. toString(), not sure yet)
                        // FIXME: in future, shall also add "sourcepath" for a selected bean
                        data.addBean(ci.id, makeBeanPath(ci, bi), makeBeanLabel(bi));
                    }
                }
            }
        }
    }

    private Collection<ContextInfo> transformContexts(List<XmxObjectInfo> ctxObjectInfos, ContextBeanDisplayPredicateProvider pp) {
        Map<Object, ContextInfo> contextInfosMap = new IdentityHashMap<>();
        List<ContextInfo> contextInfos = new ArrayList<>(); // ordered parent-first
        while (!ctxObjectInfos.isEmpty()) {
            boolean foundNext = false;
            Iterator<XmxObjectInfo> it = ctxObjectInfos.iterator();
            while (it.hasNext()) {
                XmxObjectInfo ctxObjInfo = it.next();
                Object ctxObj = ctxObjInfo.getValue();

                Object ctxParentObj = contextDataExtractor.getParentContext(ctxObj);
                ContextInfo ctxParent = null;
                if (ctxParentObj == null || (ctxParent = contextInfosMap.get(ctxParentObj)) != null) {
                    foundNext = true;
                    it.remove();

                    ContextInfo ci = transformContext(ctxObjInfo, ctxParent, pp);
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

    private ContextInfo transformContext(XmxObjectInfo ctxObjInfo, ContextInfo ctxParent, ContextBeanDisplayPredicateProvider pp) {
        String contextId = toContextId(ctxObjInfo);
        Object ctxObj = ctxObjInfo.getValue();
        String className = ctxObjInfo.getClassInfo().getClassName();
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

        ContextInfo ci = new ContextInfo(contextId,
                ctxParent == null ? null : ctxParent.id,
                ctxObj.toString(),
                simpleClassName,
                contextDataExtractor.extractContextDetails(ctxObj, className));

        Object beanFactory = contextDataExtractor.getBeanFactory(ctxObjInfo);
        String[] bdNames = contextDataExtractor.getFactoryBeanDefinitionNames(beanFactory);

        ContextBeansDisplayPredicate dispPred = pp.getPredicate(contextId);

        if (dispPred.mode() == ContextBeansDisplayMode.COUNT_ONLY) {
            ci.beansCount = bdNames.length;
        } else if (dispPred.mode() != ContextBeansDisplayMode.NONE) {
            ci.beans = new ArrayList<>(bdNames.length);

            Method mGetBeanDefinition = safeFindMethod(beanFactory, "org.springframework.beans.factory.support.DefaultListableBeanFactory", "getBeanDefinition", String.class);
            Method mGetSingleton = safeFindMethod(beanFactory, "org.springframework.beans.factory.support.DefaultSingletonBeanRegistry", "getSingleton", String.class);

            ClassLoader springCL = beanFactory.getClass().getClassLoader();
            Method mGetScope = safeFindClassAndMethod(springCL, ABSTRACT_BEAN_DEFINITION_CLASSNAME, "getScope");
            Method mGetRole = safeFindClassAndMethod(springCL, ABSTRACT_BEAN_DEFINITION_CLASSNAME, "getRole");

            for (String name : bdNames) {
                if (dispPred.displayBean(name)) {
                    Object bd = safeInvokeMethod(mGetBeanDefinition, beanFactory, name);
                    if (bd != null) {
                        String scope = (String) safeInvokeMethod(mGetScope, bd);
                        if (scope == null) {
                            scope = "ERROR";
                        }

                        Integer role = (Integer) safeInvokeMethod(mGetRole, bd);
                        Object instance = null;
                        boolean isSingleton = ConfigurableBeanFactory.SCOPE_SINGLETON.equals(scope) || AbstractBeanDefinition.SCOPE_DEFAULT.equals(scope);
                        if (isSingleton) {
                            // TODO maybe use different (color) groups for instantiated singletons, not instantiated singletons, abstract beans and prototypes
                            instance = safeInvokeMethod(mGetSingleton, beanFactory, name);
                        }

                        String beanClassName = (String) safeFindInvokeMethod(bd, ABSTRACT_BEAN_DEFINITION_CLASSNAME, "getBeanClassName");
                        String actualClassName = instance == null ? null : instance.getClass().getName();

//                        if (beanClassName == null && actualClassName == null) {
//                            // TODO: lazy-init singletons; maybe print factoryBeanName + factoryMethodName instead
//                        }
                        ci.beans.add(new BeanInfo(name, scope, role, mergeClassNames(beanClassName, actualClassName)));
                    }
                }
            }
        }

        return ci;
    }

    private String mergeClassNames(String beanClassName, String actualClassName) {
        if (beanClassName == null) {
            return actualClassName;
        } else if (actualClassName == null || actualClassName.equals(beanClassName)) {
            return beanClassName;
        } else {
            return beanClassName + " (" + actualClassName + ")";
        }
    }

    private static String makeBeanPath(ContextInfo ci, BeanInfo bi) {
        return makeBeanPath(ci.id, bi.name);
    }

    private static String makeBeanPath(String ctxId, String beanName) {
        return ctxId + "." + RefPathUtils.encodeBeanNamePathPart(beanName);
    }

    private static String makeBeanLabel(BeanInfo bi) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<b>").append(bi.name).append("</b>\n\n");

        sb.append("<b>Class:</b>").append(bi.beanClassName).append("\n");
        if (bi.scope.equalsIgnoreCase(ConfigurableBeanFactory.SCOPE_PROTOTYPE)) {
            sb.append("<b>Scope:</b>").append(bi.scope).append("\n");
        }
        if (bi.role != null) {
            sb.append("<b>Role:</b>").append(getRoleName(bi.role)).append("\n");
        }
        return sb.toString();
    }

    private static String getRoleName(int role) {
        switch (role) {
        case ROLE_APPLICATION:
            return "application";
        case ROLE_INFRASTRUCTURE:
            return "infrastructure";
        case ROLE_SUPPORT:
            return "support";
        default:
            return Integer.toString(role);
        }
    }

    private static String makeContextLabel(ContextInfo ci) {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<b>").append(ci.contextClassName).append("</b>\n");
        for (Map.Entry<String, Object> e : ci.details.entrySet()) {

            Collection<Object> coll = toCollection(e.getValue());
            if (!coll.isEmpty()) {
                sb.append("\n<b>").append(e.getKey()).append("</b>: ");
                for (Object o : coll) {
                    sb.append("\n").append(o);
                }
            }
        }
        return sb.toString();
    }

    private static String toContextId(XmxObjectInfo ctxObjInfo) {
        return "$" + ctxObjInfo.getObjectId();
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> toCollection(Object value) {
        if (value == null) {
            return Collections.emptyList();
        } if (value.getClass().isArray()) {
            Object[] arr = (Object[])value;
            return Arrays.asList(arr);
        } else if (value instanceof Collection) {
            return (Collection<Object>)value;
        } else {
            return Collections.singletonList(value);
        }
    }
}

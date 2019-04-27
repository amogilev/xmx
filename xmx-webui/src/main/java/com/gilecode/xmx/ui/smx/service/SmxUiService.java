// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.service;

import com.gilecode.xmx.model.XmxClassInfo;
import com.gilecode.xmx.model.XmxObjectInfo;
import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.spring.IXmxSpringService;
import com.gilecode.xmx.spring.ResolvedValueKind;
import com.gilecode.xmx.ui.service.IXmxUiService;
import com.gilecode.xmx.ui.smx.context.ContextBeanDisplayPredicateProvider;
import com.gilecode.xmx.ui.smx.context.ContextDataExtractor;
import com.gilecode.xmx.ui.smx.dto.BeanNameDto;
import com.gilecode.xmx.ui.smx.dto.VisData;
import com.gilecode.xmx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class SmxUiService implements ISmxUiService {

    private final static Logger logger = LoggerFactory.getLogger(SmxUiService.class);

    private final IXmxUiService xmxUiService;
    private final IXmxService xmxService;
    private final IXmxSpringService xmxSpringService;
    private final IVisDataService visDataService;
    private final ContextDataExtractor contextDataExtractor;

    @Autowired
    public SmxUiService(IXmxUiService xmxUiService, IXmxService xmxService, IXmxSpringService xmxSpringService,
            IVisDataService visDataService, ContextDataExtractor contextDataExtractor) {
        this.xmxUiService = xmxUiService;
        this.xmxService = xmxService;
        this.xmxSpringService = xmxSpringService;
        this.visDataService = visDataService;
        this.contextDataExtractor = contextDataExtractor;
    }

    @Override
    public Collection<String> getAppsAndClasses() {
        return null;
    }

    @Override
    public String getCurrentSessionId() {
        return xmxUiService.getCurrentSessionId();
    }

    @Override
    public VisData getVisData(String appNameOrNull, String beanNameOrNull, String expandContextIdOrNull) {
        ContextBeanDisplayPredicateProvider pp = new ContextBeanDisplayPredicateProvider(beanNameOrNull, expandContextIdOrNull);
        Collection<String> appNames = appNameOrNull == null ?
                xmxService.getApplicationNames() :
                Collections.singleton(appNameOrNull);

        VisData data = new VisData();
        for (String appName : appNames) {
            visDataService.fillVisData(data, appName, getContextObjects(appName), pp, this);
        }
        return data;
    }

    @Override
    public Map<String, Set<String>> getResolvedValues(ResolvedValueKind kind, String appName) {
        Map<String, Set<String>> result = new TreeMap<>();

        List<XmxObjectInfo> ctxObjectInfos = getContextObjects(appName);
        for (XmxObjectInfo ctxObjectInfo : ctxObjectInfos) {
            Set<Pair<String, String>> valuesForCtx = xmxSpringService.getContextResolvedValues(kind,
                    ctxObjectInfo.getObjectId());
            for (Pair<String, String> resolvedPair : valuesForCtx) {
                String key = resolvedPair.getFirst();
                String value = resolvedPair.getSecond();
                Set<String> values = result.get(key);
                if (values == null) {
                    values = new TreeSet<>();
                    result.put(key, values);
                }
                values.add(value);
            }
        }

        return result;
    }

    @Override
    public List<String> getAppNames() {
        List<String> allApps = xmxService.getApplicationNames();
        List<String> appsWithContexts = new ArrayList<>(allApps.size());
        for (String appName : allApps) {
            if (hasAnyContext(appName)) {
                appsWithContexts.add(appName);
            }
        }
        return appsWithContexts;
    }

    @Override
    public List<BeanNameDto> getBeans(String appNameOrNull) {
        Map<String, Integer> beanNamesAndCounts = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });
        if (appNameOrNull == null) {
            for (String appName : getAppNames()) {
                fillBeans(appName, beanNamesAndCounts);
            }
        } else {
            fillBeans(appNameOrNull, beanNamesAndCounts);
        }

        List<BeanNameDto> result = new ArrayList<>(beanNamesAndCounts.size());
        for (Map.Entry<String, Integer> nameAndCount : beanNamesAndCounts.entrySet()) {
            result.add(new BeanNameDto(nameAndCount.getKey(), nameAndCount.getValue()));
        }
        return result;
    }

    public List<XmxClassInfo> getContextClasses(String appName) {
        return xmxService.findManagedClassInfos(appName, "org.springframework.*ApplicationContext");
    }

    private boolean hasAnyContext(String appName) {
        for (XmxClassInfo classInfo : getContextClasses(appName)) {
            if (!xmxService.getManagedObjects(classInfo.getId()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private List<XmxObjectInfo> getContextObjects(String appNameOrNull) {
        List<XmxClassInfo> contextClassInfos = getContextClasses(appNameOrNull);
        if (contextClassInfos.isEmpty()) {
            return Collections.emptyList();
        }
        List<XmxObjectInfo> ctxObjectInfos = new LinkedList<>();
        for (XmxClassInfo classInfo : contextClassInfos) {
            ctxObjectInfos.addAll(xmxService.getManagedObjects(classInfo.getId()));
        }
        return ctxObjectInfos;
    }

    private void fillBeans(String appName, Map<String, Integer> beanNamesAndCounts) {
        for (XmxClassInfo classInfo : getContextClasses(appName)) {
            for (XmxObjectInfo ctxObjInfo : xmxService.getManagedObjects(classInfo.getId())) {
                String[] bdNames = contextDataExtractor.getBeanDefinitionNames(ctxObjInfo);
                for (String beanName : bdNames) {
                    Integer count = beanNamesAndCounts.get(beanName);
                    beanNamesAndCounts.put(beanName, count == null ? 1 : count + 1);
                }
            }
        }
    }
}

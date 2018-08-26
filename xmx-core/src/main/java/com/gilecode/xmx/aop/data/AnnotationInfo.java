// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * A general annotation information, which can be used instead of the actual annotation instances
 * when they are not available (i.e. instead of proxy creation).
 * <p/>
 * This class does not cover some complicated cases (like annotations for values inside annotation),
 * but they are not used for XMX annotations.
 */
public class AnnotationInfo {

    private static final String PROP_VALUE = "value";

    private final Class<? extends Annotation> annotationClass;
    private final Map<String, Object> properties = new HashMap<>(4);

    public AnnotationInfo(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void addProperty(String name, Object value) {
        properties.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T)properties.get(name);
    }

    public <T> T value() {
        return get(PROP_VALUE);
    }

    public boolean isFlagSet(String name) {
        if (properties.containsKey(name)) {
            return get(name);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(annotationClass).append('(');
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            sb.append(prop.getKey()).append('=').append(prop.getValue()).append(',');
        }
        if (!properties.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(')');
        return sb.toString();
    }
}

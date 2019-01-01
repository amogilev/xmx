// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.model;

import com.gilecode.xmx.util.StringUtils;

import java.util.Objects;

/**
 * Represents a "permanent" ID of a singleton object, which consists of the application and class names;
 */
public class SingletonPermanentId {

    private final String appName;
    private final String className;

    public SingletonPermanentId(String appName, String className) {
        assert appName != null && className != null;
        this.appName = appName;
        this.className = className;
    }

    public String getAppName() {
        return appName;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SingletonPermanentId that = (SingletonPermanentId) o;
        return Objects.equals(appName, that.appName) &&
                Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, className);
    }

    /**
     * Returns the string representation of the singleton ID.
     */
    @Override
    public String toString() {
        return StringUtils.quote(getAppName()) + ":" + getClassName();
    }
}

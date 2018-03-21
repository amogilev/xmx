// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.ITypeMatcher;

public class TypeMatcher implements ITypeMatcher {

    private final boolean fullyQualified;
    private final String name;
    private final int arrayLevel;

    public TypeMatcher(boolean fullyQualified, String name, int arrayLevel) {
        this.fullyQualified = fullyQualified;
        this.name = name;
        this.arrayLevel = arrayLevel;
    }

    @Override
    public boolean matches(Class<?> type) {
        for (int i = arrayLevel; i > 0; i--) {
            if (type.isArray()) {
                type = type.getComponentType();
            } else {
                return false;
            }
        }

        String typeName = fullyQualified ? type.getName() : type.getSimpleName();
        if (!name.equals(typeName)) {
            return false;
        }
        return true;
    }
}

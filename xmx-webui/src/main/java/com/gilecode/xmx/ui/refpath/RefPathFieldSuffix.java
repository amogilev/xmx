// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

public class RefPathFieldSuffix implements RefPathSuffix {

    private final String fieldName;

    public RefPathFieldSuffix(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return getFieldName();
    }
}

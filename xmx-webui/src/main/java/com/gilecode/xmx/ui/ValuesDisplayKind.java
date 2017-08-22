// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

/**
 * Enumerates display kinds for the values of objects and fields: JSON, toString() value and "smart".
 *
 * @author Andrey Mogilev
 */
public enum ValuesDisplayKind {
    JSON("JSON"), TO_STRING("toString()"), SMART("Smart");

    private String displayName;

    ValuesDisplayKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

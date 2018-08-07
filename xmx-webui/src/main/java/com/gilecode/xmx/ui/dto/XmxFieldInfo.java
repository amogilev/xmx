// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import java.lang.reflect.Modifier;

/**
 * Information about a single field.
 */
public class XmxFieldInfo {

    /**
     * Field ID, unique within the managed object.
     */
    private final String id;

    /**
     * Field name.
     */
    private final String name;

    /**
     * Field modifiers
     */
    private final int modifiers;

    /**
     * The text representation of the field value.
     */
    private final XmxObjectTextRepresentation text;

    public XmxFieldInfo(String fid, String name, int modifiers, XmxObjectTextRepresentation text) {
        super();
        this.id = fid;
        this.name = name;
        this.modifiers = modifiers;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public XmxObjectTextRepresentation getText() {
        return text;
    }

    public boolean isStaticField() {
        return (modifiers & Modifier.STATIC) != 0;
    }
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import java.lang.reflect.Modifier;

/**
 * Information about a single method.
 */
public class XmxMethodInfo {

    /**
     * Method ID, unique within the managed object.
     */
    private final String id;

    /**
     * Simple method name. Several methods with same name may exist.
     */
    private final String name;

    /**
     * Method signature, return type and name. Really is a part of signature before ()
     */
    private final String nameTypeSignature;

    private final String[] parameters;

    /**
     * Method modifiers
     */
    private final int modifiers;

    public XmxMethodInfo(String id, String name, String nameTypeSignature, String[] parameters,
                         int modifiers) {
        super();
        this.id = id;
        this.name = name;
        this.nameTypeSignature = nameTypeSignature;
        this.parameters = parameters;
        this.modifiers = modifiers;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameTypeSignature() {
        return nameTypeSignature;
    }

    public String[] getParameters() {
        return parameters;
    }

    public boolean isStaticMethod() {
        return (modifiers & Modifier.STATIC) != 0;
    }

    public String getMethodDesc() {
        StringBuilder sb = new StringBuilder(nameTypeSignature);
        sb.append('(');
        for (String param : parameters) {
            sb.append(param).append(',');
        }
        if (parameters.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(')');
        return sb.toString();
    }
}

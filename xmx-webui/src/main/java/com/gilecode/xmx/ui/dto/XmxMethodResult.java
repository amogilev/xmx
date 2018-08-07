// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

/**
 * Information about the invoked method and its result.
 */
public class XmxMethodResult {

    private final String className;
    private final XmxMethodInfo methodInfo;

    // either result or exception is set
    private XmxObjectTextRepresentation result;
    private Throwable exception;

    public XmxMethodResult(String className, XmxMethodInfo methodInfo) {
        this.className = className;
        this.methodInfo = methodInfo;
    }

    public String getClassName() {
        return className;
    }

    public XmxMethodInfo getMethodInfo() {
        return methodInfo;
    }

    public XmxObjectTextRepresentation getResult() {
        return result;
    }

    public void setResult(XmxObjectTextRepresentation result) {
        this.result = result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}

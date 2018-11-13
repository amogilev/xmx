// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

/**
 * An exception thrown when an object is found by ID but does not have expected proxy object detected.
 */
public class MissingProxyException extends Exception {

    /**
     * ID of the object with expected but missing proxy.
     */
    private final int objectId;

    public MissingProxyException(int objectId) {
        this.objectId = objectId;
    }

    public int getObjectId() {
        return objectId;
    }
}

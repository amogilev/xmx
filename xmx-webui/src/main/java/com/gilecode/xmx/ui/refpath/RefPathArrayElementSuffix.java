// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

public class RefPathArrayElementSuffix implements RefPathSuffix {

    private final int elementIndex;

    public RefPathArrayElementSuffix(int elementIndex) {
        this.elementIndex = elementIndex;
    }

    public int getElementIndex() {
        return elementIndex;
    }

    @Override
    public String toString() {
        return Integer.toString(getElementIndex());
    }
}

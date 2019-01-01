// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

import com.gilecode.xmx.ui.UIConstants;

public class RefPathIdRoot implements RefPathRoot {

    private final int objectId;

    public RefPathIdRoot(int objectId) {
        this.objectId = objectId;
    }

    public int getObjectId() {
        return objectId;
    }

    @Override
    public String toString() {
        return UIConstants.REFPATH_PREFIX + getObjectId();
    }
}

// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

import com.gilecode.xmx.ui.UIConstants;
import com.gilecode.xmx.util.StringUtils;

public class RefPathSingletonRoot implements RefPathRoot {

    private final String appName;
    private final String className;

    public RefPathSingletonRoot(String appName, String className) {
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
    public String toString() {
        return UIConstants.PERMA_PATH_PREFIX + StringUtils.quote(getAppName()) + ":" + getClassName() + ":";
    }
}

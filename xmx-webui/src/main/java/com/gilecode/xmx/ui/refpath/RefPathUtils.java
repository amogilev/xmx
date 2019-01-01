// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

import com.gilecode.xmx.util.StringUtils;

public class RefPathUtils {

    public static String encodeBeanNamePathPart(String beanName) {
        return "#" + StringUtils.quote(beanName);
    }

    public static String encodeBeanDefinitionNamePathPart(String beanName) {
        return "#" + encodeBeanNamePathPart(beanName);
    }

}

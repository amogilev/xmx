// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

/**
 * A suffix to factory object which directs either to the bean definition or to the bean instance,
 * by the bean name.
 */
public class RefPathBeanSuffix implements RefPathSuffix {

    private final String beanName;
    private final boolean useDefinition;

    public RefPathBeanSuffix(String beanName, boolean useDefinition) {
        this.beanName = beanName;
        this.useDefinition = useDefinition;
    }

    public String getBeanName() {
        return beanName;
    }

    public boolean isUseDefinition() {
        return useDefinition;
    }

    @Override
    public String toString() {
        return useDefinition ?
                RefPathUtils.encodeBeanDefinitionNamePathPart(getBeanName()) :
                RefPathUtils.encodeBeanNamePathPart(getBeanName());
    }
}

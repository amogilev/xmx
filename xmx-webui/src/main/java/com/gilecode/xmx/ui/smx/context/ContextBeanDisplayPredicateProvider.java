// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.context;

public class ContextBeanDisplayPredicateProvider {
    final String beanNameOrNull;
    final String expandContextOrNull;

    public ContextBeanDisplayPredicateProvider(String beanNameOrNull, String expandContextOrNull) {
        this.beanNameOrNull = beanNameOrNull;
        this.expandContextOrNull = expandContextOrNull;
    }

    public ContextBeansDisplayPredicate getPredicate(final String contextId) {
        if (beanNameOrNull != null) {
            return new ContextBeansDisplayPredicate() {
                @Override
                public ContextBeansDisplayMode mode() {
                    return ContextBeansDisplayMode.SELECTED;
                }

                @Override
                public boolean displayBean(String beanName) {
                    return beanNameOrNull.equals(beanName);
                }
            };
        } else if (contextId.equals(expandContextOrNull)) {
            return ContextBeansDisplayPredicate.ALL;
        } else {
            return ContextBeansDisplayPredicate.COUNT_ONLY;
        }
    }
}

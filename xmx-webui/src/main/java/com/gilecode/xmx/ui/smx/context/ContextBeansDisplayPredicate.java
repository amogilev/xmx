// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.smx.context;

public interface ContextBeansDisplayPredicate {
    ContextBeansDisplayMode mode();
    boolean displayBean(String beanName);

    ContextBeansDisplayPredicate COUNT_ONLY = new ContextBeansDisplayPredicate() {
        @Override
        public ContextBeansDisplayMode mode() {
            return ContextBeansDisplayMode.COUNT_ONLY;
        }

        @Override
        public boolean displayBean(String beanName) {
            return false;
        }
    };

    ContextBeansDisplayPredicate NONE = new ContextBeansDisplayPredicate() {
        @Override
        public ContextBeansDisplayMode mode() {
            return ContextBeansDisplayMode.NONE;
        }

        @Override
        public boolean displayBean(String beanName) {
            return false;
        }
    };

    ContextBeansDisplayPredicate ALL = new ContextBeansDisplayPredicate() {
        @Override
        public ContextBeansDisplayMode mode() {
            return ContextBeansDisplayMode.ALL;
        }

        @Override
        public boolean displayBean(String beanName) {
            return true;
        }
    };
}

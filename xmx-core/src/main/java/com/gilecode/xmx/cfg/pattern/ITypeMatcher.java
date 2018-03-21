// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

/**
 * Pattern-based matcher for types.
 */
public interface ITypeMatcher {

    boolean matches(Class<?> type);

    ITypeMatcher ANY_TYPE = new ITypeMatcher() {
        @Override
        public boolean matches(Class<?> type) {
            return true;
        }
    };
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

/**
 * Pattern-based matcher for methods.
 */
public interface IMethodMatcher {

    boolean matches(MethodSpec spec);

    IMethodMatcher ANY_METHOD = new IMethodMatcher() {
        @Override
        public boolean matches(MethodSpec spec) {
            return true;
        }
    };

}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

import java.lang.reflect.Method;

/**
 * Pattern-based matcher for methods.
 */
public interface IMethodMatcher {

    boolean matches(Method m);

    IMethodMatcher ANY_METHOD = new IMethodMatcher() {
        @Override
        public boolean matches(Method m) {
            return true;
        }
    };

}

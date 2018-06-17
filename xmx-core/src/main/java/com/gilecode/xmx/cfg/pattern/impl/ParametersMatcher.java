// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.IMethodMatcher;
import com.gilecode.xmx.cfg.pattern.ITypeMatcher;
import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.cfg.pattern.TypeSpec;

import java.util.List;

/**
 * The method matcher which checks only the parameters (types and count)
 */
public class ParametersMatcher implements IMethodMatcher {

    /**
     * Type matchers for each of required parameters
     */
    private List<ITypeMatcher> requiredTypesMatchers;

    /**
     * Whether additional parameters are allowed
     */
    private boolean allowExtraParams;

    public ParametersMatcher(List<ITypeMatcher> requiredTypesMatchers, boolean allowExtraParams) {
        this.requiredTypesMatchers = requiredTypesMatchers;
        this.allowExtraParams = allowExtraParams;
    }

    @Override
    public boolean matches(MethodSpec m) {
        TypeSpec[] parameterTypes = m.getParameterTypes();
        if (requiredTypesMatchers.size() > parameterTypes.length) {
            return false;
        } else if (!allowExtraParams && requiredTypesMatchers.size() != parameterTypes.length) {
            return false;
        }

        for (int i = 0; i < requiredTypesMatchers.size(); i++) {
            if (!requiredTypesMatchers.get(i).matches(parameterTypes[i])) {
                return false;
            }
        }

        return true;
    }
}

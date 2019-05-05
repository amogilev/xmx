// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.IMethodMatcher;
import com.gilecode.xmx.cfg.pattern.ITypeMatcher;
import com.gilecode.xmx.cfg.pattern.MethodSpec;

import java.util.regex.Pattern;

/**
 * Matches actual methods with the method patterns.
 */
public class MethodPatternMatcher implements IMethodMatcher {

    private final ModifierFlags modifierFlags;
    private final Pattern namePattern;
    private final ITypeMatcher returnTypeMatcher;
    private final IMethodMatcher parametersMatcher;

    public MethodPatternMatcher(ModifierFlags modifierFlags, Pattern namePattern, ITypeMatcher returnTypeMatcher, IMethodMatcher parametersMatcher) {
        this.modifierFlags = modifierFlags;
        this.namePattern = namePattern;
        this.returnTypeMatcher = returnTypeMatcher;
        this.parametersMatcher = parametersMatcher;
    }

    @Override
    public boolean matches(MethodSpec m) {
        if (!modifierFlags.matches(m.getModifiers())) {
            return false;
        }

        if (namePattern != null && !matchesName(m)) {
            return false;
        }

        if (returnTypeMatcher != null && !returnTypeMatcher.matches(m.getReturnType())) {
            return false;
        }

        if (parametersMatcher != null && !parametersMatcher.matches(m)) {
            return false;
        }

        return true;
    }

    private boolean matchesName(MethodSpec m) {
        if (m.isSpecial()) {
            if (namePattern.pattern().contains(".*")) {
                // require explicit name for special methods (constructors)
                return false;
            } else if (namePattern.matcher(m.getSpecialName()).matches()) {
                return true;
            }
        }
        return namePattern.matcher(m.getName()).matches();
    }
}

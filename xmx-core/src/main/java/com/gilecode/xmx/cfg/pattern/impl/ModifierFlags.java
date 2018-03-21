// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

/**
 * Collection of method modifier flags which describe which modifiers are allowed, required or prohibited.
 */
class ModifierFlags {

    /** All required flags shall present */
    int requiredModifiers;

    /** None of prohibited flags shall present */
    int prohibitedModifiers;

    /** At least one of alternate flags shall present */
    int alternateModifiers;

    public boolean matches(int modifiers) {
        // all required modifiers shall present
        if (requiredModifiers != 0 && (modifiers & requiredModifiers) != requiredModifiers) {
            return false;
        }
        // at least one of alternative modifiers (e.g. public or protected) shall present
        if (alternateModifiers != 0 && (modifiers & alternateModifiers) == 0) {
            return false;
        }
        // none of the prohibited modifiers shall present
        if (prohibitedModifiers != 0 && (modifiers & prohibitedModifiers) != 0) {
            return false;
        }

        return true;
    }
}

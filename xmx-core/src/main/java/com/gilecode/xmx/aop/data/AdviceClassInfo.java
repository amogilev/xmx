// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import com.gilecode.xmx.aop.impl.WeakCachedSupplier;

import java.util.List;

/**
 * Result of (pre-)loading and verification of a single advice class
 */
public class AdviceClassInfo {

    private final WeakCachedSupplier<Class<?>> classSupplier;
    private final List<MethodDeclarationInfo> adviceMethods;

    // used for debugging aims, not really needed
    private final String classDesc;

    public AdviceClassInfo(WeakCachedSupplier<Class<?>> classSupplier, List<MethodDeclarationInfo> adviceMethods,
            String classDesc) {
        this.classSupplier = classSupplier;
        this.adviceMethods = adviceMethods;
        this.classDesc = classDesc;
    }

    public WeakCachedSupplier<Class<?>> getClassSupplier() {
        return classSupplier;
    }

    public List<MethodDeclarationInfo> getAdviceMethods() {
        return adviceMethods;
    }

    public String getClassDesc() {
        return classDesc;
    }

    @Override
    public String toString() {
        return classDesc;
    }
}

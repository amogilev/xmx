// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import com.gilecode.xmx.aop.impl.WeakCachedSupplier;

import java.util.List;

/**
 * Result of (pre-)loading and verification of a single advice class
 */
public class AdviceClassInfo {

    private WeakCachedSupplier<Class<?>> classSupplier;
    private List<MethodDeclarationInfo> adviceMethods;

    public AdviceClassInfo(WeakCachedSupplier<Class<?>> classSupplier, List<MethodDeclarationInfo> adviceMethods) {
        this.classSupplier = classSupplier;
        this.adviceMethods = adviceMethods;
    }

    public WeakCachedSupplier<Class<?>> getClassSupplier() {
        return classSupplier;
    }

    public void setClassSupplier(WeakCachedSupplier<Class<?>> classSupplier) {
        this.classSupplier = classSupplier;
    }

    public List<MethodDeclarationInfo> getAdviceMethods() {
        return adviceMethods;
    }

    public void setAdviceMethods(List<MethodDeclarationInfo> adviceMethods) {
        this.adviceMethods = adviceMethods;
    }
}

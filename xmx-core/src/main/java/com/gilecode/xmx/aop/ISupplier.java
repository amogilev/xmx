// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

/**
 * Supplier of some result. Same as java.util.function.Supplier in Java 8.
 */
public interface ISupplier<T> {
	T get() throws BadAdviceException;
}

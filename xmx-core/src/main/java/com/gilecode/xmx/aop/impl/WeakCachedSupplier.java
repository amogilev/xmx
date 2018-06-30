// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.BadAdviceException;
import com.gilecode.xmx.aop.ISupplier;

import java.lang.ref.WeakReference;

/**
 * An abstract supplier which caches the value in weak reference and is able to (re-)load it when
 * the weak reference is GC'ed.
 */
public abstract class WeakCachedSupplier<T> implements ISupplier<T> {

	private WeakReference<T> reference;

	/**
	 * Creates the supplier with no cached value.
	 */
	protected WeakCachedSupplier() {}

	/**
	 * Creates the supplier with the initial pre-cached value.
	 */
	protected WeakCachedSupplier(WeakReference<T> reference) {
		this.reference = reference;
	}

	/**
	 * Load or re-load the value/
	 */
	protected abstract T load() throws BadAdviceException;

	@Override
	public T get() throws BadAdviceException {
		T value = null;
		if (reference != null) {
			value = reference.get();
		}
		if (value == null) {
			value = load();
			if (value != null) {
				reference = new WeakReference<>(value);
			}
		}
		return value;
	}

	public T getSilently() {
		try {
			return get();
		} catch (BadAdviceException e) {
			return null;
		}
	}
}

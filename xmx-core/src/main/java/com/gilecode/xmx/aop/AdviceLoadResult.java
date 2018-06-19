// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import com.gilecode.xmx.aop.impl.WeakCachedSupplier;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class AdviceLoadResult {

	/**
	 * Loaded advice classes by "jar:class" advice description.
	 *
	 */
	private final Map<String, WeakCachedSupplier<Class<?>>> adviceClassesByDesc;

	/**
	 * References to all class loaders for the advice classes, used to prevent
	 * too early GC (cache dispose) of classes stored in {@link WeakCachedSupplier}s.
	 */
	private final Collection<ClassLoader> adviceLoaders;

	public AdviceLoadResult(Map<String, WeakCachedSupplier<Class<?>>> adviceClassesByDesc, Collection<ClassLoader> adviceLoaders) {
		this.adviceClassesByDesc = adviceClassesByDesc;
		this.adviceLoaders = adviceLoaders;
	}

	public static AdviceLoadResult empty() {
		return new AdviceLoadResult(Collections.<String, WeakCachedSupplier<Class<?>>>emptyMap(),
				Collections.<ClassLoader>emptyList());
	}

	public Map<String, WeakCachedSupplier<Class<?>>> getAdviceClassesByDesc() {
		return adviceClassesByDesc;
	}

	public Collection<ClassLoader> getAdviceLoaders() {
		return adviceLoaders;
	}

	public boolean isEmpty() {
		return adviceClassesByDesc.isEmpty();
	}
}

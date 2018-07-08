// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.params;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A compact cache for parameter names of methods of classes loaded by a single class loader.
 * <p/>
 * All methods are marked synchronized, as concurrent maps requires more memory, and no real concurrency
 * expected (although it is possible).
 */
public class ParamNamesCache {

	private static final String[] MISSING_CLASS_INFO = {};

	private final Map<Key, String[]> cache = new HashMap<>(64);

	private static class Key {
		String className;
		String methodName;
		String desc;

		public Key(String className, String methodName, String desc) {
			this.className = className;
			this.methodName = methodName;
			this.desc = desc;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Key key = (Key) o;
			return Objects.equals(className, key.className) &&
					Objects.equals(methodName, key.methodName) &&
					Objects.equals(desc, key.desc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(className, methodName, desc);
		}
	}

	synchronized
	public void store(String className, String methodName, String methodDesc, String[] argNames) {
		cache.put(key(className, methodName, methodDesc), argNames);
	}

	private Key key(String className, String methodName, String methodDesc) {
		// class and method names are usually taken from existing (Reflection) objects, but method descriptors are
		// created for each method by us. So it is better to intern() them (in order to reduce extra memory usage).
		if (methodDesc != null) {
			methodDesc = methodDesc.intern();
		}
		return new Key(className,methodName, methodDesc);
	}

	synchronized
	public void storeMissingClassInfo(String className) {
		cache.put(key(className, null, null), MISSING_CLASS_INFO);
	}

	synchronized
	public boolean isClassWithMissingInfo(String className) {
		return cache.get(key(className, null, null)) == MISSING_CLASS_INFO;
	}

	synchronized
	public String[] getParameterNames(String className, String methodName, String methodDesc) {
		return cache.get(key(className, methodName, methodDesc));
	}

	public String[] getParameterNames(Method m) {
		return getParameterNames(m.getDeclaringClass().getName(), m.getName(), Type.getMethodDescriptor(m));
	}
}

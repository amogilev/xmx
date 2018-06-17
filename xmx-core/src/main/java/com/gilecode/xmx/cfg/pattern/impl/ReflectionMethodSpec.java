// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.cfg.pattern.TypeSpec;

import java.lang.reflect.Method;

/**
 * A method specified by the {@link Method} object.
 */
public class ReflectionMethodSpec extends MethodSpec {

	private final Method m;

	public ReflectionMethodSpec(Method m) {
		this.m = m;
	}

	@Override
	public String getName() {
		return m.getName();
	}

	@Override
	public int getModifiers() {
		return m.getModifiers();
	}

	@Override
	public TypeSpec[] getParameterTypes() {
		Class<?>[] types = m.getParameterTypes();
		TypeSpec[] result = new TypeSpec[types.length];
		for (int i = 0; i < types.length; i++) {
			result[i] = TypeSpec.of(types[i]);
		}
		return result;
	}

	@Override
	public TypeSpec getReturnType() {
		return TypeSpec.of(m.getReturnType());
	}
}

// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.cfg.pattern.TypeSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A method specified by the {@link Method} object.
 */
public class ReflectionConstructorSpec extends MethodSpec {

	private final Constructor c;

	public ReflectionConstructorSpec(Constructor c) {
		this.c = c;
	}

	@Override
	public String getName() {
		return c.getDeclaringClass().getSimpleName();
	}

	@Override
	public int getModifiers() {
		return c.getModifiers();
	}

	@Override
	public TypeSpec[] getParameterTypes() {
		return TypeSpec.of(c.getParameterTypes());
	}

	@Override
	public TypeSpec getReturnType() {
		return null;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public String getSpecialName() {
		return "<init>";
	}
}

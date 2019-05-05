// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

import com.gilecode.xmx.cfg.pattern.impl.DescriptorMethodSpec;
import com.gilecode.xmx.cfg.pattern.impl.DescriptorSpecialMethodSpec;
import com.gilecode.xmx.cfg.pattern.impl.ReflectionConstructorSpec;
import com.gilecode.xmx.cfg.pattern.impl.ReflectionMethodSpec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Provides information about a method required for matching method patterns.
 */
public abstract class MethodSpec {

	public static MethodSpec of(Method m) {
		return new ReflectionMethodSpec(m);
	}

	public static MethodSpec of(Constructor c) {
		return new ReflectionConstructorSpec(c);
	}

	public static MethodSpec of(int modifiers, String name, String descriptor) {
		return new DescriptorMethodSpec(modifiers, name, descriptor);
	}

	public static MethodSpec special(int modifiers, String name, String descriptor, String specialName) {
		return new DescriptorSpecialMethodSpec(modifiers, name, descriptor, specialName);
	}

	public abstract String getName();

	public abstract int getModifiers();

	public abstract TypeSpec[] getParameterTypes();

	public abstract TypeSpec getReturnType();

	public boolean isSpecial() {
		return false;
	}

	public String getSpecialName() {
		return null;
	}
}

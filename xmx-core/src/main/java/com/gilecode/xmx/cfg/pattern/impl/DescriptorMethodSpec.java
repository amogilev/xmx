// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.cfg.pattern.TypeSpec;
import org.objectweb.asm.Type;

/**
 * A method specified by the method name, modifiers flags and the descriptor.
 */
public class DescriptorMethodSpec extends MethodSpec {

	private final int modifiers;
	private final String name;
	private final String descriptor;

	public DescriptorMethodSpec(int modifiers, String name, String descriptor) {
		this.modifiers = modifiers;
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getModifiers() {
		return modifiers;
	}

	@Override
	public TypeSpec[] getParameterTypes() {
		Type[] types = Type.getArgumentTypes(descriptor);
		TypeSpec[] result = new TypeSpec[types.length];
		for (int i = 0; i < types.length; i++) {
			result[i] = TypeSpec.of(types[i].getDescriptor());
		}
		return result;
	}

	@Override
	public TypeSpec getReturnType() {
		return TypeSpec.of(Type.getReturnType(descriptor).getDescriptor());
	}
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

import com.gilecode.xmx.cfg.pattern.impl.ClassTypeSpec;
import com.gilecode.xmx.cfg.pattern.impl.DescriptorTypeSpec;

/**
 * Sub-classes of this abstract class contain information about a type used for matching type patterns.
 *
 * @see com.gilecode.xmx.cfg.pattern.impl.ClassTypeSpec
 * @see com.gilecode.xmx.cfg.pattern.impl.DescriptorTypeSpec
 */
public abstract class TypeSpec {

	public static TypeSpec of(Class<?> type) {
		return new ClassTypeSpec(type);
	}

	public static TypeSpec[] of(Class<?>[] types) {
		TypeSpec[] result = new TypeSpec[types.length];
		for (int i = 0; i < types.length; i++) {
			result[i] = TypeSpec.of(types[i]);
		}
		return result;
	}


	public static TypeSpec of(String typeDescriptor) {
		return new DescriptorTypeSpec(typeDescriptor);
	}
}

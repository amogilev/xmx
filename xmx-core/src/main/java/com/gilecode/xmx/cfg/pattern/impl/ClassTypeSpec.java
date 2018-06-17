// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.TypeSpec;

/**
 * A type specified by the class.
 */
public class ClassTypeSpec extends TypeSpec {

	private final Class<?> type;

	public ClassTypeSpec(Class<?> type) {
		this.type = type;
	}

	public Class<?> getType() {
		return type;
	}
}

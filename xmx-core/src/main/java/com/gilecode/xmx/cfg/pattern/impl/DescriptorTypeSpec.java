// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.TypeSpec;

/**
 * A type specified by the type descriptor.
 */
public class DescriptorTypeSpec extends TypeSpec {

	private final String descriptor;

	public DescriptorTypeSpec(String descriptor) {
		this.descriptor = descriptor;
	}

	public String getDescriptor() {
		return descriptor;
	}
}

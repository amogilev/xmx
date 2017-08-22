// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.PropertyValue;

class PropertyValueImpl implements PropertyValue {
	String value;
	
	private PropertyValueImpl(String value) {
		assert value != null;
		this.value = value;
	}

	@Override
	public String asString() {
		return value;
	}

	@Override
	public int asInt() {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return asBool() ? 1 : 0;
		}
	}

	@Override
	public boolean asBool() {
		return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
	}
	
	static PropertyValueImpl of(String value) {
		return value == null ? null : new PropertyValueImpl(value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		PropertyValueImpl other = (PropertyValueImpl) obj;
		return value.equals(other.value);
	}

	@Override
	public String toString() {
		return asString();
	}
}
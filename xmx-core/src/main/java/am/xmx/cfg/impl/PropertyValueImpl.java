package am.xmx.cfg.impl;

import am.xmx.cfg.PropertyValue;

class PropertyValueImpl implements PropertyValue {
	String value;
	
	private PropertyValueImpl(String value) {
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
			return 0;
		}
	}

	@Override
	public boolean asBool() {
		return Boolean.parseBoolean(value);
	}
	
	static PropertyValueImpl of(String value) {
		return value == null ? null : new PropertyValueImpl(value);
	}
}
// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

import com.gilecode.xmx.service.IXmxClassMembersLookup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

class EmptyMembersLookup implements IXmxClassMembersLookup {

	@Override
	public Map<String, Field> listManagedFields() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Integer, Method> listManagedMethods() {
		return Collections.emptyMap();
	}

	@Override
	public Field getManagedField(String fid) {
		return null;
	}

	@Override
	public Method getManagedMethod(Integer mid) {
		return null;
	}
}

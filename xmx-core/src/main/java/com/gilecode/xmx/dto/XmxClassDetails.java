// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.dto;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class XmxClassDetails extends XmxClassInfo {

	/**
	 * Fields for object and its parents, mapped by unique ID.
	 */
	final private Map<String, Field> managedFields;

	/**
	 * Lists of methods for object and its parents.
	 */
	final private Map<Integer, Method> managedMethods;

	public XmxClassDetails(Integer id, String className, Map<String, Field> managedFields, Map<Integer, Method> managedMethods) {
		super(id, className);
		this.managedFields = managedFields;
		this.managedMethods = managedMethods;
	}

	public Map<String, Field> getManagedFields() {
		return managedFields;
	}

	public Map<Integer, Method> getManagedMethods() {
		return managedMethods;
	}
}

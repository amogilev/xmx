// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.dto;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class XmxClassDetails extends XmxClassInfo {

	/**
	 * Lists of fields for object and its parents, mapped by the class name.
	 */
	final private Map<Integer, Field> managedFields;

	/**
	 * Lists of methods for object and its parents.
	 */
	final private Map<Integer, Method> managedMethods;

	public XmxClassDetails(Integer id, String className, Map<Integer, Field> managedFields, Map<Integer, Method> managedMethods) {
		super(id, className);
		this.managedFields = managedFields;
		this.managedMethods = managedMethods;
	}

	public Map<Integer, Field> getManagedFields() {
		return managedFields;
	}

	public Map<Integer, Method> getManagedMethods() {
		return managedMethods;
	}
}

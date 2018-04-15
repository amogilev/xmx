// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public interface IXmxClassMembersLookup {

	/**
	 * Fields for object and its parents, mapped by unique ID.
	 */
	Map<String, Field> listManagedFields();

	/**
	 * Lists of methods for object and its parents.
	 */
	Map<String, Method> listManagedMethods();

	/**
	 * Get a field by ID which was previously returned as a key by {@link #listManagedFields()}
	 */
	Field getManagedField(String fid);

	/**
	 * Get a method by ID which was previously returned as a key by {@link #listManagedMethods()}
	 */
	Method getManagedMethod(String mid);
}

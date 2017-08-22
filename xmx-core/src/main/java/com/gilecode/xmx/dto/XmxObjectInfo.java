// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.dto;

/**
 * A base information about the object managed in XMX system.
 */
public class XmxObjectInfo {
	
	/**
	 * Unique object ID in XMX system 
	 */
	private final int objectId;
	
	/**
	 * Information about the object's class.
	 */
	private final XmxClassInfo classInfo;

	/**
	 * The managed object itself.
	 */
	private final Object value;
	
	public XmxObjectInfo(int objectId, XmxClassInfo classInfo, Object value) {
		this.objectId = objectId;
		this.classInfo = classInfo;
		this.value = value;
	}

	public int getObjectId() {
		return objectId;
	}

	public XmxClassInfo getClassInfo() {
		return classInfo;
	}

	public Object getValue() {
		return value;
	}
}

// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.model;

import com.gilecode.xmx.service.IXmxClassMembersLookup;

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

	/**
	 * The proxy to the object, if detected.
	 */
	private final Object proxy;
	
	public XmxObjectInfo(int objectId, XmxClassInfo classInfo, Object value, Object proxy) {
		this.objectId = objectId;
		this.classInfo = classInfo;
		this.value = value;
		this.proxy = proxy;
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

	public IXmxClassMembersLookup getMembersLookup() {
		return classInfo.getMembersLookup();
	}

	public Object getProxy() {
		return proxy;
	}
}

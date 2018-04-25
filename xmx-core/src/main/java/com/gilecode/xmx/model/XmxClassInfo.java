// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.model;

import com.gilecode.xmx.service.IXmxClassMembersLookup;

/**
 * DTO XMX-related information about the class.
 */
public class XmxClassInfo {
	
	/**
	 * Unique ID of the class in XMX system, or {@code null} for unmanaged classes
	 */
	private final Integer id;
	
	/**
	 * Name of the class
	 */
	private final String className;

	/**
	 * Members lookup
	 */
	private final IXmxClassMembersLookup membersLookup;

	public XmxClassInfo(Integer id, String className, IXmxClassMembersLookup membersLookup) {
		this.id = id;
		this.className = className;
		this.membersLookup = membersLookup;
	}

	public Integer getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}

	public IXmxClassMembersLookup getMembersLookup() {
		return membersLookup;
	}
}

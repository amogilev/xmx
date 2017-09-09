// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.dto;

/**
 * DTO XMX-related information about the class.
 */
public class XmxClassInfo {
	
	/**
	 * Unique ID of the class in XMX system, or {@code null} for unmanaged classes
	 */
	private Integer id;
	
	/**
	 * Name of the class
	 */
	private String className;
	
	public XmxClassInfo(Integer id, String className) {
		this.id = id;
		this.className = className;
	}

	public Integer getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}
}

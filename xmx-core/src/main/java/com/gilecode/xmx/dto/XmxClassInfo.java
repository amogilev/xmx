// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.dto;

/**
 * DTO information about the managed class.
 */
public class XmxClassInfo {
	
	/**
	 * Unique ID of the class in XMX system 
	 */
	private int id;
	
	/**
	 * Name of the class
	 */
	private String className;
	
	public XmxClassInfo(int id, String className) {
		this.id = id;
		this.className = className;
	}

	public int getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}
	

}

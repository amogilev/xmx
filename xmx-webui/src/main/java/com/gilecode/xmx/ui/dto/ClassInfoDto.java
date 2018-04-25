// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import com.gilecode.xmx.model.XmxClassInfo;

/**
 * DTO for basic class information
 */
public class ClassInfoDto {

	/**
	 * Unique ID of the class in XMX system, or {@code null} for unmanaged classes
	 */
	private final Integer id;

	/**
	 * Name of the class
	 */
	private final String className;

	public ClassInfoDto(XmxClassInfo ci) {
		this.id = ci.getId();
		this.className = ci.getClassName();
	}

	public Integer getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}
}

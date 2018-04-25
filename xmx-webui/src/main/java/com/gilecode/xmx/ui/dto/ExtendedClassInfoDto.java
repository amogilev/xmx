// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import com.gilecode.xmx.model.XmxClassInfo;

/**
 * A class information extended with number of managed objects.
 */
public class ExtendedClassInfoDto extends ClassInfoDto {

	/**
	 * Number of managed objects
	 */
	private final int numberOfObjects;

	public ExtendedClassInfoDto(XmxClassInfo ci, int numberOfObjects) {
		super(ci);
		this.numberOfObjects = numberOfObjects;
	}

	/**
	 * @return numberOfObjects (Number of managed objects)
	 */
	public int getNumberOfObjects() {
		return numberOfObjects;
	}
}

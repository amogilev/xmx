// Copyright © 2014-2018 Andrey Mogilev. All rights reserved.

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
	private final String proxyClass;

	public ExtendedClassInfoDto(XmxClassInfo ci, int numberOfObjects, String proxyClass) {
		super(ci);
		this.numberOfObjects = numberOfObjects;
		this.proxyClass = proxyClass;
	}

	/**
	 * @return numberOfObjects (Number of managed objects)
	 */
	public int getNumberOfObjects() {
		return numberOfObjects;
	}

	public String getProxyClass() {
		return proxyClass;
	}

	public boolean isSingletonWithProxy() {
		return numberOfObjects == 1 && proxyClass != null;
	}
}

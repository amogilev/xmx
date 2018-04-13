// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import com.gilecode.xmx.dto.XmxClassInfo;
import com.gilecode.xmx.service.IXmxClassMembersLookup;

public class ExtendedXmxClassInfo extends XmxClassInfo {

	public ExtendedXmxClassInfo(Integer id, String className, IXmxClassMembersLookup membersLookup) {
		super(id, className, membersLookup);
	}

	/**
	 * Number of managed objects
	 */
	private Integer numberOfObjects;


	/**
	 * @return numberOfObjects (Number of managed objects)
	 */
	public Integer getNumberOfObjects() {
		return numberOfObjects;
	}

	/**
	 * @param numberOfObjects New value of Number of managed objects.
	 */
	public void setNumberOfObjects(Integer numberOfObjects) {
		this.numberOfObjects = numberOfObjects;
	}
}

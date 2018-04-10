// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

import com.gilecode.xmx.dto.XmxObjectInfo;

/**
 * A search result returned by {@link IXmxUiService#findObject(String)}
 */
class SearchObjectResult {

	/**
	 * The root (managed) object found for a given refpath.
	 * For example, for a refpath "$17.a" it will be the managed object with ID=17.
	 */
	final Object rootObject;

	/**
	 * The details of the object found by a given refpath.
	 */
	final XmxObjectInfo foundObjectInfo;

	public SearchObjectResult(Object rootObject, XmxObjectInfo foundObjectInfo) {
		this.rootObject = rootObject;
		this.foundObjectInfo = foundObjectInfo;
	}
}

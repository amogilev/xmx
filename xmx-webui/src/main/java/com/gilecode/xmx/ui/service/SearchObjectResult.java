// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

import com.gilecode.xmx.dto.XmxObjectDetails;

/**
 * A search result returned by {@link IXmxUiService#findObject(String)}
 */
class SearchObjectResult {

	/**
	 * The details of the root (managed) object; e.g. for a refpath "$17.a" it will
	 * be the details of the managed object with ID=17.
	 */
	XmxObjectDetails rootObjectDetails;

	/**
	 * The details of the object found by the original refpath.
	 */
	XmxObjectDetails foundObjectDetails;

	public SearchObjectResult(XmxObjectDetails rootObjectDetails, XmxObjectDetails foundObjectDetails) {
		this.rootObjectDetails = rootObjectDetails;
		this.foundObjectDetails = foundObjectDetails;
	}
}

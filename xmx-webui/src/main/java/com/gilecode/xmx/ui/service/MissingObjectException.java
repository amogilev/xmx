// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

/**
 * An exception thrown when an object cannot be found by ID or refpath.
 * This may happen when an object is already GC'ed (for query by ID) or changed
 * (for refpath query).
 */
public class MissingObjectException extends Exception {

	/**
	 * Optional ID of the missing object, if known.
	 */
	Integer missingObjectId;

	public MissingObjectException() {
	}

	public MissingObjectException(Integer missingObjectId) {
		this.missingObjectId = missingObjectId;
	}
}

// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

/**
 * Indicates errors in refpaths syntax.
 */
public class RefPathSyntaxException extends Exception {

	public RefPathSyntaxException(String message, Throwable cause) {
		super(message, cause);
	}

	public RefPathSyntaxException(String message) {
		super(message);
	}
}

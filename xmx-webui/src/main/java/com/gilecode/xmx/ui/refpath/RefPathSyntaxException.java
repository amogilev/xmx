// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

/**
 * Indicates errors in refpaths syntax.
 */
public class RefPathSyntaxException extends Exception {

	private final String refpath;

	public RefPathSyntaxException(String message, String refpath, Throwable cause) {
		super(message, cause);
		this.refpath = refpath;
	}

	public RefPathSyntaxException(String message, String refpath) {
		super(message);
		this.refpath = refpath;
	}

	public String getRefpath() {
		return refpath;
	}
}

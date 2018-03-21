// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

@SuppressWarnings("serial")
public class XmxIniParseException extends RuntimeException {

	public XmxIniParseException(String message) {
		super(message);
	}

	public XmxIniParseException(String message, Exception cause) {
		super(message, cause);
	}

}

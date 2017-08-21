// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg.impl;

@SuppressWarnings("serial")
public class XmxIniParseException extends RuntimeException {

	XmxIniParseException(String message) {
		super(message);
	}

	public XmxIniParseException(String message, Exception cause) {
		super(message, cause);
	}

}

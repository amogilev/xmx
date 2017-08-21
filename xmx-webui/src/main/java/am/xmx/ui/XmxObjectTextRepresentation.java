// Copyright © 2017 Andrey Mogilev. All rights reserved.

package am.xmx.ui;

import am.xmx.service.IMapperService;

/**
 * String representations of the Object, contains toString() and JSON values
 */
public class XmxObjectTextRepresentation {

	/**
	 * toString() value of the object
	 */
	private final String toStringValue;

	/**
	 * Whether teh object's class has declared toString() method
	 */
	private final boolean isToStringDeclared;

	/**
	 * JSON representation of the object
	 */
	private final String jsonValue;

	private final boolean isJsonTruncated;

	private final long jsonCharsLimit;

	public XmxObjectTextRepresentation(String toStringValue, String jsonValue, long jsonCharsLimit,
									   boolean isToStringDeclared) {
		this.toStringValue = toStringValue;
		this.jsonValue = jsonValue;
		this.isToStringDeclared = isToStringDeclared;
		this.isJsonTruncated = jsonCharsLimit > 0 && jsonValue.endsWith(IMapperService.LIMIT_EXCEEDED_SUFFIX);
		this.jsonCharsLimit = jsonCharsLimit;
	}

	public String getToStringValue() {
		return toStringValue;
	}

	public String getJsonValue() {
		return jsonValue;
	}

	public boolean isJsonTruncated() {
		return isJsonTruncated;
	}

	public long getJsonCharsLimit() {
		return jsonCharsLimit;
	}

	public String getSmartTextValue() {
		return isToStringDeclared ? getToStringValue() : getJsonValue();
	}

	public boolean isSmartUsesJson() {
		return !isToStringDeclared;
	}
}

package am.xmx.ui;

import am.xmx.service.IMapperService;

/**
 * String representations of the Object, contains toString() and JSON values
 */
public class XmxObjectTextRepresentation {
	/**
	 * toString() value of the object
	 */
	final private String toStringValue;

	/**
	 * JSON representation of the object
	 */
	final private String jsonValue;

	final private boolean isJsonTruncated;

	final private long jsonCharsLimit;

	public XmxObjectTextRepresentation(String toStringValue, String jsonValue, long jsonCharsLimit) {
		this.toStringValue = toStringValue;
		this.jsonValue = jsonValue;
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
}

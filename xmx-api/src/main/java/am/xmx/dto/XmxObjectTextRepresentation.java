package am.xmx.dto;

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

	public XmxObjectTextRepresentation(String toStringValue, String jsonValue) {
		this.toStringValue = toStringValue;
		this.jsonValue = jsonValue;
	}

	public String getToStringValue() {
		return toStringValue;
	}

	public String getJsonValue() {
		return jsonValue;
	}
}

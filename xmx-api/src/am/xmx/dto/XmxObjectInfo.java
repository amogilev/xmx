package am.xmx.dto;

/**
 * A base information about the object managed in XMX system.
 */
public class XmxObjectInfo {
	
	/**
	 * Unique object ID in XMX system 
	 */
	private int objectId;
	
	/**
	 * Information about the object's class.
	 */
	private XmxClassInfo classInfo;
	
	/**
	 * Result of {@link Object#toString()}
	 */
	private String strRepresentation;
	
	/**
	 * JSON representation of the object
	 */
	private String jsonRepresentation;

	public XmxObjectInfo(int objectId, XmxClassInfo classInfo,
			String strRepresentation, String jsonRepresentation) {
		super();
		this.objectId = objectId;
		this.classInfo = classInfo;
		this.strRepresentation = strRepresentation;
		this.jsonRepresentation = jsonRepresentation;
	}

	public int getObjectId() {
		return objectId;
	}

	public XmxClassInfo getClassInfo() {
		return classInfo;
	}

	public String getStrRepresentation() {
		return strRepresentation;
	}

	public String getJsonRepresentation() {
		return jsonRepresentation;
	}
}

package am.xmx.dto;

import java.util.List;

/**
 * Interface of XMX service provided for web apps.
 */
public interface XmxService {
	
	/**
	 * Returns names (contexts) of all recognized web applications.
	 */
	List<String> getApplicationNames() throws XmxRuntimeException;
	
	/**
	 * Finds information about the classes of the managed objects by application name
	 * and class name pattern. If both parameters are null, returns all registered classes.
	 * 
	 * @param appNameOrNull the application name as returned by {@link #getApplicationNames()}, or null
	 * @param classNamePatternOrNull the class name, pattern, or null
	 * 
	 * @return matching classes information
	 */
	List<XmxClassInfo> findManagedClassInfos(String appNameOrNull, String classNamePatternOrNull) throws XmxRuntimeException;
	
	/**
	 * Returns all 'live' objects info for the specified class ID, which may be obtained from classes info
	 * return by {@link #findManagedClassInfos(String, String)}.
	 *  
	 * @param classId unique class ID (or null to return all objects)
	 */
	List<XmxObjectInfo> getManagedObjects(Integer classId) throws XmxRuntimeException;
	
	/**
	 * Obtains the details of the object by its ID, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *  
	 * @param objectId the unique object ID
	 */
	XmxObjectDetails getObjectDetails(int objectId) throws XmxRuntimeException;
	
	// TODO: identifying fields and methods is a subject to change, as the order is not guaranteed by JLS 
	
	/**
	 * Sets new value of the specified object field.
	 * 
	 * @param objectId the ID of the object, obtained from {@link XmxObjectInfo#getObjectId()}
	 * @param fieldId the ID of the field to set, obtained from {@link XmxObjectDetails.FieldInfo#getId()}
	 * @param newValue the string representation of the value to assign to the new field
	 *  
	 * @return the new state of the object, after the field is set
	 *  
	 * @throws XmxRuntimeException if failed to assign field
	 */
	XmxObjectDetails setObjectField(int objectId, int fieldId, String newValue) throws XmxRuntimeException;
	
	/**
	 * Invokes the specified method of the object, and returns the returned value as JSON string.
	 * 
	 * @param objectId the ID of the object, obtained from {@link XmxObjectInfo#getObjectId()}
	 * @param methodId the ID of the method to invoke, obtained from {@link XmxObjectDetails.MethodInfo#getId()}
	 * @param args string representation of the arguments to pass, if any
	 *  
	 * @return the method's returned value serialized to JSON
	 *  
	 * @throws XmxRuntimeException if failed to invoke the method, or the method throws exception
	 */
	String invokeObjectMethod(int objectId, int methodId, String...args) throws XmxRuntimeException;
	
	// TBC...
}

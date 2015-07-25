package am.xmx.service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;

/**
 * Interface of XMX service provided for web apps.
 */
public interface IXmxService {
	
	/**
	 * Property name for XMX home folder.
	 */
	String XMX_HOME_PROP = "xmx.home.dir";	
	
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
	 * Invokes the specified method of the object.
	 * 
	 * @param obj the object to use
	 * @param m the method to invoke
	 * @param args the arguments to pass, if any
	 *  
	 * @return the method's returned value
	 *  
	 * @throws XmxRuntimeException if failed to invoke the method
	 * @throws InvocationTargetException if invoked method thrown exception
	 */
	Object invokeObjectMethod(Object obj, Method m, Object...args) throws XmxRuntimeException, InvocationTargetException;

	/**
	 * Returns the managed object with the specified ID, or {@code null} if no
	 * object with such ID is managed now.
	 * <p/>
	 * For example, {@code null} is returned if the managed object is GC'ed.  
	 * 
	 * @param objectId the internal object ID
	 * 
	 * @return the object, or {@code null}
	 */
	Object getObjectById(int objectId);
	
	/**
	 * Resolves and returns actual Method by internal method ID. 
	 * 
	 * @param obj the object to which the method belongs
	 * @param methodId the internal method ID
	 * 
	 * @return the resolved {@link Method}
	 * @throws XmxRuntimeException if the method cannot be resolved
	 */
	Method getObjectMethodById(Object obj, int methodId) throws XmxRuntimeException;
	
	/**
	 * Resolves and returns actual Field by internal field ID. 
	 * 
	 * @param obj the object to which the method belongs
	 * @param fieldId the internal field ID
	 * 
	 * @return the resolved {@link Field}
	 * @throws XmxRuntimeException if the field cannot be resolved
	 */
	Field getObjectFieldById(Object obj, int fieldId) throws XmxRuntimeException;
	
	// TBC...
}

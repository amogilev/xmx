package am.xmx.service;

import am.xmx.core.ManagedClassInfo;
import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Interface of XMX service provided for web apps.
 */
public interface IXmxService {
	
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

	ManagedClassInfo getManagedClassInfo(Class<?> clazz);

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

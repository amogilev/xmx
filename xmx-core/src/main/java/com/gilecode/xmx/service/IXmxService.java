// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import com.gilecode.xmx.dto.*;

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

	/**
	 * Returns detailed information about a class, not matter managed or not.
	 * The returned information includes its fields/methods.
	 *
	 * @param c the class to obtain teh information about
	 *
	 * @return the detailed information about the class
	 */
	XmxClassDetails getClassDetails(Class<?> c);

	/**
	 * Returns all 'live' objects info for the specified class ID, which may be obtained from classes info
	 * return by {@link #findManagedClassInfos(String, String)}.
	 *  
	 * @param classId unique class ID (or null to return all objects)
	 */
	List<XmxObjectInfo> getManagedObjects(Integer classId) throws XmxRuntimeException;

	/**
	 * Returns an managed object (along with a brief meta-information) by its ID.
	 * If this object is already GC'ed, returns null.
	 *
	 * @param objectId unique object ID
	 */
	XmxObjectInfo getManagedObject(int objectId) throws XmxRuntimeException;

	/**
	 * Obtains the details of the object by its ID, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *  
	 * @param objectId the unique object ID
	 */
	XmxObjectDetails getObjectDetails(int objectId) throws XmxRuntimeException;
}

// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import com.gilecode.xmx.model.NotSingletonException;
import com.gilecode.xmx.model.XmxClassInfo;
import com.gilecode.xmx.model.XmxObjectInfo;
import com.gilecode.xmx.model.XmxRuntimeException;

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
	 * Returns information about a class, not matter managed or not.
	 *
	 * @param c the class to obtain the information about
	 *
	 * @return the information about the class
	 */
	XmxClassInfo getClassInfo(Class<?> c);

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
	 * If an object with the specified ID exists, is alive and is singleton, returns permanent ID
	 * which can be used to access that singleton during this or another session. Otherwise, returns {@code null}
	 * <p/>
	 * A singleton is an object which is unique in its web or global application by the class name.
	 *
	 * @param objectId unique object ID
	 *
	 * @return {@code null} if an object ID does not represent singleton, or permanent singleton ID otherwise
	 */
	String getSingletonPermanentId(int objectId);

	/**
	 * Find an object by its permanent singleton ID previously returned by {@link #getSingletonPermanentId(int)}.
	 * If not found or not singleton anymore, throws {@link NotSingletonException} with the detailed reason
	 * available in {@link NotSingletonException#getReason()}
	 *
	 * @param permanentId permanent singleton ID previously returned by {@link #getSingletonPermanentId(int)}
	 *
	 * @return the singleton object
	 *
	 * @throws NotSingletonException if the singleton object can not be found, or not singleton anymore
	 */
	XmxObjectInfo getSingletonObject(String permanentId) throws NotSingletonException;
}

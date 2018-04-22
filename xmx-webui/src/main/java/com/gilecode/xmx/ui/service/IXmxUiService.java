// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.service;

import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.ui.dto.ExtendedXmxClassInfo;
import com.gilecode.xmx.ui.dto.ExtendedXmxObjectDetails;
import com.gilecode.xmx.ui.dto.ExtendedXmxObjectInfo;
import com.gilecode.xmx.ui.dto.XmxObjectTextRepresentation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Handles all accesses to {@link IXmxService} from XMX UI, wraps them with additional logic
 * and transformations required for UI.
 */
public interface IXmxUiService {

	Map<String, Collection<ExtendedXmxClassInfo>> getAppsAndClasses();

	/**
	 * If there is exactly one alive managed class instance, return its internal ID.
	 * Otherwise, returns null.
	 */
	Integer getManagedClassSingleInstanceId(int classId);

	List<ExtendedXmxObjectInfo> getManagedClassInstancesInfo(Integer classId);

	/**
	 * Obtains the details of the object by its refpath, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *
	 * @param refpath the object refpath, like "$18.arrField.1"
	 * @param arrPageNum if the object is array, specifies the page of the array elements to provide the details of
	 */
	ExtendedXmxObjectDetails getExtendedObjectDetails(String refpath, int arrPageNum) throws MissingObjectException, RefPathSyntaxException;

	/**
	 * Searches an object by refpath, i.e. either managed object (for refpaths like "$23"), or an object traversed
	 * from the managed objects using "getField" or "getArrayElement" parts of the refpath.
	 *
	 * @param refpath the path to an object which starts with a managed object ID, followed by field specifiers or
	 *                array element indexes, separated with '.', e.g. "$23.myArrField.0"
	 *
	 * @return a search result which consists of object details for the root and the found objects
	 *
	 * @throws MissingObjectException if a managed object is not found by ID
	 * @throws RefPathSyntaxException if a refpath is incorrect, i.e. has bad syntax or an sub-object cannot be extracted
	 * 	using "getField" ot "getArrayElement" specifiers
	 */
	SearchObjectResult findObject(String refpath) throws MissingObjectException, RefPathSyntaxException;

	XmxObjectTextRepresentation invokeObjectMethod(String refpath, String methodId, String[] argsArr)
			throws MissingObjectException, RefPathSyntaxException, Throwable;

	/**
	 * Sets the object's field or array's element to the value specified by the string representation.
	 * @param refpath the object refpath, like "$18.arrField.1"
	 * @param elementId the ID of the element to set: either field name (with optional "superclass" suffix), or
	 *                     array element's number
	 * @param value the string (usually JSON) representation of the value to set
	 * @throws MissingObjectException if a managed object is missing now
	 * @throws RefPathSyntaxException if refpath is not valid
	 */
	void setObjectFieldOrElement(String refpath, String elementId, String value) throws MissingObjectException, RefPathSyntaxException;

	void printAllObjectsReport(PrintWriter out);

	void printFullObjectJson(String refpath, String fid, PrintWriter out) throws IOException, RefPathSyntaxException, MissingObjectException;

	String getCurrentSessionId();
}

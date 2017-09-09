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
	 * Obtains the details of the object by its ID, which includes all fields
	 * and methods. If this object is already GC'ed, returns null.
	 *
	 * @param objectId the unique object ID
	 */
	ExtendedXmxObjectDetails getExtendedObjectDetails(int objectId) throws MissingObjectException;

	ExtendedXmxObjectDetails getExtendedObjectDetails(String refpath) throws MissingObjectException, RefPathSyntaxException;

	XmxObjectTextRepresentation invokeObjectMethod(Integer objectId, int methodId, String[] argsArr)
			throws MissingObjectException, Throwable;

	void setObjectField(Integer objectId, Integer fieldId, String value) throws MissingObjectException;

	void printAllObjectsReport(PrintWriter out);

	void printFullObjectJson(int objectId, Integer fieldId, PrintWriter out) throws IOException;
}

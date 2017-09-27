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

	XmxObjectTextRepresentation invokeObjectMethod(String refpath, int methodId, String[] argsArr)
			throws MissingObjectException, RefPathSyntaxException, Throwable;

	void setObjectField(String refpath, String fid, String value) throws MissingObjectException, RefPathSyntaxException;

	void printAllObjectsReport(PrintWriter out);

	void printFullObjectJson(String refpath, String fid, PrintWriter out) throws IOException, RefPathSyntaxException, MissingObjectException;
}

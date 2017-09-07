// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import com.gilecode.xmx.dto.XmxClassInfo;
import com.gilecode.xmx.dto.XmxObjectInfo;

public class ExtendedXmxObjectInfo extends XmxObjectInfo {
	private final XmxObjectTextRepresentation text;

	public ExtendedXmxObjectInfo(int objectId, XmxClassInfo classInfo, Object value, XmxObjectTextRepresentation text) {
		super(objectId, classInfo, value);
		this.text = text;
	}

	public ExtendedXmxObjectInfo(XmxObjectInfo objInfo, XmxObjectTextRepresentation text) {
		this(objInfo.getObjectId(), objInfo.getClassInfo(), objInfo.getValue(), text);
	}

	public XmxObjectTextRepresentation getText() {
		return text;
	}
}

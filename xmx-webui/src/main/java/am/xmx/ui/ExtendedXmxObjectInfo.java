// Copyright © 2017 Andrey Mogilev. All rights reserved.

package am.xmx.ui;

import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectInfo;

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

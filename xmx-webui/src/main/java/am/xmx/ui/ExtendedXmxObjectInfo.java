package am.xmx.ui;

import am.xmx.dto.XmxObjectInfo;

public class ExtendedXmxObjectInfo extends XmxObjectInfo {
	private final XmxObjectTextRepresentation text;

	public ExtendedXmxObjectInfo(XmxObjectInfo objInfo, XmxObjectTextRepresentation text) {
		super(objInfo.getObjectId(), objInfo.getClassInfo(), objInfo.getValue());
		this.text = text;
	}

	public XmxObjectTextRepresentation getText() {
		return text;
	}
}

// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

/**
 * DTO for basic object information
 */
public class ObjectInfoDto {

	/**
	 * Unique object ID in XMX system
	 */
	private final int objectId;

	/**
	 * Information about the object's class.
	 */
	private final ClassInfoDto classInfo;

	private final XmxObjectTextRepresentation text;

	/**
	 * Class of the Spring proxy object, if detected.
	 */
	private final String proxyClass;

	public ObjectInfoDto(int objectId, ClassInfoDto classDto, XmxObjectTextRepresentation text, String proxyClass) {
		this.objectId = objectId;
		this.classInfo = classDto;
		this.text = text;
		this.proxyClass = proxyClass;
	}

	public XmxObjectTextRepresentation getText() {
		return text;
	}

	public int getObjectId() {
		return objectId;
	}

	public ClassInfoDto getClassInfo() {
		return classInfo;
	}

	public String getProxyClass() {
		return proxyClass;
	}
}

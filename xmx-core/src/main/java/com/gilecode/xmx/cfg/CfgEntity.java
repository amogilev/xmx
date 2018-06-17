// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg;

import java.util.Objects;

/**
 * Represents one of configurable entities - application,
 * class or member. Additionally, a special SYSTEM entity
 * is used for accessing system-level properties.
 * 
 * @author Andrey Mogilev
 */
public class CfgEntity {
	
	public static final CfgEntity SYSTEM = new CfgEntity(null, null, null, false);
	
	/**
	 * The application name, or {@code null} for system entity.
	 */
	private String appName;
	
	/**
	 * The class name, or {@code null} for system or app entities.
	 */
	private String className;
	
	/**
	 * The field or method name, or {@code null} for system, app 
	 * or class entities
	 */
	private String memberName;

	/**
	 * Whether the member represents a method or a field.
	 */
	private boolean memberIsMethod;

	private CfgEntity(String appName, String className, String memberName, boolean memberIsMethod) {
		this.appName = appName;
		this.className = className;
		this.memberName = memberName;
		this.memberIsMethod = memberIsMethod;

		assert appName != null || (className == null && memberName == null) : "Missing appName";
		assert className != null || memberName == null : "Missing className";
	}
	
	public static CfgEntity ofApp(String appName) {
		return new CfgEntity(appName, null, null, false);
	}
	
	public static CfgEntity ofClass(String appName, String className) {
		return new CfgEntity(appName, className, null, false);
	}

	public static CfgEntity ofMethod(String appName, String className, String methodName) {
		return new CfgEntity(appName, className, methodName, true);
	}

	public static CfgEntity ofField(String appName, String className, String fieldName) {
		return new CfgEntity(appName, className, fieldName, false);
	}

	public static CfgEntity ofMember(String appName, String className, String memberName, boolean memberIsMethod) {
		return new CfgEntity(appName, className, memberName, memberIsMethod);
	}
	
	public CfgEntityLevel getLevel() {
		return CfgEntityLevel.levelFor(appName, className, memberName, memberIsMethod);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CfgEntity cfgEntity = (CfgEntity) o;
		return memberIsMethod == cfgEntity.memberIsMethod &&
				Objects.equals(appName, cfgEntity.appName) &&
				Objects.equals(className, cfgEntity.className) &&
				Objects.equals(memberName, cfgEntity.memberName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(appName, className, memberName, memberIsMethod);
	}
}

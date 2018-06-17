// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.CfgEntityLevel;

import java.util.regex.Pattern;

/**
 * Parsed name for System, App-level, Class-level or Member-level configuration section. 
 * 
 * @author Andrey Mogilev
 */
class SectionHeader {

	Pattern appPattern;
	Pattern classPattern;
	Pattern memberPattern;
	boolean memberIsMethod;

	CfgEntityLevel level;
	
	String appSpec;
	String classSpec;
	String memberSpec;
	
	SectionHeader() {
		initLevel();
	}

	SectionHeader(Pattern appPattern, Pattern classPattern) {
		this (appPattern, classPattern, null, false);
	}
	
	SectionHeader(Pattern appPattern, Pattern classPattern,
			Pattern memberPattern, boolean memberIsMethod) {
		this.appPattern = appPattern;
		this.classPattern = classPattern;
		this.memberPattern = memberPattern;
		this.memberIsMethod = memberIsMethod;

		if (appPattern == null || (classPattern == null && memberPattern != null)) {
			throw new IllegalArgumentException("Illegal SectionHeader");
		}

		initLevel();
	}

	void initLevel() {
		level = CfgEntityLevel.levelFor(appPattern, classPattern, memberPattern, memberIsMethod);
	}
	
	/**
	 * Used for property matching. Property level is determined by the least non-null
	 * name, sections with lower level are considered non-matching (i.e. Member-level
	 * sections cannot contain App-level properties).
	 */
	public boolean matches(String appName, String className, String memberName, boolean memberIsMethod) {
		return check(appPattern, appName) && check(classPattern, className) &&
			(memberPattern == null ||
			this.memberIsMethod == memberIsMethod && check(memberPattern, memberName));
	}
	
	/**
	 * Used for property matching in app subconfigs, where all sections are known
	 * to match the application. 
	 */
	public boolean matchesAfterApp(String className, String memberName, boolean memberIsMethod) {
		return check(classPattern, className) &&
			(memberPattern == null ||
			this.memberIsMethod == memberIsMethod && check(memberPattern, memberName));
	}

	/**
	 * Checks only app name, ignore other parts.
	 */
	public boolean appMatches(String appName) {
		return check(appPattern, appName);
	}
	
	private static boolean check(Pattern pattern, String name) {
		if (pattern == null) {
			// no check at this level
			return true;
		} else if (name == null) {
			// higher level of property than of section
			return false;
		} else if (name.equals("*")) {
			// special case, used to collect distinct values of a property for all members
			return true;
		}
		return pattern.matcher(name).matches();
	}

	@Override
	public String toString() {
		if (appSpec == null) {
			return "[No specs]";
		}
		StringBuilder sb = new StringBuilder(64);
		
		sb.append("[App=").append(appSpec);
		if (classSpec != null) {
			sb.append(";Class=").append(classSpec);
		}
		if (memberSpec != null) {
			sb.append(";Member=").append(memberSpec);
		}
		sb.append(']');
		return sb.toString();
	}
}

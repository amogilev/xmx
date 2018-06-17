// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.CfgEntityLevel;
import com.gilecode.xmx.cfg.pattern.IMethodMatcher;
import com.gilecode.xmx.cfg.pattern.MethodSpec;

import java.util.regex.Pattern;

/**
 * Parsed name for System, App-level, Class-level or Member-level configuration section.
 * Provides convenient methods for property matching.
 * 
 * @author Andrey Mogilev
 */
class SectionHeader {

	Pattern appPattern;
	Pattern classPattern;
	IMethodMatcher methodMatcher;
	Pattern fieldPattern; // will be changed to IFieldMatcher eventually
	boolean hasMemberPart;

	CfgEntityLevel level;
	
	String appSpec;
	String classSpec;
	String methodOrFieldSpec;
	
	SectionHeader() {
		init();
	}

	SectionHeader(Pattern appPattern, Pattern classPattern) {
		this (appPattern, classPattern, null, null);
	}
	
	SectionHeader(Pattern appPattern, Pattern classPattern,
	              IMethodMatcher methodMatcher, Pattern fieldPattern) {
		this.appPattern = appPattern;
		this.classPattern = classPattern;
		this.methodMatcher = methodMatcher;
		this.fieldPattern = fieldPattern;

		init();

		if (methodMatcher != null && fieldPattern != null) {
			throw new IllegalArgumentException("Illegal SectionHeader: both Method and Field parts");
		}

		if (appPattern == null || (classPattern == null && hasMemberPart)) {
			throw new IllegalArgumentException("Illegal SectionHeader");
		}
	}

	void init() {
		this.hasMemberPart = methodMatcher != null || fieldPattern != null;
		Object memberPart = hasMemberPart ? this : null; // any non-null Object works here
		this.level = CfgEntityLevel.levelFor(appPattern, classPattern, memberPart, methodMatcher != null);
	}
	
	public boolean isMatchingClassSection(String appName, String className) {
		return appMatches(appName) && isMatchingClassSection(className);
	}

	public boolean isMatchingMethodSection(String appName, String className, MethodSpec methodSpec) {
		return appMatches(appName) && isMatchingMethodSection(className, methodSpec);
	}

	/**
	 * Check whether this section is a Field section which matches specified app, class and field.
	 */
	public boolean isMatchingFieldSection(String appName, String className, String fieldName) {
		return appMatches(appName) && isMatchingFieldSection(className, fieldName);
	}

	/**
	 * Used for property matching in app subconfigs, where all sections are known
	 * to match the application. 
	 */
	public boolean isMatchingClassSection(String className) {
		return level == CfgEntityLevel.CLASS && check(classPattern, className);
	}

	public boolean isMatchingFieldSection(String className, String fieldName) {
		return level == CfgEntityLevel.FIELD && check(classPattern, className)
				&& (fieldName == null || fieldPattern.matcher(fieldName).matches());
	}

	/**
	 * Return {@code true} iff this section is a Method section which matches the class name and the
	 * method, if the latter is specified. If {@code null} 'methodSpec' is provided, then only the
	 * class name is checked.
	 *
	 * @param className the class name to match against the Class part
	 * @param methodSpec the method specification to match, or {@code null}
	 *
	 * @return whether this section is a matching Method-level section
	 */
	public boolean isMatchingMethodSection(String className, MethodSpec methodSpec) {
		return level == CfgEntityLevel.METHOD && check(classPattern, className)
				&& (methodSpec == null || methodMatcher.matches(methodSpec));
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
		if (methodOrFieldSpec != null) {
			String key = fieldPattern == null ? "Method" : "Field";
			sb.append(";").append(key).append("=").append(methodOrFieldSpec);
		}
		sb.append(']');
		return sb.toString();
	}
}

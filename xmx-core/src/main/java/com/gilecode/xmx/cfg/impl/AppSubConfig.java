// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.cfg.PropertyValue;

import java.util.List;

public class AppSubConfig implements IAppPropertiesSource {
	
	/**
	 * The application name.
	 */
	@SuppressWarnings("unused")
	private String appName;
	
	/**
	 * The list of all sections with headers matching the application name,
	 * in reversed order.
	 */
	private List<SectionWithHeader> matchingSectionsReversed;
	
	// TODO: fast path for "Managed" property
	
	AppSubConfig(String appName, List<SectionWithHeader> matchingSectionsReversed) {
		this.appName = appName;
		this.matchingSectionsReversed = matchingSectionsReversed;
	}

	@Override
	public PropertyValue getAppProperty(String propName) {
		return getProperty(null, null, propName);
	}
	
	@Override
	public PropertyValue getClassProperty(String className, String propName) {
		return getProperty(className, null, propName);
	}

	@Override
	public PropertyValue getMemberProperty(String className, String memberName, String propName) {
		return getProperty(className, memberName, propName);
	}
	
	/**
	 * Internal method which works with all property levels from App to Member. 
	 */
	private PropertyValue getProperty(String className,
			String memberName, String propName) {
		
		if (memberName == null && Properties.isSpecial(propName)) {
			return getSpecialClassProperty(className, propName);
		}
		
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().matchesAfterApp(className, memberName) && sh.containsKey(propName)) {
				return PropertyValueImpl.of(sh.get(propName));
			}
		}
		
		return null;
	}
	
	/**
	 * Supports "special" class properties and their Class-pattern form, like Managed & ManangedClasses.
	 */
	private PropertyValue getSpecialClassProperty(String className, String propName) {
		String propClassForm = Properties.specialClassesForm(propName);
		
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().matchesAfterApp(className, null)) {
				if (sh.containsKey(propName)) {
					return PropertyValueImpl.of(sh.get(propName)); 
				} else if (sh.getHeader().classSpec == null && sh.containsKey(propClassForm)) {
					String patternValue = sh.get(propClassForm);
					boolean propValue = PatternsSupport.matches(patternValue, className);
					return PropertyValueImpl.of(Boolean.toString(propValue));
				}
			}
		}
		
		// default for all special boolean-based properties is "false" 
		return PropertyValueImpl.of(Boolean.FALSE.toString());
	}
}
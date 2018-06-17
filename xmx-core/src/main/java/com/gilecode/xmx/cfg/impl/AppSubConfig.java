// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.cfg.PropertyValue;
import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.cfg.pattern.PatternsSupport;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
		return getProperty(null, null, false, propName);
	}
	
	@Override
	public PropertyValue getClassProperty(String className, String propName) {
		return getProperty(className, null, false, propName);
	}

	@Override
	public PropertyValue getMethodProperty(String className, MethodSpec methodSpec, String propName) {
		// FIXME: match method pattern instead
		return getProperty(className, methodSpec.getName(), true, propName);
	}

	@Override
	public PropertyValue getFieldProperty(String className, String fieldName, String propName) {
		return getProperty(className, fieldName, false, propName);
	}

	@Override
	public List<PropertyValue> getDistinctMethodPropertyValues(String className, String propName) {
		Set<String> distinctValues = new LinkedHashSet<>();
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().matchesAfterApp(className, "*", true) && sh.containsKey(propName)) {
				distinctValues.add(sh.get(propName));
			}
		}

		List<PropertyValue> result = new ArrayList<>(distinctValues.size());
		for (String val : distinctValues) {
			result.add(PropertyValueImpl.of(val));
		}

		return result;
	}
	
	/**
	 * Internal method which works with all property levels from App to Member. 
	 */
	private PropertyValue getProperty(String className,
			String memberName, boolean memberIsMethod, String propName) {
		
		if (memberName == null && Properties.isSpecial(propName)) {
			return getSpecialClassProperty(className, propName);
		}
		
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().matchesAfterApp(className, memberName, memberIsMethod) && sh.containsKey(propName)) {
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
			if (sh.getHeader().matchesAfterApp(className, null, false)) {
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

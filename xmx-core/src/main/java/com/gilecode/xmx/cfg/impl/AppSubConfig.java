// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.CfgEntityLevel;
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
		for (SectionWithHeader sh : matchingSectionsReversed) {
			SectionHeader header = sh.getHeader();
			if (header.level == CfgEntityLevel.APP && sh.containsKey(propName)) {
				return PropertyValueImpl.of(sh.get(propName));
			}
		}

		return null;
	}
	
	@Override
	public PropertyValue getClassProperty(String className, String propName) {
		if (Properties.isSpecial(propName)) {
			return getSpecialClassProperty(className, propName);
		}

		for (SectionWithHeader sh : matchingSectionsReversed) {
			SectionHeader header = sh.getHeader();
			// currently. class properties are allowed both in App and Class sections
			if (sh.containsKey(propName) &&
					(header.level == CfgEntityLevel.APP || header.isMatchingClassSection(className))) {
				return PropertyValueImpl.of(sh.get(propName));
			}
		}

		return null;
	}

	@Override
	public PropertyValue getMethodProperty(String className, MethodSpec methodSpec, String propName) {
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().isMatchingMethodSection(className, methodSpec) && sh.containsKey(propName)) {
				return PropertyValueImpl.of(sh.get(propName));
			}
		}

		return null;
	}

	@Override
	public PropertyValue getFieldProperty(String className, String fieldName, String propName) {
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().isMatchingFieldSection(className, fieldName) && sh.containsKey(propName)) {
				return PropertyValueImpl.of(sh.get(propName));
			}
		}

		return null;
	}

	@Override
	public List<PropertyValue> getDistinctMethodPropertyValues(String className, String propName) {
		Set<String> distinctValues = new LinkedHashSet<>();
		for (SectionWithHeader sh : matchingSectionsReversed) {
			if (sh.getHeader().isMatchingMethodSection(className, null) && sh.containsKey(propName)) {
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
	 * Supports "special" class properties and their Class-pattern form, like Managed & ManagedClasses.
	 */
	private PropertyValue getSpecialClassProperty(String className, String propName) {
		String propClassForm = Properties.specialClassesForm(propName);
		
		for (SectionWithHeader sh : matchingSectionsReversed) {
			SectionHeader header = sh.getHeader();
			if (header.level == CfgEntityLevel.APP && sh.containsKey(propClassForm)) {
				String patternValue = sh.get(propClassForm);
				boolean propValue = PatternsSupport.matches(patternValue, className);
				return PropertyValueImpl.of(Boolean.toString(propValue));
			} else if (header.isMatchingClassSection(className) && sh.containsKey(propName)) {
				return PropertyValueImpl.of(sh.get(propName));
			}
		}
		
		// default for all special boolean-based properties is "false" 
		return PropertyValueImpl.of(Boolean.FALSE.toString());
	}
}

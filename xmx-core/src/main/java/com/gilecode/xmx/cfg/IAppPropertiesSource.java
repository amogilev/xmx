// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg;

import java.util.List;

/**
 * Sub-config which provides read access to App, Class and Member
 * properties. 
 */
public interface IAppPropertiesSource {
	
	PropertyValue getAppProperty(String propName);
	
	PropertyValue getClassProperty(String className, String propName);
	
	PropertyValue getMethodProperty(String className, String methodName, String propName);

	PropertyValue getFieldProperty(String className, String fieldName, String propName);

	List<PropertyValue> getDistinctMethodPropertyValues(String className, String propName);
}
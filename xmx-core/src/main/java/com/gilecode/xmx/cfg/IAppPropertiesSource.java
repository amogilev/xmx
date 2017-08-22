// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg;

/**
 * Sub-config which provides read access to App, Class and Member
 * properties. 
 */
public interface IAppPropertiesSource {
	
	PropertyValue getAppProperty(String propName);
	
	PropertyValue getClassProperty(String className, String propName);
	
	PropertyValue getMemberProperty(String className, String memberName, String propName);
}
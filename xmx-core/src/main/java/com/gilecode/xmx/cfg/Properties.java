// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Collection of names of known properties.
 * 
 * @author Andrey Mogilev
 */
public final class Properties {
	
	//
	// known global properties
	//
	public static final String GLOBAL_ENABLED = "Enabled";
	public static final String GLOBAL_EMB_SERVER_ENABLED = "EmbeddedWebServer.Enabled";
	public static final String GLOBAL_EMB_SERVER_IMPL = "EmbeddedWebServer.Impl";
	public static final String GLOBAL_EMB_SERVER_PORT = "EmbeddedWebServer.Port";
	public static final String GLOBAL_JMX_ENABLED = "JMX.Bridge.Enabled";
	public static final String GLOBAL_LOG_LEVEL = "Log.Level";
	public static final String GLOBAL_LOG_DIR = "Log.Dir";
	public static final String GLOBAL_LOG_CFG_FILE = "Log.LogbackCfg.File";

	//
	// "hidden" global properties
	//
	public static final String GLOBAL_WELCOME_XMX_NAME = "Welcome.Name";
	public static final String GLOBAL_WELCOME_SKIP_HOME = "Welcome.Home.Skip";

	//
	// known class-level (and above) properties
	//
	public static final String CLASS_MAX_INSTANCES = "MaxInstances";
	
	//
	// "special" class-level properties
	//
	public static final String SP_MANAGED = "Managed";

	//
	// known method-level properties
	//
	public static final String M_ADVICES = "Advices";

	private static final String SPECIAL_CLASSES_SUFFIX = "Classes";
	
	//
	// known application-level properties
	//
	public static final String APP_ENABLED = "AppManagementEnabled";
	
	// all known names of System-level properties
	private static final Set<String> ALL_SYSTEM_PROPNAMES = new HashSet<>(Arrays.asList(
			GLOBAL_ENABLED, GLOBAL_EMB_SERVER_ENABLED, GLOBAL_EMB_SERVER_IMPL, GLOBAL_EMB_SERVER_PORT, 
			GLOBAL_JMX_ENABLED, GLOBAL_LOG_LEVEL, GLOBAL_LOG_DIR, GLOBAL_LOG_CFG_FILE,
			GLOBAL_WELCOME_XMX_NAME, GLOBAL_WELCOME_SKIP_HOME));
	
	// all known names of Class-level properties
	private static final Set<String> ALL_CLASS_PROPNAMES = new HashSet<>(Arrays.asList(
			CLASS_MAX_INSTANCES, SP_MANAGED));
	
	// all known names of Method-level properties
	private static final Set<String> ALL_METHOD_PROPNAMES = Collections.singleton(M_ADVICES);

	// all known names of Field-level properties - none yet
	private static final Set<String> ALL_FIELD_PROPNAMES = Collections.emptySet();

	// all known names of App-level properties - includes all class-level properties, theirs special '*Classes" form,
	//  and App-only properies 
	private static final Set<String> ALL_APP_PROPNAMES = new HashSet<>();

	static {
		ALL_APP_PROPNAMES.addAll(Arrays.asList(APP_ENABLED, specialClassesForm(SP_MANAGED)));
		ALL_APP_PROPNAMES.addAll(ALL_CLASS_PROPNAMES);
	}
	
	// no public constructor
	private Properties() {
	}

	/**
	 * Returns whether a property is a special class property which may occur both
	 * on App level as a classes pattern, and on a class level, as simple property.
	 * <p/>For example: 'ManagedClasses' in App section and 'Managed' in Classes
	 * section.
	 *   
	 * @param propName the "simple" property name
	 */
	public static boolean isSpecial(String propName) {
		return SP_MANAGED.equals(propName);
	}
	
	public static String specialClassesForm(String specialPropertySimpleName) {
		assert isSpecial(specialPropertySimpleName);
		return specialPropertySimpleName + SPECIAL_CLASSES_SUFFIX;
	}
	
	public static boolean isKnownProperty(CfgEntityLevel level, String name) {
		switch(level) {
		case SYSTEM:
			return ALL_SYSTEM_PROPNAMES.contains(name);
		case APP:
			return ALL_APP_PROPNAMES.contains(name);
		case CLASS:
			return ALL_CLASS_PROPNAMES.contains(name);
		case METHOD:
			return ALL_METHOD_PROPNAMES.contains(name);
		case FIELD:
			return ALL_FIELD_PROPNAMES.contains(name);
		default:
			assert false;
			return false;
		}
	}
}

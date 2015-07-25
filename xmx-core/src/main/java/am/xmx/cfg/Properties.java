package am.xmx.cfg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	public static final String GLOBAL_SORT_FIELDS = "Sort.Fields";
	
	private static final String[] ALL_GLOBAL_PROPERTIES = {GLOBAL_ENABLED, GLOBAL_EMB_SERVER_ENABLED, 
		GLOBAL_EMB_SERVER_IMPL, GLOBAL_EMB_SERVER_PORT, GLOBAL_JMX_ENABLED, GLOBAL_SORT_FIELDS};
	
	//
	// known application-level properties
	//
	public static final String APP_ENABLED = "ManagementEnabled";
	
	//
	// known class-level properties
	//
	public static final String CLASS_MAX_INSTANCES = "MaxInstances";
	
	//
	// properties available on several levels
	//
	public static final String PROP_SECURITY_CLASS = "SecurityClass";
	
	
	//
	// "special" class-level properties
	//
	public static final String SP_MANAGED = "Managed";
	
	private static final String SPECIAL_CLASSES_SUFFIX = "Classes";
	
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
	
	/**
	 * Returns unmodifiable list of known system properties.
	 */
	public static List<String> getKnownSystemProperties() {
		return Collections.unmodifiableList(Arrays.asList(ALL_GLOBAL_PROPERTIES));
	}
	
}

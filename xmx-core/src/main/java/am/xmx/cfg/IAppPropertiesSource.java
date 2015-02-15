package am.xmx.cfg;

/**
 * Sub-config which provides read access to App, Class and Member
 * properties. 
 */
public interface IAppPropertiesSource {
	
	PropertyValue getAppProperty(String propName);
	
	PropertyValue getClassProperty(String className, String propName);
	
	PropertyValue getMemberProperty(String className, String memberName, String propName);
}
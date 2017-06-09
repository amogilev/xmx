package am.xmx.cfg;

import java.io.File;
import java.util.Map;

/**
 * Interface to XMX configuration. Provides the following different types of access:
 * <ul>
 * 
 * <li> {@link IXmxPropertiesSource} is the recommended way for checking
 * the value of known system or entity properties. All override and special
 * properties rules are applied automatically, always returning actual 
 * property value.
 * 
 * <li> {@link IConfigManager} provides means to view all properties for some entity,
 * to check if that properties are overridden on lower levels, and to change the
 * properties or overrides.
 * 
 * </ul>
 * <p/>
 * Newly added and overwritten entity properties are available only after restart.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxConfig extends IXmxPropertiesSource /*, IConfigManager */ {
	
	/**
	 * Overrides known global (XMX [System]) configuration properties
	 * <p/>
	 * Property names are compared case-insensitively, so any case of override
	 * properties is accepted.
	 * <p/>
	 * As most of properties are used only at startup, the override takes place
	 * only if done before the actual use of those properties.
	 * 
	 * @param properties properties to override
	 */
	void overrideSystemProperties(Map<String, String> properties);

	/**
	 * Return the configuration file used
	 */
	File getConfigurationFile();
}

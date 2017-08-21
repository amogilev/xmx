// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg;


/**
 * Read-only source of XMX properties available for different
 * entities - system or specific applications, classes or their members.
 * <p/>
 * For performance reasons, all properties except system ones shall
 * be obtained through {@link IAppPropertiesSource} sub-configs. The sub-configs
 * are lazily initialized and cached, but they also may be cached locally.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxPropertiesSource {
	
	/**
	 * Returns a values of a system property.
	 */
	PropertyValue getSystemProperty(String name);
	
	/**
	 * Obtain sub-config for the application specified by name.
	 */
	IAppPropertiesSource getAppConfig(String appName);
}

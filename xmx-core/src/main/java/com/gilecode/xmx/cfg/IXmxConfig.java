// Copyright © 2015-2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg;

import com.gilecode.ucfg.ConfigLoadStatus;

import java.io.File;

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
	 * Return the configuration file used
	 */
	File getConfigurationFile();

	/**
	 * Invoked after the logging is initialized. If there are deferred log events, they shall be replayed now.
	 */
	void onLoggingInitialized();

	/**
	 * Returns the loading status
	 */
	ConfigLoadStatus getLoadStatus();
}

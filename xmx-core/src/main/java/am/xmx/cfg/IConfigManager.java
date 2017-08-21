// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg;

import java.util.Map;

/**
 * Provides means to view all properties for some entity,
 * to check if that properties are overridden on lower levels, and to change the
 * properties or overrides.
 * 
 * @author Andrey Mogilev
 */
public interface IConfigManager {
	
	/**
	 * Returns all system-level properties.
	 */
	Map<String, String> getAllSystemProperties();
	
	/**
	 * Sets new system-level properties. Old properties are discarded, i.e.
	 * if an existing property is missing in the new map, it is deleted.
	 */
	void setAllSystemProperties(Map<String, String> systemProperties);
	
	/**
	 * Obtains a config manager for the specified entity (app, class or member),
	 * which allows to get and set properties for that entity.
	 */
	IEntityConfigManager getEntityConfigManager(CfgEntity entity);
	
}

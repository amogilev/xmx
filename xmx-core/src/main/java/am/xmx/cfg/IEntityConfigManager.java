package am.xmx.cfg;

import java.util.List;
import java.util.Map;

/**
 * Manages properties for some entity (app, class or member).
 * 
 * @author Andrey Mogilev
 */
public interface IEntityConfigManager {
	
	/**
	 * Get all properties for the entity.
	 * <p/>
	 * These properties may be defined in different sections - the 
	 * section corresponding to the entity, sections with matching patterns,
	 * and parent sections.
	 */
	Map<String, String> getAllProperties();
	
	/**
	 * Ensures the property. If the property is explicitly defined in the 
	 * section corresponding the entity, it is modified. Otherwise, a new override
	 * property is added to a section corresponding the entity (the section
	 * may be created or moved if needed).
	 */
	void setProperty(String propName, String propValue);
	
	/**
	 * Get list of all overrides of the property in children entities, if any.
	 */
	List<IEntityPropertyOverride> getPropertyOverrides(String propertyName);
	
	/**
	 * Deletes a property override previously returned by {@link #getPropertyOverrides()}
	 */
	boolean deletePropertyOverride(IEntityPropertyOverride override);
	
	/**
	 * Modifies a property override previously returned by {@link #getPropertyOverrides()}
	 * by changing its value to the new one.
	 */
	boolean changePropertyOverride(IEntityPropertyOverride override, String newValue);
}

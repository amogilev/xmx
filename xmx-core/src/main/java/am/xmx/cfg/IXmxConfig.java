package am.xmx.cfg;

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
public interface IXmxConfig extends IXmxPropertiesSource, IConfigManager {
	
}

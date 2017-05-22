package am.xmx.boot;

import java.util.Map;

/**
 * Provides bootstrap functionality for the agent and transformed classes, including the
 * class transformation method and registering of beans.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxBootService {

	/**
	 * Property name for XMX home folder.
	 */
	String XMX_HOME_PROP = "xmx.home.dir";

	/**
	 * Checks whether the class is configured to be managed and, if needed,
	 * transform it so that each created instance of it will
	 * be registered.
	 * 
	 * @param classLoader the class loader
	 * @param className	the name of the class
	 * @param classBuffer the bytecode of the class
	 * @param classBeingRedefined in case of the hot code replacement, the class which is 
	 * 	modified; otherwise, {@code null} 
	 * 
	 * @return the resulting bytecode, either transformed or left intact
	 */
	byte[] transformClassIfInterested(ClassLoader classLoader, String className, byte[] classBuffer, 
			Class<?> classBeingRedefined);
	
	/**
	 * Registers a newly created object, so that it becomes managed by XMX.
	 */
	void registerObject(Object obj, int classId);
	
	/**
	 * Overrides known global (XMX [System]) configuration properties
	 * <p/>
	 * Property names are compared case-insensitively, so any case of override
	 * properties is accepted.
	 * 
	 * @param properties properties to override
	 */
	void overrideSystemProperties(Map<String, String> properties);
	
	/**
	 * Returns whether XMX is globally enabled in the configuration. 
	 */
	boolean isEnabled();

}

package am.xmx.service;

import java.util.Map;

/**
 * Extends base {@link IXmxService} interface with the method used
 * for class transformation and registering of beans.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxServiceEx extends IXmxService {
	
	/**
	 * Checks whether the class is configured to be managed and, if needed,
	 * transform it so that each created instance of it will
	 * be registered.
	 * 
	 * @param classLoader the class loader
	 * @param className	the name of the class
	 * @param classBuffer the bytecode of the class
	 * 
	 * @return the resulting bytecode, either transformed or left intact
	 */
	byte[] transformClassIfInterested(ClassLoader classLoader, String className, byte[] classBuffer);
	
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

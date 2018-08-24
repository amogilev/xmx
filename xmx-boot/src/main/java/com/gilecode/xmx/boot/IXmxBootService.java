// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

/**
 * Provides bootstrap functionality for the agent and transformed classes, including the
 * class transformation method and registering of beans.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxBootService {

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
	 * Registers a proxy object (e.g. Spring bean proxy) to a target managed object.
	 * Is supposed to be ignored if the target is not managed.
	 */
	void registerProxyObject(Object target, Object proxy);

	/**
	 * Returns whether XMX is globally enabled in the configuration. 
	 */
	boolean isEnabled();

	/**
	 * Return AOP manager used
	 */
	IXmxAopService getAopService();

	/**
	 * Creates and returns a wrapper to a logger which can be used by advices.
	 * The returned wrapper will have interface {@link com.gilecode.xmx.aop.log.IAdviceLogger},
	 * which is not declared to avoid extra dependencies for xmx-boot module.
	 */
	Object getAdviceLogger(String name);
}

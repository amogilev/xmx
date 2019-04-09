// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

import com.gilecode.xmx.aop.log.IAdviceLogger;

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
	 * The returned wrapper will have interface {@link IAdviceLogger},
	 * which is not declared to avoid extra dependencies for xmx-boot module.
	 */
	IAdviceLogger getAdviceLogger(String name);

	/**
	 * Propagates an event with one argument from some advice to the XMX core and plugins.
	 *
	 * @param pluginId  optional ID of the target plugin for the event; should be propagated to all if {@code null}
	 * @param eventName the name of the event
	 * @param arg       the only event argument, or {@code null} for 0-arguments events
	 */
	void fireAdviceEvent(String pluginId, String eventName, Object arg);

	/**
	 * Propagates an event with two arguments from some advice to the XMX core and plugins.
	 *
	 * @param pluginId  optional ID of the target plugin for the event; should be propagated to all if {@code null}
	 * @param eventName the name of the event
	 * @param arg1      the first of two event arguments
	 * @param arg2      the second of two event arguments
	 */
	void fireAdviceEvent(String pluginId, String eventName, Object arg1, Object arg2);
}

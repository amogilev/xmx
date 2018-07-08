// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;

/**
 * Extended service available for core classes.
 */
public interface IXmxCoreService extends IXmxService {

	/**
	 * Obtain a managed class loader reference for the given class loader, making it
	 * managed if it was not managed yet.
	 */
	ManagedClassLoaderWeakRef getOrInitManagedClassLoader(ClassLoader classLoader);

}

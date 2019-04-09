// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import com.gilecode.xmx.boot.IXmxBootService;
import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;
import com.gilecode.xmx.core.ManagedObjectWeakRef;

/**
 * Extended service available for core classes.
 */
public interface IXmxCoreService extends IXmxService, IXmxBootService {

	/**
	 * Obtain a managed class loader reference for the given class loader, making it
	 * managed if it was not managed yet.
	 */
	ManagedClassLoaderWeakRef getOrInitManagedClassLoader(ClassLoader classLoader);

	/**
	 * Finds and returns the reference to a managed object instance.
	 * If the instance is not currently managed, returns {@code null}.
	 */
	ManagedObjectWeakRef findManagedObjectRef(Object obj);

	/**
	 * Finds and returns the managed object reference by internal ID.
	 * If the instance is not currently managed, returns {@code null}.
	 */
	ManagedObjectWeakRef getManagedObjectRef(int objectId);
}

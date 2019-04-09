// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.plugin;

/**
 * Extra information collected about a managed object, which is related to a specific XMX plugin.
 * <p/>
 * This information is usually stored in {@link com.gilecode.xmx.core.ManagedObjectWeakRef}.
 */
public abstract class AbstractPluginManagedObjectInfo {

    public abstract String getPluginId();

    public abstract String getType();

}

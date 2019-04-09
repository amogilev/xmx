// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.plugin;

/**
 * Extra information collected about a managed class, which is related to a specific XMX plugin.
 * <p/>
 * Currently it is not used, but in future it may be stored in stored in {@link com.gilecode.xmx.core.XmxClassManager}
 * and signify that the plugin is interested in instances of that class and shall be informed when such instances
 * are created or destroyed.
 */
public abstract class AbstractPluginManagedClassInfo {

    public abstract String getPluginId();

}

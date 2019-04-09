// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.plugin;

public interface IXmxPlugin {

    String getPluginId();

    boolean processEvent(String eventName, Object arg);

    boolean processEvent(String eventName, Object arg1, Object arg2);

    // not needed now, may be added in future
//    void onClassRegistered(XmxClassManager classInfo);
//    void onClassDestroyed(XmxClassManager classInfo);
//    void onObjectDestroyed(ManagedObjectWeakRef ref);
}

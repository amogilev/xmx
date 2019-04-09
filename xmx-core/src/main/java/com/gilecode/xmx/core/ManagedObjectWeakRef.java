// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import com.gilecode.xmx.plugin.AbstractPluginManagedObjectInfo;

import javax.management.ObjectName;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Weak reference for managed object, which additionally contains 
 * object data. 
 */
public class ManagedObjectWeakRef extends WeakReference<Object> {
	
	final int objectId;
	final XmxClassManager classInfo;

	ObjectName jmxObjectName;

	// TODO maybe move to plugin info
	WeakReference<Object> springProxy;

	private List<AbstractPluginManagedObjectInfo> extraInfo = null;
	
	public ManagedObjectWeakRef(Object referent, ReferenceQueue<Object> q,
	                            int objectId, XmxClassManager classInfo, ObjectName jmxObjectName) {
		super(referent, q);
		this.objectId = objectId;
		this.classInfo = classInfo;
		this.jmxObjectName = jmxObjectName;
	}

	/**
	 * Adds a new information object, or returns existing one of the same plugin/type.
	 * @return added or found information object
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T extends AbstractPluginManagedObjectInfo> T findOrAddExtraInfo(T info) {
		AbstractPluginManagedObjectInfo found = getExtraInfo(info.getPluginId(), info.getType());
		if (found != null) {
			return (T) found;
		} else {
			if (extraInfo == null) {
				extraInfo = new ArrayList<>(1);
			}
			extraInfo.add(info);
			return info;
		}
	}

	public synchronized AbstractPluginManagedObjectInfo getExtraInfo(String pluginId, String type) {
		if (extraInfo != null) {
			for (AbstractPluginManagedObjectInfo info : extraInfo) {
				if (type.equals(info.getType()) && info.getPluginId().equals(pluginId)) {
					return info;
				}
			}
		}
		return null;
	}
}

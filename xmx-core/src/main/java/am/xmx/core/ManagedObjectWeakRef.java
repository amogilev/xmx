package am.xmx.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import javax.management.ObjectName;

/**
 * Weak reference for managed object, which additionally contains 
 * object data. 
 */
public class ManagedObjectWeakRef extends WeakReference<Object> {
	
	int objectId;
	ManagedClassInfo classInfo;
	ObjectName jmxObjectName;
	
	public ManagedObjectWeakRef(Object referent, ReferenceQueue<Object> q, 
			int objectId, ManagedClassInfo classInfo, ObjectName jmxObjectName) {
		super(referent, q);
		this.objectId = objectId;
		this.classInfo = classInfo;
		this.jmxObjectName = jmxObjectName;
	}
}

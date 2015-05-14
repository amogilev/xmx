package am.xmx.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Weak reference for managed object, which additionally contains 
 * object data. 
 */
class ManagedObjectWeakRef extends WeakReference<Object> {
	
	int objectId;
	int classId;

	public ManagedObjectWeakRef(Object referent, ReferenceQueue<Object> q, int objectId, int classId) {
		super(referent, q);
		this.objectId = objectId;
		this.classId = classId;
	}

}

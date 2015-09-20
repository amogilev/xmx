package am.xmx.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Weak reference for class loaders of managed classes, suitable for use
 * as a keys of hashmaps.
 * 
 * @author Andrey Mogilev
 */
public class ManagedClassLoaderWeakRef extends WeakReference<ClassLoader> {
	
	private final int hashCode;
	
	/**
	 * All IDs of managed classes loaded by the referent class loader, mapped by class name.
	 */
	private final ConcurrentMap<String, Integer> classIdsByName;
	
	private ManagedClassLoaderWeakRef(ClassLoader referent, ReferenceQueue<? super ClassLoader> q, 
			ConcurrentMap<String, Integer> classIdsByName) {
		super(referent, q);
		this.hashCode = referent.hashCode();
		this.classIdsByName = classIdsByName;
	}
	
	public ManagedClassLoaderWeakRef(ClassLoader referent, ReferenceQueue<? super ClassLoader> q) {
		this(referent, q, new ConcurrentHashMap<String, Integer>(1024));
	}
	
	public static ManagedClassLoaderWeakRef key(ClassLoader referent) {
		return new ManagedClassLoaderWeakRef(referent, null, null);
	}
	
	public ConcurrentMap<String, Integer> getClassIdsByName() {
		return classIdsByName;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		// check equality of the two references
		ManagedClassLoaderWeakRef other = (ManagedClassLoaderWeakRef) obj;
		
		ClassLoader thisReferent = this.get();
		ClassLoader otherReferent = other.get();
		
		// use identity equality for ClassLoader; it is compatible with its hashCode
		return thisReferent == otherReferent;
	}
}

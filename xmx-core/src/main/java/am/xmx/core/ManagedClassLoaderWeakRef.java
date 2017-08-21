// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

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
	
	/**
	 * Class and constant to represent bootstrap class loader, which is
	 * {@code null} in the current Java implementation.
	 */
	private static final class NullClassLoader extends ClassLoader {
	}
	
	static final NullClassLoader NULL_CL = new NullClassLoader();
	
	/**
	 * Hash code of the referent class loader, used as hash code of this
	 * reference too.
	 */
	private final int hashCode;
	
	/**
	 * The app to which this class loader belongs.
	 */
	private final ManagedAppInfo appInfo;
	
	/**
	 * All IDs of managed classes loaded by the referent class loader, mapped by class name.
	 */
	private final ConcurrentMap<String, Integer> classIdsByName;
	
	
	private ManagedClassLoaderWeakRef(ClassLoader referent, ReferenceQueue<? super ClassLoader> q, 
			ManagedAppInfo appInfo, ConcurrentMap<String, Integer> classIdsByName) {
		super(referent, q);
		this.hashCode = referent.hashCode();
		this.appInfo = appInfo;
		this.classIdsByName = classIdsByName;
	}
	
	public ManagedClassLoaderWeakRef(ClassLoader referent, ReferenceQueue<? super ClassLoader> q, 
			ManagedAppInfo appInfo) {
		this(referent, q, appInfo, new ConcurrentHashMap<String, Integer>(1024));
	}
	
	public static ManagedClassLoaderWeakRef key(ClassLoader referent) {
		return new ManagedClassLoaderWeakRef(referent, null, null, null);
	}
	
	public ManagedAppInfo getAppInfo() {
		return appInfo;
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

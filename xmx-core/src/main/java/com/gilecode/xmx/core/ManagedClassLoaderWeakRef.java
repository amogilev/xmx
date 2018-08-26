// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import com.gilecode.xmx.aop.data.AdviceClassInfo;
import com.gilecode.xmx.core.params.ParamNamesCache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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

	/**
	 * Count of all alive managed instances of all managed classes loaded by this class loader
	 */
	private final AtomicInteger managedInstancesCount = new AtomicInteger();

	/**
	 * All smart references managed by this class loader.
	 */
	private final Collection<SmartReference<?>> smartReferences = new ConcurrentLinkedQueue<>();

	/**
	 * A reference which can switch between strong and weak references based on the current count of managed instances.
	 * <b/>
	 * It can be used to "bind" dependent class loaders to this class loader, without creating cyclic dependencies which
	 * would prevent Class GC. When there are some managed instances of any class loaded by this CL still alive, all
	 * the smart references are in "strong" mode and thus prevents referents to be GC'ed.
	 * <br/>
	 * On the other hand, such binding is not absolutely reliable (it is possible that all managed instances are
	 * collected but then created again; or there are non-managed instances left; or there are concurrency issues
	 * between creating/removing managed instances and references, etc.), so this mechanism shall be considered
	 * only as an optimization. There is still a possibility that teh reference is GC'ed, so the users of the smart
	 * references shall be able to re-load the actual referents at any time.
	 */
	public static class SmartReference<T> {
		private volatile WeakReference<T> weakReference;
		private volatile T strongReference;

		private SmartReference(T strongReference, boolean strong) {
			assert strongReference != null;
			if (strong) {
				this.strongReference = strongReference;
			} else {
				this.weakReference = new WeakReference<>(strongReference);
			}
		}

		public T get() {
			T localStrongRef = strongReference;
			if (localStrongRef != null) {
				return localStrongRef;
			}

			// NOTE: even if concurrently switched to "strong" mode here, we still can safely use the weak ref
			WeakReference<T> localWeakRef = this.weakReference;
			if (localWeakRef != null) {
				return localWeakRef.get();
			} else {
				return null;
			}
		}

		void update(boolean newStrong) {
			T localStrongRef = strongReference;
			boolean curStrong = localStrongRef != null;
			if (curStrong != newStrong) {
				if (newStrong) {
					// weak ref can be used even if already switched to "strong" in another thread
					this.strongReference = weakReference.get();
				} else {
					this.weakReference = new WeakReference<>(localStrongRef);
					this.strongReference = null;
				}
			}
		}
	}

	/**
	 * The advice ClassLoaders mapped by the JAR name, used for the target classes loaded by this class loader.
	 */
	private final ConcurrentMap<String, SmartReference<ClassLoader>> adviceJarLoaders = new ConcurrentHashMap<>();

	/**
	 * The advice descriptors (jar + class name) which failed to be loaded or verified in the context of this class
	 * loader.
	 * <br/>
	 * Note that the same advice could potentially be successfully loaded for another class loader, e.g. if some
	 * required classes exist in that context and not in this one.
	 */
	private final Set<String> knownBadAdviceDescs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

	/**
	 * The cache of successfully verified advice classes, mapped by advice descriptor.
	 */
	private final Map<String, AdviceClassInfo> verifiedAdvicesByDesc = new ConcurrentHashMap<>();

	private final ParamNamesCache paramNamesCache = new ParamNamesCache();

	private ManagedClassLoaderWeakRef(ClassLoader referent, ReferenceQueue<? super ClassLoader> q, 
			ManagedAppInfo appInfo, ConcurrentMap<String, Integer> classIdsByName) {
		super(referent = handleNullCL(referent), q);
		this.hashCode = referent.hashCode();
		this.appInfo = appInfo;
		this.classIdsByName = classIdsByName;
	}

	private static ClassLoader handleNullCL(ClassLoader referent) {
		return referent == null ? NULL_CL : referent;
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

	public ConcurrentMap<String, SmartReference<ClassLoader>> getAdviceJarLoaders() {
		return adviceJarLoaders;
	}

	public Set<String> getKnownBadAdviceDescs() {
		return knownBadAdviceDescs;
	}

	public Map<String, AdviceClassInfo> getVerifiedAdvicesByDesc() {
		return verifiedAdvicesByDesc;
	}

	public <T> SmartReference<T> createSmartReference(T referent) {
		SmartReference<T> ref = new SmartReference<>(referent, true);
		smartReferences.add(ref);
		// update after adding to storage
		ref.update(managedInstancesCount.get() > 0);
		return ref;
	}

	public <T> void removeSmartReference(SmartReference<T> smartRef) {
		smartReferences.remove(smartRef);
		smartRef.update(false);
	}

	void incrementManagedInstancesCount() {
		if (managedInstancesCount.incrementAndGet() == 1) {
			// use double-checking to prevent GC locks in case of concurrency between inc/dec counts
			synchronized (this) {
				if (managedInstancesCount.get() > 0) {
					for (Iterator<SmartReference<?>> iterator = smartReferences.iterator(); iterator.hasNext(); ) {
						SmartReference<?> sr = iterator.next();
						sr.update(true);
						if (sr.get() == null) {
							iterator.remove();
						}
					}
				}
			}
		}
	}

	void decrementManagedInstancesCount() {
		if (managedInstancesCount.decrementAndGet() == 0) {
			synchronized (this) {
				if (managedInstancesCount.get() == 0) {
					for (Iterator<SmartReference<?>> iterator = smartReferences.iterator(); iterator.hasNext(); ) {
						SmartReference<?> sr = iterator.next();
						if (sr.get() == null) {
							iterator.remove();
						} else {
							sr.update(false);
						}
					}
				}
			}
		}
	}

	public ParamNamesCache getParamNamesCache() {
		return paramNamesCache;
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

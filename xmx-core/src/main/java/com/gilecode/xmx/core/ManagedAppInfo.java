// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Descriptor of a (sub)application inside a managed Java process.
 * <p/>
 * If a managed Java classes are run on the platform of some supported
 * application server, they are grouped according to which application
 * they belong. System classes and user classes which do not belong to 
 * any particular subapplication, are considered belonging to a "default" 
 * application with empty name. 
 * 
 * @author Andrey Mogilev
 *
 */
public class ManagedAppInfo {
	
	/**
	 * Internal ID of the application.
	 */
	private final int id;
	
	/**
	 * Application name.
	 */
	private final String name;
	
	/**
	 * References and information about the class loaders which belong to the app and
	 * contain managed classes.
	 * <p/>
	 * NOTE: it is not a Set, as we need putIfAbsent. It is not a Map from ClassLoader, as we can only use WeakRefs
	 */
	private final ConcurrentMap<ManagedClassLoaderWeakRef, ManagedClassLoaderWeakRef> classLoaderInfos =
			new ConcurrentHashMap<>(8);

	private final ConcurrentMap<String, Collection<ManagedClassLoaderWeakRef>> classLoadersWithClassName = new ConcurrentHashMap<>(256);

	public ManagedAppInfo(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public ConcurrentMap<ManagedClassLoaderWeakRef, ManagedClassLoaderWeakRef> getClassLoaderInfos() {
		return classLoaderInfos;
	}
	
	/**
	 * Finds or initializes a reference and information for the given class loader, which shall
	 * belong this app.
	 */
	public ManagedClassLoaderWeakRef getOrInitManagedClassLoaderInfo(ClassLoader cl, ReferenceQueue<ClassLoader> managedClassLoadersRefQueue) {
		// at first, try to find using simple iterations
		ManagedClassLoaderWeakRef ref = findManagedClassLoaderInfo(cl);
		if (ref != null) {
			return ref;
		}

		// if iteration fails, putIfAbsent approach will do getOrInit atomically
		ManagedClassLoaderWeakRef candidate = new ManagedClassLoaderWeakRef(cl, managedClassLoadersRefQueue, this);
		ref = classLoaderInfos.putIfAbsent(candidate, candidate);
		
		// null ref means that putIfAbsent actually put candidate, so it shall be used 
		return ref == null ? candidate : ref;
	}

	public ManagedClassLoaderWeakRef findManagedClassLoaderInfo(ClassLoader cl) {
		if (cl == null) {
			// use special placeholder to represent bootstrap class loader
			cl = ManagedClassLoaderWeakRef.NULL_CL;
		}

		// at first, try simple iteration to prevent extraneous creation of weak references
		for (ManagedClassLoaderWeakRef ref : classLoaderInfos.keySet()) {
			if (ref.get() == cl) {
				return ref;
			}
		}

		return null;
	}

	/**
	 * Returns a list of managed class ids.
	 */
	public List<Integer> getManagedClassIds() {
		List<Integer> result = new ArrayList<>(100);
		for (ManagedClassLoaderWeakRef clRef : classLoaderInfos.values()) {
			if (clRef.get() != null) {
				result.addAll(clRef.getClassIdsByName().values());
			}
		}
		return result;
	}

	public void registerClass(ManagedClassLoaderWeakRef clRef, String className, Integer classId) {
		// CAS loop
		boolean success = false;
		while (!success) {
			Collection<ManagedClassLoaderWeakRef> oldVal = classLoadersWithClassName.get(className);
			Collection<ManagedClassLoaderWeakRef> newVal;
			if (oldVal == null) {
				newVal = Collections.singleton(clRef);
				success = classLoadersWithClassName.putIfAbsent(className, newVal) == null;
			} else {
				// NOTE: do not use Set instead of ArrayList, as expected number of dup classes is very small!
				// NOTE: do not 'add' to collection (like ConcurrentHashSet) instead of copy, as it would be much harder
				//       (or even impossible) to correctly remove the empty collection when all these classes GC'ed
				newVal = new ArrayList<>(oldVal.size() + 1);
				newVal.addAll(oldVal);
				newVal.add(clRef);
				newVal = Collections.unmodifiableCollection(newVal);
				success = classLoadersWithClassName.replace(className, oldVal, newVal);
			}
		}
		clRef.getClassIdsByName().put(className, classId);
	}

	public void unregisterLoaderClasses(ManagedClassLoaderWeakRef clRef) {
		for (String className : clRef.getClassIdsByName().keySet()) {
			unregisterClass(clRef, className);
		}
		classLoaderInfos.remove(clRef);
	}

	private void unregisterClass(ManagedClassLoaderWeakRef clRef, String className) {
		// CAS loop
		boolean success = false;
		while (!success) {
			Collection<ManagedClassLoaderWeakRef> oldVal = classLoadersWithClassName.get(className);
			Collection<ManagedClassLoaderWeakRef> newVal;
			if (oldVal == null) {
				// unexpected, but fine
				// TODO: log or assert?
				success = true;
			} else if (oldVal.size() == 1) {
				success = classLoadersWithClassName.remove(className, oldVal);
			} else {
				newVal = new ArrayList<>(oldVal);
				newVal.remove(clRef);
				newVal = Collections.unmodifiableCollection(newVal);
				success = classLoadersWithClassName.replace(className, oldVal, newVal);
			}
		}
		// NOTE: classIdsByName actually cleaned in ClassCleanerThread
		// clRef.getClassIdsByName().remove(className);
	}

	public List<Integer> getClassIdsByName(String className) {
		Collection<ManagedClassLoaderWeakRef> clRefs = classLoadersWithClassName.get(className);
		List<Integer> result = new ArrayList<>(clRefs.size());
		for (ManagedClassLoaderWeakRef clRef : clRefs) {
			Integer classId = clRef.getClassIdsByName().get(className);
			if (classId != null) {
				result.add(classId);
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ManagedAppInfo other = (ManagedAppInfo) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ManagedAppInfo [id=" + id + ", name=" + name + "]";
	}

}

package am.xmx.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
	 * List of managed class ids belonging to this app.
	 */
	private final List<Integer> classIds = new ArrayList<>(1024);
	
	private final ReadWriteLock classListLock = new ReentrantReadWriteLock();
	
	/**
	 * References and information about the class loaders which belong to the app and
	 * contain managed classes.
	 */
	private final ConcurrentMap<ManagedClassLoaderWeakRef, ManagedClassLoaderWeakRef> classLoaderInfos = 
			new ConcurrentHashMap<>(8);

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
	public ManagedClassLoaderWeakRef getOrInitManagedClassLoaderInfo(ClassLoader cl) {
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
		
		// if iteration fails, putIfAbsent approach will do getOrInit atomically
		ManagedClassLoaderWeakRef candidate = new ManagedClassLoaderWeakRef(cl, XmxManager.managedClassLoadersRefQueue, this);
		ManagedClassLoaderWeakRef ref = classLoaderInfos.putIfAbsent(candidate, candidate);
		
		// null ref means that putIfAbsent actually put candidate, so it shall be used 
		return ref == null ? candidate : ref;
	}

	public void addManagedClassId(int classId) {
		classListLock.writeLock().lock();
		try {
			classIds.add(classId);
		} finally {
			classListLock.writeLock().unlock();
		}
	}
	
	public void removeManagedClassIds(Collection<Integer> classIds) {
		if (classIds.size() > 0) {
			classListLock.writeLock().lock();
			try {
				classIds.removeAll(classIds);
			} finally {
				classListLock.writeLock().unlock();
			}
		}
	}
	
	interface ClassIdProcessor {
		void process(int classId);
	}
	
	public void processManagedClassIds(ClassIdProcessor processor) {
		classListLock.readLock().lock();
		try {
			for (Integer classId : classIds) {
				processor.process(classId);
			}
		} finally {
			classListLock.readLock().unlock();
		}
	}
	
	/**
	 * Returns a copy of list of managed class ids. 
	 */
	public List<Integer> getManagedClassIds() {
		classListLock.readLock().lock();
		try {
			return new ArrayList<>(classIds);
		} finally {
			classListLock.readLock().unlock();
		}
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

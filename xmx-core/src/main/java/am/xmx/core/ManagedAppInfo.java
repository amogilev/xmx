package am.xmx.core;

import java.util.ArrayList;
import java.util.List;
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
	private int id;
	
	/**
	 * Application name.
	 */
	private String name;
	
	/**
	 * List of managed class ids belonging to this app.
	 */
	private List<Integer> classIds = new ArrayList<>(1024);
	
	private ReadWriteLock classListLock = new ReentrantReadWriteLock(); 

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
	
	public void addManagedClassId(int classId) {
		classListLock.writeLock().lock();
		try {
			classIds.add(classId);
		} finally {
			classListLock.writeLock().unlock();
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

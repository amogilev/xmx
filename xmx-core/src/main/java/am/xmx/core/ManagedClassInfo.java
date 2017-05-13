package am.xmx.core;

import am.xmx.dto.XmxRuntimeException;

import javax.management.modelmbean.ModelMBeanInfoSupport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManagedClassInfo {
	
	/**
	 * Unique ID of the class in XMX system 
	 */
	private final int id;
	
	/**
	 * Name of the class
	 */
	private final String className;
	
	/**
	 * Corresponding class loader info.
	 */
	private final ManagedClassLoaderWeakRef loaderInfo;
	
	/**
	 * Corresponding application.
	 */
	private final ManagedAppInfo appInfo;
	
	/**
	 * Max managed instances allowed.
	 */
	private final int maxInstances;
	
	/**
	 * Part of JMX ObjectName (without object ID) used for managed 
	 * class instances, or {@code null} if they do not need to be
	 * published to JMX.
	 */
	private final String jmxObjectNamePart;
	
	// other fields are initiated when first object of the class is registered
	
	/**
	 * List of managed methods. Index in the list is
	 * equal to the internal method ID. 
	 */
	private List<Method> managedMethods;
	
	/**
	 * List of managed fields. Index in the list is
	 * equal to the internal field ID. 
	 */
	private List<Field> managedFields;
	
	/**
	 * JMX model for managed class instances, or {@code null}
	 * if they need not to be published to JMX.
	 */
	private ModelMBeanInfoSupport jmxClassModel;
	
	/**
	 * Managed objects of this class, if any.
	 * <strong>NOTE:</strong> This set is not thread-safe, require
	 * external synchronization.
	 */
	private Set<Integer> objectIds;	
	
	private boolean disabledByMaxInstances; // TODO maybe extend to custom flags 
	
	public ManagedClassInfo(int id, String className, ManagedClassLoaderWeakRef loaderInfo, 
			ManagedAppInfo appInfo, int maxInstances, String jmxObjectNamePart) {
		this.id = id;
		this.className = className;
		this.loaderInfo = loaderInfo;
		this.appInfo = appInfo;
		this.maxInstances = maxInstances;
		this.jmxObjectNamePart = jmxObjectNamePart;
	}

	public void init(List<Method> managedMethods, List<Field> managedFields, 
			ModelMBeanInfoSupport jmxClassModel) {
		this.managedMethods = managedMethods;
		this.managedFields = managedFields;
		this.jmxClassModel = jmxClassModel;
		this.objectIds = new HashSet<>(2);
	}
	
	public boolean isInitialized() {
		return managedMethods != null;
	}
	
	/**
	 * Resets the previous initialization, reverts to uninitialized form with only basic information about the 
	 * class. This method may be invoked only when there are no alive instances of the class left.
	 */
	// synchronized // NOTE: actually, all uses are synchronized on XmxManager.instance, similar to all accesses to objectIds
	public void reset() {

		if (objectIds != null && objectIds.size() > 0) {
			throw new IllegalStateException("Cannot reset ManagedClassInfo while some instances are still "
					+ "alive; class=" + className);
		}
		
		this.managedFields = null;
		this.managedMethods = null;
		this.jmxClassModel = null;
		this.objectIds = null;
		this.disabledByMaxInstances = false;
	}
	
	public int getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}
	
	public ManagedClassLoaderWeakRef getLoaderInfo() {
		return loaderInfo;
	}

	public ManagedAppInfo getAppInfo() {
		return appInfo;
	}

	public ModelMBeanInfoSupport getJmxClassModel() {
		return jmxClassModel;
	}
	
	public String getJmxObjectNamePart() {
		return jmxObjectNamePart;
	}
	
	public List<Method> getManagedMethods() {
		return managedMethods;
	}

	public List<Field> getManagedFields() {
		return managedFields;
	}

	public Method getMethodById(int methodId) {
		List<Method> methods = managedMethods;
		if (methods == null) {
			throw new XmxRuntimeException("ClassInfo is not initialized for " + className);
		}
		if (methodId < 0 || methodId >= methods.size()) {
			throw new XmxRuntimeException("Method not found in " + className + " by ID=" + methodId);
		}
		Method m = methods.get(methodId);
		if (!m.isAccessible()) {
			m.setAccessible(true);
		}
		return m;
	}
	
	public Field getFieldById(int fieldId) {
		List<Field> fields = managedFields;
		if (fields == null) {
			throw new XmxRuntimeException("ClassInfo is not initialized for " + className);
		}
		if (fieldId < 0 || fieldId >= fields.size()) {
			throw new XmxRuntimeException("Field not found in " + className + " by ID=" + fieldId);
		}
		return fields.get(fieldId);
	}
	
	public boolean isDisabled() {
		return disabledByMaxInstances;
	}

	public boolean isDisabledByMaxInstances() {
		return disabledByMaxInstances;
	}

	public void setDisabledByMaxInstances(boolean disabledByMaxInstances) {
		this.disabledByMaxInstances = disabledByMaxInstances;
	}

	public Set<Integer> getObjectIds() {
		return objectIds;
	}
	
	public int getMaxInstances() {
		return maxInstances;
	}

	// NOTE: no ID and JMX info in hashCode & equals! That's OK as it not used as Map keys
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
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
		ManagedClassInfo other = (ManagedClassInfo) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "XmxManagedClassInfo [id=" + id + ", className=" + className
				+ "]";
	}
}


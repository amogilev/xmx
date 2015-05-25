package am.xmx.dto;

import java.lang.reflect.Method;
import java.util.List;

import javax.management.modelmbean.ModelMBeanInfoSupport;

public class XmxClassInfo {
	
	/**
	 * Unique ID of the class in XMX system 
	 */
	private int id;
	
	/**
	 * Name of the class
	 */
	private String className;
	
	/**
	 * List of managed methods. Index in the list is
	 * equal to the internal method ID. 
	 */
	private List<Method> managedMethods;
	
	/**
	 * JMX model for managed class instances, or {@code null}
	 * if they need not to be published to JMX.
	 */
	ModelMBeanInfoSupport jmxClassModel;
	
	/**
	 * Part of JMX ObjectName (without object ID) used for managed 
	 * class instances, or {@code null} if they need not to be 
	 * published to JMX.
	 */
	String jmxObjectNamePart;
	
	public XmxClassInfo(int id, String className, List<Method> managedMethods, 
			ModelMBeanInfoSupport jmxClassModel, String jmxObjectNamePart) {
		this.id = id;
		this.className = className;
		this.managedMethods = managedMethods;
		this.jmxClassModel = jmxClassModel;
		this.jmxObjectNamePart = jmxObjectNamePart;
	}
	
	public int getId() {
		return id;
	}

	public String getClassName() {
		return className;
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

	public Method getMethodById(int methodId) {
		if (methodId < 0 || methodId >= managedMethods.size()) {
			throw new XmxRuntimeException("Method not found in " + className + " by ID=" + methodId);
		}
		return managedMethods.get(methodId);
	}

	// NOTE: no ID and JMX info in hashCode & equals!

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
		XmxClassInfo other = (XmxClassInfo) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}
}

package am.xmx.dto;

import javax.management.modelmbean.ModelMBeanInfo;

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
	 * JMX model for managed class instances, or {@code null}
	 * if they need not to be published to JMX.
	 */
	ModelMBeanInfo jmxClassModel;
	
	/**
	 * Part of JMX ObjectName (without object ID) used for managed 
	 * class instances, or {@code null} if they need not to be 
	 * published to JMX.
	 */
	String jmxObjectNamePart;
	
	public XmxClassInfo(int id, String className, ModelMBeanInfo jmxClassModel, String jmxObjectNamePart) {
		this.id = id;
		this.className = className;
		this.jmxClassModel = jmxClassModel;
		this.jmxObjectNamePart = jmxObjectNamePart;
	}
	
	public int getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}
	
	
	public ModelMBeanInfo getJmxClassModel() {
		return jmxClassModel;
	}
	
	public String getJmxObjectNamePart() {
		return jmxObjectNamePart;
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

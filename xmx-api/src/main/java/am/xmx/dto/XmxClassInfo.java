package am.xmx.dto;

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
	 * Unique ID within XMX system of the class loader of the class.
	 */
	private int classLoaderId;
	
	/**
	 * Name of the web application, if known
	 */
	private String appName;

	public XmxClassInfo(int id, String className, int classLoaderId,
			String appName) {
		super();
		this.id = id;
		this.className = className;
		this.classLoaderId = classLoaderId;
		this.appName = appName;
	}
	
	public int getId() {
		return id;
	}

	public String getClassName() {
		return className;
	}

	public int getClassLoaderId() {
		return classLoaderId;
	}

	public String getAppName() {
		return appName;
	}
	
	// NOTE: no ID in hashCode & equals!

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appName == null) ? 0 : appName.hashCode());
		result = prime * result + classLoaderId;
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
		if (appName == null) {
			if (other.appName != null)
				return false;
		} else if (!appName.equals(other.appName))
			return false;
		if (classLoaderId != other.classLoaderId)
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}
}

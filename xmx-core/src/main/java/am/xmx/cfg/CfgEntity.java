package am.xmx.cfg;

/**
 * Represents one of configurable entities - application,
 * class or member. Additionally, a special SYSTEM entity
 * is used for accessing system-level properties.
 * 
 * @author Andrey Mogilev
 */
public class CfgEntity {
	
	public static final CfgEntity SYSTEM = new CfgEntity(null, null, null);
	
	/**
	 * The application name, or {@code null} for system entity.
	 */
	private String appName;
	
	/**
	 * The class name, or {@code null} for system or app entities.
	 */
	private String className;
	
	/**
	 * The field or method name, or {@code null} for system, app 
	 * or class entities
	 */
	private String memberName;

	private CfgEntity(String appName, String className, String memberName) {
		this.appName = appName;
		this.className = className;
		this.memberName = memberName;
		
		assert appName != null || (className == null && memberName == null) : "Missing appName";
		assert className != null || memberName == null : "Missing className";
	}
	
	public static CfgEntity ofApp(String appName) {
		return new CfgEntity(appName, null, null);
	}
	
	public static CfgEntity ofClass(String appName, String className) {
		return new CfgEntity(appName, className, null);
	}
	
	public static CfgEntity ofMember(String appName, String className, String memberName) {
		return new CfgEntity(appName, className, memberName);
	}
	
	public CfgEntityLevel getLevel() {
		return CfgEntityLevel.levelFor(appName, className, memberName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appName == null) ? 0 : appName.hashCode());
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime * result
				+ ((memberName == null) ? 0 : memberName.hashCode());
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
		CfgEntity other = (CfgEntity) obj;
		if (appName == null) {
			if (other.appName != null)
				return false;
		} else if (!appName.equals(other.appName))
			return false;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		if (memberName == null) {
			if (other.memberName != null)
				return false;
		} else if (!memberName.equals(other.memberName))
			return false;
		return true;
	}
}

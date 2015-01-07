package am.xmx.ui;

import am.xmx.dto.XmxClassInfo;

public class AdvancedXmxClassInfo extends XmxClassInfo {
	public AdvancedXmxClassInfo(int id, String className, int classLoaderId, String appName) {
		super(id, className, classLoaderId, appName);
	}

	/**
	 * Number of managed objects
	 */
	private Integer numberOfObjects;


	/**
	 * @return numberOfObjects (Number of managed objects)
	 */
	public Integer getNumberOfObjects() {
		return numberOfObjects;
	}

	/**
	 * @param numberOfObjects New value of Number of managed objects.
	 */
	public void setNumberOfObjects(Integer numberOfObjects) {
		this.numberOfObjects = numberOfObjects;
	}
}

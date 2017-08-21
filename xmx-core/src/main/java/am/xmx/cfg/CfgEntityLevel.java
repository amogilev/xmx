// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg;


/**
 * Level of configuration section or property - System, App, Class or Member.
 * <p/>
 * The level of section is determined by patterns which are specified in the
 * section name - Member-level has all three patterns, Class-level - only App
 * and Class, while App-level must have single App pattern.
 * <p/>
 * The level of the property is determined by the method which is used to
 * access it, like {@link IXmxConfig#getClassProperty()}
 *  
 * @author Andrey
 *
 */
public enum CfgEntityLevel {

	/** Special level - only one section */
	SYSTEM(0),
	
	APP(1),
	CLASS(2),
	MEMBER(3);
	
	private int level;

	private CfgEntityLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
	
	public static <T> CfgEntityLevel levelFor(T appSpec, T classSpec, T memberSpec) {
		if (memberSpec != null) {
			return MEMBER;
		} else if (classSpec != null) {
			return CLASS;
		} else if (appSpec != null) {
			return APP;
		} else {
			return SYSTEM;
		}
	}
}

package am.xmx.cfg.impl;

import java.util.regex.Pattern;

import am.xmx.cfg.CfgEntityLevel;

/**
 * Parsed name for App-level, Class-level or Member-level configuration section. 
 * 
 * @author Andrey Mogilev
 */
class SectionHeader {
	
	Pattern appPattern;
	Pattern classPattern;
	Pattern memberPattern;
	
	CfgEntityLevel level;
	
	String appSpec;
	String classSpec;
	String memberSpec;
	
	SectionHeader() {
	}

	SectionHeader(Pattern appPattern, Pattern classPattern,
			Pattern memberPattern) {
		this.appPattern = appPattern;
		this.classPattern = classPattern;
		this.memberPattern = memberPattern;
		
		if (appPattern == null || (classPattern == null && memberPattern != null)) {
			throw new IllegalArgumentException("Illegal SectionHeader");
		}
		
		level = CfgEntityLevel.levelFor(appPattern, classPattern, memberPattern);
	}
	
	/**
	 * Used for property matching. Property level is determined by the least non-null
	 * name, sections with lower level are considered non-matching (i.e. Member-level
	 * sections cannot contain App-level properties).
	 */
	public boolean matches(String appName, String className, String memberName) {
		return check(memberPattern, memberName) 
				&& check(classPattern, className)
				&& check(appPattern, appName);
	}
	
	/**
	 * Used for property matching in app subconfigs, where all sections are known
	 * to match the application. 
	 */
	public boolean matchesAfterApp(String className, String memberName) {
		return check(memberPattern, memberName) 
				&& check(classPattern, className);
	}

	/**
	 * Checks only app name, ignore other parts.
	 */
	public boolean appMatches(String appName) {
		return check(appPattern, appName);
	}
	
	private static boolean check(Pattern pattern, String name) {
		if (pattern == null) {
			// no check at this level
			return true;
		} else if (name == null) {
			// higher level of property than of section
			return false;
		}
		return pattern.matcher(name).matches();
	}

}

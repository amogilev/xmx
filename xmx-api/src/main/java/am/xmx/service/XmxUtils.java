package am.xmx.service;

/**
 * Utility methods which are used both in core and web components.
 */
public class XmxUtils {

	public static String safeToString(Object obj) {
		try {
			return obj == null ? "null" : obj.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}


}

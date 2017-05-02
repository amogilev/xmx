package am.xmx.core;

import org.apache.catalina.loader.WebappClassLoader;

public class Tomcat7WebappNameExtractor implements IWebappNameExtractor {
	
	@Override
	public String extract(ClassLoader cl) {
		if (cl instanceof WebappClassLoader) {
			return ((WebappClassLoader)cl).getContextName();
		}

		return null;
	}
}

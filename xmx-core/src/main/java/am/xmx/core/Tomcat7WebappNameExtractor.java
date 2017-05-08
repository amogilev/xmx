package am.xmx.core;

import org.apache.catalina.loader.WebappClassLoader;

public class Tomcat7WebappNameExtractor implements IWebappNameExtractor {
	
	@Override
	public String extract(ClassLoader cl) {
		if (cl instanceof WebappClassLoader) {
			WebappClassLoader wcl = (WebappClassLoader) cl;
			// unfortunately, Tomcat7 does not provide access to the display name through classloader
			// so, al we can use is the context name (path)
			String name = wcl.getContextName();
			if (name != null && name.startsWith("/")) {
				name = name.substring(1);
			}
			return name;
		}

		return null;
	}
}

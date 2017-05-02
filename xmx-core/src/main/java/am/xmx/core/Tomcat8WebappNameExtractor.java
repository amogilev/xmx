package am.xmx.core;

import org.apache.catalina.loader.WebappClassLoaderBase;

public class Tomcat8WebappNameExtractor implements IWebappNameExtractor {
	
	@Override
	public String extract(ClassLoader cl) {
		if (cl instanceof WebappClassLoaderBase) {
			return ((WebappClassLoaderBase)cl).getContextName();
		}

		return null;
	}
}

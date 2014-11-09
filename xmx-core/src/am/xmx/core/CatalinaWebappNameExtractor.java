package am.xmx.core;

import org.apache.catalina.loader.WebappClassLoader;

public class CatalinaWebappNameExtractor implements IWebappNameExtractor {
	
	@Override
	public String extract(Object obj) {
		ClassLoader cl = obj.getClass().getClassLoader();
		if (cl instanceof WebappClassLoader) {
			return ((WebappClassLoader)cl).getContextName();
		}

		return null;
	}
}

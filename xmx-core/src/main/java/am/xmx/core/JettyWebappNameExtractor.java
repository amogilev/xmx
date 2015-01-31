package am.xmx.core;

import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppClassLoader.Context;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyWebappNameExtractor implements IWebappNameExtractor {

	@Override
	public String extract(Object obj) {
		ClassLoader cl = obj.getClass().getClassLoader();
		if (cl instanceof WebAppClassLoader) {
			Context context = ((WebAppClassLoader)cl).getContext();
			if (context instanceof WebAppContext) {
				WebAppContext wac = (WebAppContext)context;
				String name = wac.getDisplayName();
				if (name == null || name.isEmpty()) {
					name = wac.getWar();
				}

				return name;
			}
		}

		return null;
	}
}

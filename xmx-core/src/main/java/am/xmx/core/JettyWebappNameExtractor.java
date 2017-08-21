// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.core;

import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppClassLoader.Context;
import org.eclipse.jetty.webapp.WebAppContext;

public class JettyWebappNameExtractor implements IWebappNameExtractor {

	@Override
	public String extract(ClassLoader cl) {
		if (cl instanceof WebAppClassLoader) {
			Context context = ((WebAppClassLoader)cl).getContext();
			if (context instanceof WebAppContext) {
				WebAppContext wac = (WebAppContext)context;
				String name = wac.getDisplayName();
				if (name == null || name.isEmpty()) {
					// use context path if display name is empty
					name = wac.getContextPath();
					if (name != null && name.startsWith("/")) {
						name = name.substring(1);
					}
				}
				// war name may be used too, but currently see no need in it... // name = wac.getWar();
				return name;
			}
		}

		return null;
	}
}

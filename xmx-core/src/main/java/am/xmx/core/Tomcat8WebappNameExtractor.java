// Copyright © 2017 Andrey Mogilev. All rights reserved.

package am.xmx.core;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.loader.WebappClassLoaderBase;

public class Tomcat8WebappNameExtractor implements IWebappNameExtractor {

	@Override
	public String extract(ClassLoader cl) {
		if (cl instanceof WebappClassLoaderBase) {
			WebResourceRoot rootResource = ((WebappClassLoaderBase)cl).getResources();
			if (rootResource !=  null) {
				Context context = rootResource.getContext();

				String name = context.getDisplayName();
				if (name == null || name.isEmpty()) {
					name = context.getPath();
					if (name != null && name.startsWith("/")) {
						name = name.substring(1);
					}
				}

				return name;
			}
		}

		return null;
	}
}

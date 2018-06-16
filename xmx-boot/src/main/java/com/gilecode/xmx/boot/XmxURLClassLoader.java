// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This loader is similar to its base {@link URLClassLoader}, except of the fact that the classes
 * loaded by it are never managed by XMX.
 * <br/>
 * In particular, it is used to load internal XMX classes and thus prevent infinite recursion.
 */
public class XmxURLClassLoader extends URLClassLoader {

	public XmxURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public XmxURLClassLoader(URL[] urls) {
		super(urls);
	}
}

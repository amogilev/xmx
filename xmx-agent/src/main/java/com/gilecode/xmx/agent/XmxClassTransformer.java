// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.agent;

import com.gilecode.xmx.boot.XmxProxy;
import com.gilecode.xmx.boot.XmxURLClassLoader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class XmxClassTransformer implements ClassFileTransformer {
	
	private boolean disabled = false;

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		
		if (disabled) {
			return null;
		
		}
		
		if (loader == null) {
			// skip bootstrap classes (performance reasons only, may be changed in future)
			return null;
		}
		
		if (className == null) {
			// skip anonymous (lambda) classes
			return null;
		}

		if (isXmxClass(className) || isXmxLoader(loader)) {
			// skip XMX classes from management to prevent circular class loading and showing internal services
			return null;
		}

		String loaderClassName = loader.getClass().getName();
		if (loaderClassName.equals("sun.reflect.DelegatingClassLoader")) {
			// skip auxiliary classes loaded by sun/reflect/DelegatingClassLoader, helps reducing number of processed classloaders
			return null;
		}

		try {
			byte[] transformClass = XmxProxy.transformClass(loader, className, classfileBuffer, classBeingRedefined);
			return transformClass;
		} catch (Throwable e) {
			e.printStackTrace();
			disabled = true;
			return null;
		}
	}

	private static boolean isXmxClass(String className) {
		return className.startsWith("com/gilecode") && (className.startsWith("com/gilecode/specr/") ||
				className.startsWith("com/gilecode/xmx/"));
	}

	private static boolean isXmxLoader(ClassLoader loader) {
		while (loader != null) {
			if (loader.getClass() == XmxURLClassLoader.class) {
				return true;
			}
			loader = loader.getParent();
		}
		return false;
	}
}

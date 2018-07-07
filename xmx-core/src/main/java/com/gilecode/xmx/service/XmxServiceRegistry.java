// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import com.gilecode.xmx.core.XmxLoader;
import com.gilecode.xmx.core.type.IMethodInfoService;
import com.gilecode.xmx.core.type.MethodInfoServiceImpl;

/**
 * Provides implementations of services for use in core and Web UI.
 */
public class XmxServiceRegistry {

	public static IXmxService getXmxService() {
		return XmxLoader.getServiceInstance();
	}

	private static class MethodInfoServiceHolder {
		final static IMethodInfoService serviceInstance = create();

		private static IMethodInfoService create() {
			IMethodInfoService svc = null;
			if (getMajorJavaVersion() >= 8) {
				svc = tryCreateInstance("com.gilecode.xmx.core.type.j8.Java8MethodInfoServiceImpl");
			}
			if (svc == null) {
				svc = new MethodInfoServiceImpl();
			}
			return svc;
		}
	}

	public static IMethodInfoService getMethodInfoService() {
		return MethodInfoServiceHolder.serviceInstance;
	}

	private static class MapperServiceHolder {
		final static IMapperService serviceInstance = new MapperService();
	}

	public static IMapperService getMapperService() {
		return MapperServiceHolder.serviceInstance;
	}

	@SuppressWarnings("unchecked")
	private static <T> T tryCreateInstance(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			return (T)clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	static int getMajorJavaVersion() {
		String[] parts = System.getProperty("java.version").split("[._]");
		int firstVer = Integer.parseInt(parts[0]);
		if (firstVer == 1 && parts.length > 1) {
			return Integer.parseInt(parts[1]);
		} else {
			return firstVer;
		}
	}
}

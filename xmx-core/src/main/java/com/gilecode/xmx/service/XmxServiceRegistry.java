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
		final static IMethodInfoService serviceInstance = new MethodInfoServiceImpl();
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
}

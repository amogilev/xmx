// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import com.gilecode.xmx.core.XmxLoader;
import com.gilecode.xmx.core.params.IParamNamesFetcher;
import com.gilecode.xmx.core.params.ParamNamesFetcher;
import com.gilecode.xmx.core.type.IMethodInfoService;
import com.gilecode.xmx.core.type.MethodInfoServiceImpl;
import com.gilecode.xmx.spring.IXmxSpringService;

/**
 * Provides implementations of services for use in core and Web UI.
 */
public class XmxServiceRegistry {

	private final static int majorJavaVersion = obtainMajorJavaVersion();

	static int obtainMajorJavaVersion() {
		String[] parts = System.getProperty("java.version").split("[._]");
		int firstVer = Integer.parseInt(parts[0]);
		if (firstVer == 1 && parts.length > 1) {
			return Integer.parseInt(parts[1]);
		} else {
			return firstVer;
		}
	}

	public static int getMajorJavaVersion() {
		return majorJavaVersion;
	}

	public static IXmxService getXmxService() {
		return XmxLoader.getServiceInstance();
	}

	public static IXmxSpringService getXmxSpringService() {
		return XmxLoader.getServiceInstance().findPlugin(IXmxSpringService.class);
	}

	private static class ParamNamesFetcherHolder {
		final static IParamNamesFetcher serviceInstance = new ParamNamesFetcher(XmxLoader.getServiceInstance());
	}

	public static IParamNamesFetcher getParamNamesFetcher() {
		return ParamNamesFetcherHolder.serviceInstance;
	}

	private static class MethodInfoServiceHolder {
		final static IMethodInfoService serviceInstance = new MethodInfoServiceImpl(getParamNamesFetcher());
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

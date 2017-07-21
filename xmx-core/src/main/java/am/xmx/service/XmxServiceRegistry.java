package am.xmx.service;

import am.xmx.core.XmxLoader;

/**
 * Provides implementations of services for use in core and Web UI.
 */
public class XmxServiceRegistry {

	public static IXmxService getXmxService() {
		return XmxLoader.getServiceInstance();
	}

	private static class MapperServiceHolder {
		final static IMapperService serviceInstance = new MapperService();
	}

	public static IMapperService getMapperService() {
		return MapperServiceHolder.serviceInstance;
	}
}

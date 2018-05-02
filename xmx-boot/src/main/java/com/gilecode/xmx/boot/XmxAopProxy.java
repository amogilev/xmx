// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

import java.util.Map;

public class XmxAopProxy {

	private static IXmxAopService aopService;
	static void setAopService(IXmxAopService aopService) {
		XmxAopProxy.aopService = aopService;
	}

	public static Map<Class<?>, Object> before(int joinPointId, Object thisArg, Object[] interestedArgs) {
		if (aopService != null) {
			return aopService.before(joinPointId, thisArg, interestedArgs);
		}
		return null;
	}

	public static Object afterReturn(Object retVal, int joinPointId, Map<Class<?>, Object> adviceInstances,
	                                 Object thisArg, Object[] interestedArgs) {
		if (aopService != null) {
			return aopService.afterReturn(joinPointId, adviceInstances, thisArg, interestedArgs, retVal);
		}
		return retVal;
	}

	public static void afterThrow(Throwable ex, int joinPointId, Map<Class<?>, Object> adviceInstances,
	                              Object thisArg, Object[] interestedArgs) {
		if (aopService != null) {
			aopService.afterThrow(joinPointId, adviceInstances, thisArg, interestedArgs, ex);
		}
	}
}

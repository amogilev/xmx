// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

import java.util.Map;

public interface IXmxAopService {

    Map<Class<?>, Object> before(int joinPointId, Object thisArg, Object[] interestedArgs);

    Object afterReturn(int joinPointId, Map<Class<?>, Object> adviceInstances,
                       Object thisArg, Object[] interestedArgs, Object retVal);

	void afterThrow(int joinPointId, Map<Class<?>, Object> adviceInstances,
	                Object thisArg, Object[] interestedArgs, Throwable ex);
}

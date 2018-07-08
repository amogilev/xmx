// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.params;

import java.lang.reflect.Method;

public interface IParamNamesFetcher {

	/**
	 * Return parameter names, if known, or {@code null} otherwise
	 */
	String[] getMethodParameterNames(Method m);


}

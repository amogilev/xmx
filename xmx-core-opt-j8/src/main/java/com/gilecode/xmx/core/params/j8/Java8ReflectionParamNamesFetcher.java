// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.params.j8;

import com.gilecode.xmx.core.params.IParamNamesFetcher;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Parameter names fetcher which uses Java8-based Reflection parameters information.
 */
@SuppressWarnings("unused")
public class Java8ReflectionParamNamesFetcher implements IParamNamesFetcher {

	@Override
	public String[] getMethodParameterNames(Method m) {
		Parameter[] params = m.getParameters();
		String[] ret = new String[params.length];
		for (int j = 0; j < params.length; j++) {
			Parameter p = params[j];
			if (!p.isNamePresent()) {
				return null;
			}
			ret[j] = p.getName();
		}
		return ret;
	}
}

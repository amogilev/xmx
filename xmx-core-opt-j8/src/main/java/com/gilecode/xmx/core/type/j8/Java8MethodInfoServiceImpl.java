// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.type.j8;

import com.gilecode.xmx.core.type.MethodInfoServiceImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Adds Java8-based parameters name extraction to basic method parameters information.
 */
public class Java8MethodInfoServiceImpl extends MethodInfoServiceImpl {

	@Override
	public String[] getMethodParameters(Method m) {
		Parameter[] params = m.getParameters();
		String[] ret = new String[params.length];
		for (int j = 0; j < params.length; j++) {
			Parameter p = params[j];
			ret[j] = p.getType().getSimpleName() + ' ' + p.getName();
		}
		return ret;
	}
}

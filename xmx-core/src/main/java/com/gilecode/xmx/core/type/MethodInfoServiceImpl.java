// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.type;

import com.gilecode.xmx.core.params.IParamNamesFetcher;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodInfoServiceImpl implements IMethodInfoService {

	private final IParamNamesFetcher paramNamesFetcher;

	public MethodInfoServiceImpl(IParamNamesFetcher paramNamesFetcher) {
		this.paramNamesFetcher = paramNamesFetcher;
	}

	@Override
	public String getMethodNameTypeSignature(Method m) {
		StringBuilder sb = new StringBuilder();
		int mod = m.getModifiers() & Modifier.methodModifiers();
		if (mod != 0) {
			sb.append(Modifier.toString(mod)).append(' ');
		}
		sb.append(m.getReturnType().getName());
		sb.append(' ').append(m.getName());
		return sb.toString();
	}

	@Override
	public String[] getMethodParameterDescriptions(Method m) {
		Class<?>[] params = m.getParameterTypes();
		String[] paramNames = paramNamesFetcher.getMethodParameterNames(m);
		String[] ret = new String[params.length];
		for (int j = 0; j < params.length; j++) {
			String desc = params[j].getSimpleName();
			if (paramNames != null && j < paramNames.length) {
				desc += ' ' + paramNames[j];
			}
			ret[j] = desc;
		}
		return ret;
	}
}

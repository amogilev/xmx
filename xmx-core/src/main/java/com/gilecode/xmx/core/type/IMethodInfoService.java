// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.type;

import java.lang.reflect.Method;

public interface IMethodInfoService {

	/**
	 * Returns partial signature of method, which includes qualifiers, return type and method name.
	 */
	String getMethodNameTypeSignature(Method m);

	/**
	 * Returns the textual descriptions of the method parameters, which consist of the (short) type name, followed
	 * by an optional parameter name (if real parameter name is not known, it is skipped).
	 */
	String[] getMethodParameterDescriptions(Method m);

	/**
	 * Return parameter names, if known, or {@code null} otherwise
	 */
	String[] getMethodParameterNames(Method m);

}

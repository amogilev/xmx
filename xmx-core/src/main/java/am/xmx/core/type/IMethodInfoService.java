// Copyright © 2017 Andrey Mogilev. All rights reserved.

package am.xmx.core.type;

import java.lang.reflect.Method;

public interface IMethodInfoService {

	/**
	 * Returns partial signature of method, which includes qualifiers, return type and method name.
	 */
	String getMethodNameTypeSignature(Method m);

	String[] getMethodParameters(Method m);

}

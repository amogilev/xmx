// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

public enum AdviceKind {

	/**
	 * Invoked at the very start of the method invocation. Optionally allows to replace the intercepted method
	 * arguments, if the corresponding advice parameters are declared as an array type which component type is
	 * the original argument type.
	 */
	BEFORE,

	/**
	 * Invoked on a normal exit from the method using 'return'. Not invoked on throws!
	 * Optionally allows to override the return value.
	 */
	AFTER_RETURN,

	/**
	 * Invoked on an abrupt method exit by an explicit or implicit throw.
	 */
	AFTER_THROW;
}

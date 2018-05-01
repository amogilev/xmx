// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Specifies which parameter of the intercepted method is mapped to the annotated parameter of the advice method.
 * <p/>
 * NOTE: the types of the corresponding parameters must be either equal, or the advice parameter shall ne of type
 * {@link Object}. Otherwise, the advice will not be mapped to the managed method.
 *
 * TODO: in future, support arg to be array of matching type with a single replaceable element.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Argument {

	/**
	 * The index of the explicit parameter of the intercepted method, starting from 0.
	 * <p/>
	 * Note that the implicit 'this' parameter is not counted, as it shall be mapped using {@link This @This} annotation.
	 */
	int value();
}

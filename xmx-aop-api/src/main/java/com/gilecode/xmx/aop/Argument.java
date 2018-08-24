// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Specifies which argument of the intercepted method is mapped to the annotated parameter of the advice method,
 * by specifying its index (starting from 0).
 *
 * <p/>
 * NOTE: the type of the annotated parameter shall be either {@code Object}, or equal to the corresponding parameter
 * of the target method.
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

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Specifies that this advice parameter expects to get an array with all explicit arguments of the intercepted method
 * (i.e. implicit 'this' is not included).
 *
 * <p/>
 * NOTE: the type of the annotated parameter shall be either {@code Object}, or equal to the corresponding parameter
 * of the target method.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AllArguments {

	/**
	 * If explicitly set to {@code true}, the advice method can modify the intercepted arguments by storing the new
	 * values to the arguments array. Otherwise, all changes are ignored.
	 */
	boolean modifiable() default false;
}

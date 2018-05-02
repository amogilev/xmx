// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * This annotation allows to replace the original argument of the target method with another value.
 * <p/>
 * The argument is passed to the advice as one-element array with the component type either @code Object}, or equal to
 * the corresponding parameter of the target method. The original value is passed as the first element of that array.
 * If needed, a new value may be stored to that array element; in that case, the argument is repplaced to the new one.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModifiableArgument {

	/**
	 * The index of the explicit parameter of the intercepted method, starting from 0.
	 * <p/>
	 * Note that the implicit 'this' parameter is not counted, as it shall be mapped using {@link This @This} annotation.
	 */
	int value();
}

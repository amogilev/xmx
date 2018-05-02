// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Specifies that this advice parameter holds the {@link Throwable} thrown from the intercepted method.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Thrown {
}

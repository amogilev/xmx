// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Specifies that the return value of the intercepted method shall be replaced with the return value of the advice.
 * <p/>
 * The type of the return values must either be equal, or the advice type shall be {@link Object} at compile time, and
 * be compatible at runtime.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OverrideRetVal {
}

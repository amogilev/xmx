// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Used to pass a target {@link java.lang.reflect.Method} as an advice argument.
 * <br/>
 * The advice parameter must have the type {@link java.lang.reflect.Method}.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetMethod {
}

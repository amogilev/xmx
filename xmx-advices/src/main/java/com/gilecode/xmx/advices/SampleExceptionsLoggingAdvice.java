// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.*;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A sample advice which logs uncaught exceptions thrown from advised methods to console.
 */
public class SampleExceptionsLoggingAdvice {

	@Advice(AdviceKind.AFTER_THROW)
	public static void afterThrow(@AllArguments Object[] args, @Thrown Throwable ex, @TargetMethod Method target) {
		System.err.println("WARNING: exception " + ex.getClass() +
				" is thrown from " + target + ", ARGS=" + Arrays.toString(args));
		ex.printStackTrace(System.err);
	}
}

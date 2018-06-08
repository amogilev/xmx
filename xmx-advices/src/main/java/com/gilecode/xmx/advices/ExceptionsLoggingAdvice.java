// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.advices;

import com.gilecode.xmx.aop.Advice;
import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.AllArguments;
import com.gilecode.xmx.aop.Thrown;

import java.util.Arrays;

/**
 * A sample advice which logs uncaught exceptions thrown from advised methods.
 */
public class ExceptionsLoggingAdvice {

	@Advice(AdviceKind.AFTER_THROW)
	public static void afterThrow(@AllArguments Object[] args, @Thrown Throwable ex) {
		// TODO: need to add objectId to proxy & @TargetObjectId & @TargetMethod. Obtaining theinfo from StackTrace
		//       is inefficient
		String methodName = "", className="";
		StackTraceElement[] stackTrace = ex.getStackTrace();
		if (stackTrace.length > 0) {
			StackTraceElement first = stackTrace[0];
			methodName = first.getMethodName();
			className = first.getClassName();
		}

		// TODO: pass through proxy to core manager with actual logger
		System.err.println("WARNING: exception " + ex.getClass() + " is thrown from " + className +
				"." + methodName + "(), ARGS=" + Arrays.toString(args));
	}
}

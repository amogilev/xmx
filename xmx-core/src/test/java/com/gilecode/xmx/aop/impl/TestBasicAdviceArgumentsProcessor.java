// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class TestBasicAdviceArgumentsProcessor extends BasicAdviceArgumentsProcessor {

	@OverrideRetVal int sampleMethod(@Argument(0) int arg0, @ModifiableArgument(1) int arg1, @This int arg2,
	                  @RetVal int arg3, @Thrown int arg4, @AllArguments int arg5,
	                  @Deprecated @Argument(6) @SuppressWarnings("foo") int arg6,
	                  @TargetMethod Method arg7) {
		return 0;
	}

	@Test
	public void testGetAdviceArgumentKind() {
		Method m = AopTestUtils.findMethod(TestBasicAdviceArgumentsProcessor.class, "sampleMethod");
		Annotation[][] annotations = m.getParameterAnnotations();

		assertEquals(AdviceArgument.Kind.ARGUMENT, getAdviceArgumentKind(annotations[0][0]));
		assertEquals(AdviceArgument.Kind.ARGUMENT, getAdviceArgumentKind(annotations[1][0]));
		assertEquals(AdviceArgument.Kind.THIS, getAdviceArgumentKind(annotations[2][0]));
		assertEquals(AdviceArgument.Kind.RETVAL, getAdviceArgumentKind(annotations[3][0]));
		assertEquals(AdviceArgument.Kind.THROWN, getAdviceArgumentKind(annotations[4][0]));
		assertEquals(AdviceArgument.Kind.ALL_ARGUMENTS, getAdviceArgumentKind(annotations[5][0]));
		assertEquals(AdviceArgument.Kind.TARGET, getAdviceArgumentKind(annotations[7][0]));
	}

	@Test
	public void testFindArgumentAnnotation() {
		Method m = AopTestUtils.findMethod(TestBasicAdviceArgumentsProcessor.class, "sampleMethod");
		Annotation[][] annotations = m.getParameterAnnotations();

		assertEquals(annotations[0][0], findArgumentAnnotation(annotations[0]));
		assertEquals(annotations[1][0], findArgumentAnnotation(annotations[1]));
		assertEquals(annotations[2][0], findArgumentAnnotation(annotations[2]));
		assertEquals(annotations[3][0], findArgumentAnnotation(annotations[3]));
		assertEquals(annotations[4][0], findArgumentAnnotation(annotations[4]));
		assertEquals(annotations[5][0], findArgumentAnnotation(annotations[5]));
		assertEquals(annotations[7][0], findArgumentAnnotation(annotations[7]));

		Annotation found = findArgumentAnnotation(annotations[6]);
		assertTrue(found instanceof Argument);
		assertEquals(annotations[6][1], found);
	}

	@Test
	public void testGetArgumentIdx() {
		Method m = AopTestUtils.findMethod(TestBasicAdviceArgumentsProcessor.class, "sampleMethod");
		Annotation[][] annotations = m.getParameterAnnotations();

		assertEquals(0, getArgumentIdx(annotations[0][0]));
		assertEquals(1, getArgumentIdx(annotations[1][0]));
		for (int i = 2; i < 7; i++) {
			try {
				getArgumentIdx(annotations[i][0]);
				fail("Exception expected");
			} catch (IllegalArgumentException e) {
				// expected
			}
		}
	}
}

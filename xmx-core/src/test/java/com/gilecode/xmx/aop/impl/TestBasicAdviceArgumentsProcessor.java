// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.aop.data.AnnotatedTypeInfo;
import com.gilecode.xmx.aop.data.AnnotationInfo;
import com.gilecode.xmx.aop.data.MethodDeclarationInfo;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
	public void testFindArgumentAnnotation() throws IOException {
		MethodDeclarationInfo m = AopTestUtils.findMethodDeclaration(TestBasicAdviceArgumentsProcessor.class, "sampleMethod");
		AnnotatedTypeInfo[] params = m.getParameters();

		assertEquals(firstAnno(params[0]), findArgumentAnnotation(params[0]));
		assertEquals(firstAnno(params[1]), findArgumentAnnotation(params[1]));
		assertEquals(firstAnno(params[2]), findArgumentAnnotation(params[2]));
		assertEquals(firstAnno(params[3]), findArgumentAnnotation(params[3]));
		assertEquals(firstAnno(params[4]), findArgumentAnnotation(params[4]));
		assertEquals(firstAnno(params[5]), findArgumentAnnotation(params[5]));
		assertEquals(firstAnno(params[6]), findArgumentAnnotation(params[6]));
		assertEquals(firstAnno(params[7]), findArgumentAnnotation(params[7]));
	}

	public AnnotationInfo firstAnno(AnnotatedTypeInfo param) {
		return getAnno(param, 0);
	}

	public AnnotationInfo getAnno(AnnotatedTypeInfo param, int idx) {
		return param.getAnnotations().get(idx);
	}

	@Test
	public void testGetArgumentIdx() throws IOException {
		MethodDeclarationInfo m = AopTestUtils.findMethodDeclaration(TestBasicAdviceArgumentsProcessor.class, "sampleMethod");
		AnnotatedTypeInfo[] params = m.getParameters();


		assertEquals(0, getArgumentIdx(firstAnno(params[0])));
		assertEquals(1, getArgumentIdx(firstAnno(params[1])));
		assertEquals(6, getArgumentIdx(firstAnno(params[6])));
		for (int i = 2; i < 7; i++) {
			if (i != 6) {
				try {
					getArgumentIdx(firstAnno(params[i]));
					fail("Exception expected");
				} catch (IllegalArgumentException e) {
					// expected
				}
			}
		}
	}
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.aop.data.AnnotatedTypeInfo;
import com.gilecode.xmx.aop.data.AnnotationInfo;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains basic shared methods to work with advice arguments
 */
class BasicAdviceArgumentsProcessor {

	protected static Set<Class<? extends Annotation>> knownArgAnnotations = new HashSet<>(Arrays.asList(
			Argument.class, ModifiableArgument.class, AllArguments.class,
			This.class, RetVal.class, Thrown.class, TargetMethod.class));

	protected static AdviceArgument.Kind getAdviceArgumentKind(Annotation argAnnotation) {
		if (argAnnotation instanceof This) {
			return AdviceArgument.Kind.THIS;
		} else if (argAnnotation instanceof AllArguments) {
			return AdviceArgument.Kind.ALL_ARGUMENTS;
		} else if (argAnnotation instanceof RetVal) {
			return AdviceArgument.Kind.RETVAL;
		} else if (argAnnotation instanceof Thrown) {
			return AdviceArgument.Kind.THROWN;
		} else if (argAnnotation instanceof TargetMethod) {
			return AdviceArgument.Kind.TARGET;
		} else if (argAnnotation instanceof Argument || argAnnotation instanceof ModifiableArgument) {
			return AdviceArgument.Kind.ARGUMENT;
		} else {
			throw new IllegalArgumentException("Unknown argument annotation: " + argAnnotation);
		}
	}

	protected static AdviceArgument.Kind getAdviceArgumentKind(AnnotationInfo argAnnotation) {
		Class<? extends Annotation> aClass = argAnnotation.getAnnotationClass();
		if (aClass == This.class) {
			return AdviceArgument.Kind.THIS;
		} else if (aClass == AllArguments.class) {
			return AdviceArgument.Kind.ALL_ARGUMENTS;
		} else if (aClass == RetVal.class) {
			return AdviceArgument.Kind.RETVAL;
		} else if (aClass == Thrown.class) {
			return AdviceArgument.Kind.THROWN;
		} else if (aClass == TargetMethod.class) {
			return AdviceArgument.Kind.TARGET;
		} else if (aClass == Argument.class || aClass == ModifiableArgument.class) {
			return AdviceArgument.Kind.ARGUMENT;
		} else {
			throw new IllegalArgumentException("Unknown argument annotation: " + argAnnotation);
		}
	}

	protected static AnnotationInfo findArgumentAnnotation(AnnotatedTypeInfo annotatedTypeInfo) {
		for (AnnotationInfo annotation : annotatedTypeInfo.getAnnotations()) {
			Class<? extends Annotation> annotationType = annotation.getAnnotationClass();
			if (knownArgAnnotations.contains(annotationType)) {
				return annotation;
			}
		}
		return null;
	}

	protected static int getArgumentIdx(AnnotationInfo annotation) {
		if (annotation.getAnnotationClass() == Argument.class
				|| annotation.getAnnotationClass() == ModifiableArgument.class) {
			return annotation.value();
		}
		throw new IllegalArgumentException();
	}
}

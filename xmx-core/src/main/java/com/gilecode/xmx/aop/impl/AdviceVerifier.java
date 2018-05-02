// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Provides preliminary general verification of advice classes. Checks that there is at least one advice method,
 * all parameters are correctly annotated with one of known AOP annotations etc.
 * <p/>
 * Even a successful verification does not guarantee that the advice can be applied to a given target method, as
 * we cannot check number of parameters, types compatibility etc.
 */
public class AdviceVerifier extends BasicAdviceArgumentsProcessor {

	private final static Logger logger = LoggerFactory.getLogger(AdviceVerifier.class);

	private Map<AdviceKind, Set<Class<? extends Annotation>>> allowedArgAnnotationsByKind =
			new EnumMap<>(AdviceKind.class);
	{
		allowedArgAnnotationsByKind.put(AdviceKind.BEFORE, new HashSet<>(Arrays.asList(
				Argument.class, ModifiableArgument.class, AllArguments.class, This.class)));
		allowedArgAnnotationsByKind.put(AdviceKind.AFTER_RETURN, new HashSet<>(Arrays.asList(
				Argument.class, AllArguments.class, This.class, RetVal.class)));
		allowedArgAnnotationsByKind.put(AdviceKind.AFTER_THROW, new HashSet<>(Arrays.asList(
				Argument.class, AllArguments.class, This.class, Thrown.class)));
	}

	/**
	 * Verifies that a given advice class has at least one advice method, and all advice methods could be potentially
	 * applied at least to some target methods, i.e. that all parameters are correctly annotated.
	 *
	 * @throws BadAdviceException if any of verification problems is found
	 */
	public void verifyAdviceClass(Class<?> c) throws BadAdviceException {
		for (Method advice : c.getDeclaredMethods()) {
			Advice adviceAnnotation = advice.getAnnotation(Advice.class);
			if (adviceAnnotation != null) {
				AdviceKind adviceKind = adviceAnnotation.value();
				Set<Class<? extends Annotation>> allowedArgAnnotations = allowedArgAnnotationsByKind.get(adviceKind);

				Annotation[][] parametersAnnotations = advice.getParameterAnnotations();
				Class<?>[] parameterTypes = advice.getParameterTypes();
				for (int paramIdx = 0; paramIdx < parametersAnnotations.length; paramIdx++) {
					Annotation[] parameterAnnotations = parametersAnnotations[paramIdx];
					Class<?> parameterType = parameterTypes[paramIdx];
					Annotation foundArgAnnotation = null;
					for (Annotation annotation : parameterAnnotations) {
						Class<? extends Annotation> annotationClass = annotation.annotationType();
						if (knownArgAnnotations.contains(annotationClass)) {
							if (!allowedArgAnnotations.contains(annotationClass) ||
									(annotation instanceof AllArguments && adviceKind != AdviceKind.BEFORE &&
											((AllArguments) annotation).modifiable())) {
								throw new BadAdviceException(advice, annotation, "is not allowed for advice kind " + adviceKind);
							}
							if (annotation instanceof AllArguments && !parameterType.equals(Object[].class)) {
								throw new BadAdviceException(advice, annotation, "requires Object[] type");
							}
							if (annotation instanceof Argument || annotation instanceof ModifiableArgument) {
								int idx = getArgumentIdx(annotation);
								if (idx < 0 || idx >= 255) {
									throw new BadAdviceException(advice, annotation, "has invalid parameter index " + idx);
								}
								if (annotation instanceof ModifiableArgument && !parameterType.isArray()) {
									throw new BadAdviceException(advice, annotation, "requires array type");
								}
							}
							if (annotation instanceof Thrown && !parameterType.equals(Throwable.class)) {
								throw new BadAdviceException(advice, annotation, "requires java.lang.Throwable type");
							}
							if (foundArgAnnotation != null) {
								throw new BadAdviceException(advice, annotation, "overwrites annotation " + foundArgAnnotation);
							}
							foundArgAnnotation = annotation;
						}
					}
					if (foundArgAnnotation == null) {
						throw new BadAdviceException(advice, null, "no argument annotations found for parameter " + paramIdx);
					}
				}

				OverrideRetVal annotation = advice.getAnnotation(OverrideRetVal.class);
				if (annotation != null) {
					if (adviceKind != AdviceKind.AFTER_RETURN) {
						throw new BadAdviceException(advice, annotation, "is not allowed for advice kind " + adviceKind);
					}
					if (advice.getReturnType().equals(void.class)) {
						throw new BadAdviceException(advice, annotation, "is not allowed for void methods");
					}
				}
			}
		}
	}

	/**
	 * Checks whether the advice method is compatible (by types) to the specified target method. Log DEBUG-level
	 * message if the advice is not compatible.
	 * <p/>
	 * <strong>NOTE:</strong> The advice class MUST be verified with {@link #verifyAdviceClass(Class)} before
	 * this compatibility check!
	 */
	public boolean isAdviceCompatibleMethod(Method advice,
	                                        Type[] targetParamTypes, Type targetReturnType,
	                                        String targetClassName, String targetMethodName) {
		Annotation[][] parametersAnnotations = advice.getParameterAnnotations();
		Class<?>[] parameterTypes = advice.getParameterTypes();
		for (int paramIdx = 0; paramIdx < parametersAnnotations.length; paramIdx++) {
			Annotation[] parameterAnnotations = parametersAnnotations[paramIdx];
			Class<?> parameterType = parameterTypes[paramIdx];
			Annotation argAnnotation = findArgumentAnnotation(parameterAnnotations);
			Class<?> adviceParamType = null;
			Type targetType = null;
			boolean isModifiable = false;
			if (argAnnotation instanceof Argument || argAnnotation instanceof ModifiableArgument) {
				int argumentIdx = getArgumentIdx(argAnnotation);
				if (argumentIdx >= targetParamTypes.length) {
					logger.debug("Advice method {} cannot be applied to {}.{} as it has not enough arguments",
							advice, targetClassName, targetMethodName);
					return false;
				}
				if (argAnnotation instanceof ModifiableArgument) {
					isModifiable = true;
					adviceParamType = parameterType.getComponentType();
				} else {
					adviceParamType = parameterType;
				}
				targetType = targetParamTypes[argumentIdx];
			} else if (argAnnotation instanceof RetVal) {
				adviceParamType = parameterType;
				targetType = targetReturnType;
			}
			if (adviceParamType != null && !adviceParamType.equals(Object.class) &&
					!Type.getType(adviceParamType).equals(targetType) &&
					(isModifiable || !isBoxedTypeFor(targetType, adviceParamType))) {
				logger.debug("Advice method {} cannot be applied to {}.{} as the type of the parameter {} is " +
								"not compatible with the target",
						advice, targetClassName, targetMethodName, paramIdx);
				return false;
			}
		}
		// check @OverrideRetVal if present
		OverrideRetVal annotation = advice.getAnnotation(OverrideRetVal.class);
		if (annotation != null && !advice.getReturnType().equals(Object.class) &&
				!Type.getType(advice.getReturnType()).equals(targetReturnType)) {
			logger.debug("Advice method {} cannot be applied to {}.{} as its @OverrideRetVal return type is " +
							"not compatible with the target",
					advice, targetClassName, targetMethodName);
			return false;
		}

		return true;
	}

	private boolean isBoxedTypeFor(Type primType, Class<?> boxedType) {
		return boxedType == getBoxedTypeFor(primType);
	}

	private Class<?> getBoxedTypeFor(Type primType) {
		switch (primType.getSort()) {
		case Type.BOOLEAN:
			return Boolean.class;
		case Type.CHAR:
			return Character.class;
		case Type.BYTE:
			return Byte.class;
		case Type.SHORT:
			return Short.class;
		case Type.INT:
			return Integer.class;
		case Type.FLOAT:
			return Float.class;
		case Type.LONG:
			return Long.class;
		case Type.DOUBLE:
			return Double.class;
		}
		return null;
	}
}
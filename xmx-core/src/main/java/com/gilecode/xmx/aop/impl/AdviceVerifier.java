// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.aop.data.AnnotatedTypeInfo;
import com.gilecode.xmx.aop.data.AnnotationInfo;
import com.gilecode.xmx.aop.data.MethodDeclarationInfo;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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
				Argument.class, ModifiableArgument.class, AllArguments.class, TargetMethod.class, This.class)));
		allowedArgAnnotationsByKind.put(AdviceKind.AFTER_RETURN, new HashSet<>(Arrays.asList(
				Argument.class, AllArguments.class, TargetMethod.class, This.class, RetVal.class)));
		allowedArgAnnotationsByKind.put(AdviceKind.AFTER_THROW, new HashSet<>(Arrays.asList(
				Argument.class, AllArguments.class, TargetMethod.class, This.class, Thrown.class)));
	}

	private static final Type OBJECT_TYPE = Type.getType(Object.class);
	private static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);
	private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);
	private static final Type METHOD_TYPE = Type.getType(Method.class);

	// "boxed" object types for primitive types
	private static final Type BYTE_OBJTYPE = Type.getType(Byte.class);
	private static final Type BOOLEAN_OBJTYPE = Type.getType(Boolean.class);
	private static final Type SHORT_OBJTYPE = Type.getType(Short.class);
	private static final Type CHARACTER_OBJTYPE = Type.getType(Character.class);
	private static final Type INTEGER_OBJTYPE = Type.getType(Integer.class);
	private static final Type FLOAT_OBJTYPE = Type.getType(Float.class);
	private static final Type LONG_OBJTYPE = Type.getType(Long.class);
	private static final Type DOUBLE_OBJTYPE = Type.getType(Double.class);

	/**
	 * Verifies that a given class (represented by InputSream) contains at least one advice method,
	 * and all advice methods could be potentially applied at least to some target methods, i.e.
	 * that all parameters are correctly annotated.
	 *
	 * @param classAsStream the input stream which contain the class byte code
	 * @param classDesc the descriptor of the class (jar plus class names)
	 * @return the methods declared in the class verified to be advice candidates
	 *
	 * @throws BadAdviceException if any of verification problems is found
	 */
	public List<MethodDeclarationInfo> verifyAdviceClass(InputStream classAsStream, String classDesc) throws BadAdviceException, IOException {
		List<MethodDeclarationInfo> declaredMethods = MethodDeclarationAsmReader.readMethodDeclarations(classAsStream, classDesc);

		List<MethodDeclarationInfo> adviceMethods = new ArrayList<>();
		for (MethodDeclarationInfo adviceCandidate : declaredMethods) {
			AnnotationInfo adviceAnnotation = adviceCandidate.getAnnotation(Advice.class);
			if (adviceAnnotation != null) {
				AdviceKind adviceKind = adviceAnnotation.value();
				Set<Class<? extends Annotation>> allowedArgAnnotations = allowedArgAnnotationsByKind.get(adviceKind);

				AnnotatedTypeInfo[] parameters = adviceCandidate.getParameters();
				for (int paramIdx = 0; paramIdx < parameters.length; paramIdx++) {
					AnnotatedTypeInfo parameter = parameters[paramIdx];
					AnnotationInfo foundArgAnnotation = null;
					for (AnnotationInfo annotation : parameter.getAnnotations()) {
						Class<? extends Annotation> annotationClass = annotation.getAnnotationClass();
						Type parameterType = parameter.getType();
						if (knownArgAnnotations.contains(annotationClass)) {
							if (!allowedArgAnnotations.contains(annotationClass) ||
									(annotationClass == AllArguments.class && adviceKind != AdviceKind.BEFORE &&
											annotation.isFlagSet("modifiable"))) {
								throw new BadAdviceException(adviceCandidate, annotation, "is not allowed for advice kind " + adviceKind);
							}
							if (annotationClass == AllArguments.class && !parameterType.equals(OBJECT_ARRAY_TYPE)) {
								throw new BadAdviceException(adviceCandidate, annotation, "requires Object[] type");
							}
							if (annotationClass == Argument.class || annotationClass == ModifiableArgument.class) {
								int idx = getArgumentIdx(annotation);
								if (idx < 0 || idx >= 255) {
									throw new BadAdviceException(adviceCandidate, annotation, "has invalid parameter index " + idx);
								}
								if (annotationClass == ModifiableArgument.class && parameterType.getSort() != Type.ARRAY) {
									throw new BadAdviceException(adviceCandidate, annotation, "requires array type");
								}
							}
							if (annotationClass ==  Thrown.class && !parameterType.equals(THROWABLE_TYPE)) {
								throw new BadAdviceException(adviceCandidate, annotation, "requires java.lang.Throwable type");
							}
							if (annotationClass == TargetMethod.class && !parameterType.equals(METHOD_TYPE)) {
								throw new BadAdviceException(adviceCandidate, annotation, "requires java.lang.reflect.Method type");
							}
							if (foundArgAnnotation != null) {
								throw new BadAdviceException(adviceCandidate, annotation, "overwrites annotation " + foundArgAnnotation);
							}
							foundArgAnnotation = annotation;
						}
					}
					if (foundArgAnnotation == null) {
						throw new BadAdviceException(adviceCandidate, null, "no argument annotations found for parameter " + paramIdx);
					}
				}

				AnnotationInfo annotation = adviceCandidate.getAnnotation(OverrideRetVal.class);
				if (annotation != null) {
					if (adviceKind != AdviceKind.AFTER_RETURN) {
						throw new BadAdviceException(adviceCandidate, annotation, "is not allowed for advice kind " + adviceKind);
					}
					if (adviceCandidate.getReturnType().getSort() == Type.VOID) {
						throw new BadAdviceException(adviceCandidate, annotation, "is not allowed for void methods");
					}
				}
				// verified
				adviceMethods.add(adviceCandidate);
			}
		}
		return adviceMethods;
	}

	private static Type getArrayComponentType(Type arrType) {
		assert arrType.getSort() == Type.ARRAY : "Array type is expected";
		// NOTE: arrType.getElementType() does not work here, as it removes all array dimensions, not just one
		return Type.getType(arrType.getDescriptor().substring(1));
	}

	/**
	 * Checks whether the advice method is compatible (by types) to the specified target method. Log DEBUG-level
	 * message if the advice is not compatible.
	 * <p/>
	 * <strong>NOTE:</strong> The advice class MUST be verified with {@link #verifyAdviceClass(InputStream, String)} before
	 * this compatibility check!
	 */
	public boolean isAdviceCompatibleMethod(MethodDeclarationInfo advice,
	                                        Type[] targetParamTypes, Type targetReturnType,
	                                        String targetClassName, String targetMethodName) {
		AnnotatedTypeInfo[] parameters = advice.getParameters();
		for (int paramIdx = 0; paramIdx < parameters.length; paramIdx++) {
			AnnotatedTypeInfo parameter = parameters[paramIdx];
			Type parameterType = parameter.getType();
			AnnotationInfo argAnnotation = findArgumentAnnotation(parameter);
			assert argAnnotation != null : "Bad advice: missing arg annotation";

			Class<? extends Annotation> annoClass = argAnnotation.getAnnotationClass();

			Type adviceParamType = null;
			Type targetType = null;
			boolean isModifiable = false;
			if (annoClass == Argument.class || annoClass == ModifiableArgument.class) {
				int argumentIdx = getArgumentIdx(argAnnotation);
				if (argumentIdx >= targetParamTypes.length) {
					logger.debug("Advice method {} cannot be applied to {}.{} as it has not enough arguments",
							advice, targetClassName, targetMethodName);
					return false;
				}
				if (annoClass == ModifiableArgument.class) {
					isModifiable = true;
					adviceParamType = getArrayComponentType(parameterType);
				} else {
					adviceParamType = parameterType;
				}
				targetType = targetParamTypes[argumentIdx];
			} else if (annoClass == RetVal.class) {
				adviceParamType = parameterType;
				targetType = targetReturnType;
			}
			if (adviceParamType != null && !adviceParamType.equals(OBJECT_TYPE) && !adviceParamType.equals(targetType)
					&& (isModifiable || !isBoxedTypeFor(targetType, adviceParamType))) {
				logger.debug("Advice method {} cannot be applied to {}.{} as the type of the parameter {} is " +
								"not compatible with the target",
						advice, targetClassName, targetMethodName, paramIdx);
				return false;
			}
		}
		// check @OverrideRetVal if present
		AnnotationInfo annotation = advice.getAnnotation(OverrideRetVal.class);
		if (annotation != null && !advice.getReturnType().equals(OBJECT_TYPE) &&
				!advice.getReturnType().equals(targetReturnType)) {
			logger.debug("Advice method {} cannot be applied to {}.{} as its @OverrideRetVal return type is " +
							"not compatible with the target",
					advice, targetClassName, targetMethodName);
			return false;
		}

		return true;
	}

	private boolean isBoxedTypeFor(Type primType, Type boxedType) {
		return boxedType.equals(getBoxedTypeFor(primType));
	}

	private Type getBoxedTypeFor(Type primType) {
		switch (primType.getSort()) {
		case Type.BOOLEAN:
			return BOOLEAN_OBJTYPE;
		case Type.CHAR:
			return CHARACTER_OBJTYPE;
		case Type.BYTE:
			return BYTE_OBJTYPE;
		case Type.SHORT:
			return SHORT_OBJTYPE;
		case Type.INT:
			return INTEGER_OBJTYPE;
		case Type.FLOAT:
			return FLOAT_OBJTYPE;
		case Type.LONG:
			return LONG_OBJTYPE;
		case Type.DOUBLE:
			return DOUBLE_OBJTYPE;
		}
		return null;
	}
}
// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import com.gilecode.xmx.aop.data.AnnotationInfo;
import com.gilecode.xmx.aop.data.MethodDeclarationInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class BadAdviceException extends Exception {
	public BadAdviceException(String message) {
		super(message);
	}

	public BadAdviceException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadAdviceException(Method adviceMethod, String reasonMessage) {
		super(buildMessage(adviceMethod, null, reasonMessage));
	}

	public BadAdviceException(Method adviceMethod, Annotation badAnnotation, String reasonMessage) {
		super(buildMessage(adviceMethod, badAnnotation, reasonMessage));
	}

	public BadAdviceException(MethodDeclarationInfo adviceMethod, AnnotationInfo badAnnotation, String reasonMessage) {
		super(buildMessage(adviceMethod, badAnnotation, reasonMessage));
	}

	private static String buildMessage(Object adviceMethod, Object badAnnotation, String reasonMessage) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("Bad advice method ").append(adviceMethod).append(": ");
		if (badAnnotation != null) {
			sb.append("annotation ").append(badAnnotation).append(' ');
		}
		sb.append(reasonMessage);
		return sb.toString();
	}


}

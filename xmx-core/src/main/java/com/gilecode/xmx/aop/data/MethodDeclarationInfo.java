// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;

/**
 * The basic information about the method declaration, like name, annotations, parameters etc.
 * Used when the actual class is not loaded yet, and shall not be loaded, so
 * Reflection API {@link Method} information is not available.
 */
public class MethodDeclarationInfo {
    private final String methodName;
    private final List<AnnotationInfo> methodAnnotations;
    private final AnnotatedTypeInfo[] parameters;
    private final Type returnType;

    public MethodDeclarationInfo(String methodName, Type[] parameterTypes, Type returnType) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.methodAnnotations = new LinkedList<>();
        this.parameters = new AnnotatedTypeInfo[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = new AnnotatedTypeInfo(parameterTypes[i]);
        }
    }

    public void setParameterInfo(int paramNum, AnnotatedTypeInfo param) {
        if (paramNum >= 0 && paramNum < parameters.length) {
            parameters[paramNum] = param;
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public List<AnnotationInfo> getMethodAnnotations() {
        return methodAnnotations;
    }

    public AnnotatedTypeInfo[] getParameters() {
        return parameters;
    }

    public void addAnnotation(AnnotationInfo info) {
        methodAnnotations.add(info);
    }

    public Type getReturnType() {
        return returnType;
    }

    public <T> AnnotationInfo getAnnotation(Class<T> annotationClass) {
        for (AnnotationInfo info : methodAnnotations) {
            if (info.getAnnotationClass() == annotationClass) {
                return info;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("'").append(returnType.getClassName()).append(' ').append(methodName).append('(');
        for (AnnotatedTypeInfo param : parameters) {
            sb.append(param.getType().getClassName()).append(',');
        }
        if (parameters.length > 0) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(')');
        return sb.toString();
    }
}

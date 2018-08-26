// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;

/**
 * A type with annotations. Used in {@link MethodDeclarationInfo}
 */
public class AnnotatedTypeInfo {
    private Type type;
    private List<AnnotationInfo> annotations = new LinkedList<>();

    public AnnotatedTypeInfo(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(AnnotationInfo annotation) {
        this.annotations.add(annotation);
    }
}

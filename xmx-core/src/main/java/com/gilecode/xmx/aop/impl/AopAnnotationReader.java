// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.data.AnnotationInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asm-based reader which extracts annotation properties to the given {@link AnnotationInfo} class
 */
public class AopAnnotationReader extends AnnotationVisitor {

    private final static Logger logger = LoggerFactory.getLogger(AopAnnotationReader.class);

    private final AnnotationInfo info;

    public AopAnnotationReader(AnnotationInfo info) {
        super(Opcodes.ASM6);
        this.info = info;
    }

    @Override
    public void visit(String name, Object value) {
        info.addProperty(name, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visitEnum(String name, String descriptor, String value) {
        info.addProperty(name, value);
        String enumClassName = Type.getType(descriptor).getClassName();
        try {
            Class<? extends Enum> enumClass = Class.forName(enumClassName).asSubclass(Enum.class);
            info.addProperty(name, Enum.valueOf(enumClass, value));
        } catch (Exception e) {
            logger.warn("Failed to read enum annotation value", e);
        }
    }
}

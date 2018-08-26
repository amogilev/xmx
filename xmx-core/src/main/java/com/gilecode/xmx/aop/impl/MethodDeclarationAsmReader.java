// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.Advice;
import com.gilecode.xmx.aop.data.AnnotationInfo;
import com.gilecode.xmx.aop.data.MethodDeclarationInfo;
import com.gilecode.xmx.model.XmxRuntimeException;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads method declarations from a class bytecode, using Asm's {@link ClassReader}
 */
public class MethodDeclarationAsmReader {

    /**
     * Parses the class bytecode (passed as a stream) and extracts the information about all method declarations.
     *
     * @param classStream the input stream which contains class bytecode
     * @return information about all methods declared in the class
     *
     * @throws IOException if I/O failure occurs
     */
    public static List<MethodDeclarationInfo> readMethodDeclarations(InputStream classStream) throws IOException {
        final List<MethodDeclarationInfo> methods = new ArrayList<>();
        ClassReader cr = new ClassReader(classStream);
        cr.accept(new ClassVisitor(Opcodes.ASM6) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_ABSTRACT) != 0) {
                    return null;
                }
                Type[] argumentTypes = Type.getArgumentTypes(descriptor);
                Type returnType = Type.getReturnType(descriptor);

                final MethodDeclarationInfo methodInfo = new MethodDeclarationInfo(name, argumentTypes, returnType);
                methods.add(methodInfo);

                return new MethodVisitor(Opcodes.ASM6) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        String className = Type.getType(descriptor).getClassName();
                        if (isXmxAopAnnotation(className)) {
                            // only care for XMX AOP annotations
                            AnnotationInfo info = makeEmptyAnnotation(className);
                            methodInfo.addAnnotation(info);
                            return new AopAnnotationReader(info);
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                        String className = Type.getType(descriptor).getClassName();
                        if (isXmxAopAnnotation(className)) {
                            // only care for XMX AOP annotations
                            AnnotationInfo info = makeEmptyAnnotation(className);
                            methodInfo.getParameters()[parameter].addAnnotation(info);
                            return new AopAnnotationReader(info);
                        } else {
                            return null;
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES + ClassReader.SKIP_DEBUG + ClassReader.SKIP_CODE);

        return methods;
    }

    private static boolean isXmxAopAnnotation(String className) {
        return className.startsWith(Advice.class.getPackage().getName());
    }

    private static AnnotationInfo makeEmptyAnnotation(String className) {
        try {
            return new AnnotationInfo(Class.forName(className).asSubclass(Annotation.class));
        } catch (ClassNotFoundException e) {
            throw new XmxRuntimeException(e);
        }
    }
}

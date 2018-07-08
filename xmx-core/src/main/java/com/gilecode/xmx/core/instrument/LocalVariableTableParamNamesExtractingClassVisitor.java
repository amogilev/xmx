// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import com.gilecode.xmx.core.params.IParamNamesConsumer;
import com.gilecode.xmx.core.params.ParamNamesCache;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A class visitor which invokes {@link LocalVariableTableParamNamesExtractor} for each found method.
 */
public class LocalVariableTableParamNamesExtractingClassVisitor extends ClassVisitor {

	/**
	 * The names cache to store the extracted information.
	 */
	private final ParamNamesCache paramNamesCache;

	/**
	 * Whether to skip extracting parameters from constructors.
	 */
	private final boolean skipInits;

	/**
	 * The name of the class.
	 */
	private final String javaClassName;

	/**
	 * A flag which can be set to skip further extraction of the methods of this class;
	 */
	private boolean skipExtraction = false;

	public LocalVariableTableParamNamesExtractingClassVisitor(String javaClassName, ParamNamesCache paramNamesCache, boolean skipInits) {
		super(Opcodes.ASM5);
		this.javaClassName = javaClassName;
		this.paramNamesCache = paramNamesCache;
		this.skipInits = skipInits;
	}

	@Override
	public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
		MethodVisitor parentVisitor = super.visitMethod(access, name, desc, signature, exceptions);

		Type[] argumentTypes;
		if (skipExtraction || name.equals("<clinit>") || isSynthetic(access) || isBridged(access)
				|| (skipInits && (name.equals("<init>")))
				|| ((argumentTypes = Type.getArgumentTypes(desc)).length == 0)) {
			return parentVisitor;
		} else {
			return new LocalVariableTableParamNamesExtractor(access, parentVisitor, argumentTypes,
					new IParamNamesConsumer() {
				@Override
				public void consume(String[] argNames) {
					boolean found = argNames != null && argNames.length > 0 && argNames[0] != null;
					if (found) {
						paramNamesCache.store(javaClassName, name, desc, argNames);
					} else {
						paramNamesCache.storeMissingClassInfo(javaClassName);
						skipExtraction = true;
					}
				}
			});
		}
	}

	private boolean isSynthetic(int access) {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}

	private boolean isBridged(int access) {
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import com.gilecode.xmx.core.params.IParamNamesConsumer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A method visitor which extracts parameter names from LocalVariableTable (debug info)
 */
public class LocalVariableTableParamNamesExtractor extends MethodVisitor {

	private final Type[] argumentTypes;
	private final String[] argumentNames;
	private final IParamNamesConsumer namesConsumer;
	private final int firstLocal;
	private final boolean isStatic;
	private boolean hasWideParams;

	public LocalVariableTableParamNamesExtractor(int access, MethodVisitor parentVisitor, Type[] argumentTypes,
			IParamNamesConsumer namesConsumer) {
		super(Opcodes.ASM5, parentVisitor);

		this.argumentTypes = argumentTypes;
		this.argumentNames = new String[argumentTypes.length];
		this.namesConsumer = namesConsumer;
		this.isStatic = (Opcodes.ACC_STATIC & access) != 0;

		int nextLocal = isStatic ? 0 : 1;
		for (Type argType : argumentTypes) {
			int sz = argType.getSize();
			if (sz > 1) {
				hasWideParams = true;
			}
			nextLocal += sz;
		}
		this.firstLocal = nextLocal;
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int localSlot) {
		super.visitLocalVariable(name, descriptor, signature, start, end, localSlot);

		if (localSlot < firstLocal) {
			// parameter found
			int paramIdx = getParamIndex(localSlot);
			if (paramIdx >= 0 && paramIdx < argumentNames.length) {
				argumentNames[paramIdx] = name;
			}
		}
	}

	private int getParamIndex(int localSlot) {
		if (!isStatic) {
			localSlot--;
		}
		if (!hasWideParams || localSlot < 0) {
			return localSlot;
		}
		int slot = 0;
		for (int i = 0; i < argumentTypes.length; i++) {
			Type argType = argumentTypes[i];
			if (slot == localSlot) {
				return i;
			}
			slot += argType.getSize();
		}
		// unexpected
		return -1;
	}

	public String[] getArgumentNames() {
		return argumentNames;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		namesConsumer.consume(argumentNames);
	}
}

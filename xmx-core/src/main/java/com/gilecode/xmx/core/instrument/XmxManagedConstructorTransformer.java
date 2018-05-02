// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Transforms constructors of managed objects - adds calls to
 * {@link com.gilecode.xmx.boot.XmxProxy#registerObject(Object, int)}
 */
public class XmxManagedConstructorTransformer extends MethodVisitor {

	private static final String CONSTR_NAME = "<init>";

	/**
	 * The internal class ID assigned to the class being transformed.
	 * <p/>
	 * registerObject() uses this ID to prevent duplicate or undesired registration
	 * from superclasses.
	 */
	private final int classId;

	/**
	 * The name of the class being transformed, in bytecode format (e.g. "java/lang/Object").
	 */
	private final String bcClassName;

	/**
	 * Whether invocation of another this(...) constructor is found.
	 * <p/>
	 * registerObject() is added only to constructors which do not invoke this()
	 * to prevent duplicate registrations.
	 */
	private boolean foundInvokeThis = false;

	public XmxManagedConstructorTransformer(int classId, String bcClassName, MethodVisitor parentVisitor) {
		super(Opcodes.ASM5, parentVisitor);
		this.classId = classId;
		this.bcClassName = bcClassName;
	}

	/**
	 * Detects invocation of this(...) constructors.
	 */
	@Override
	public void visitMethodInsn(int opcode, String owner,
	                            String name, String desc, boolean itf) {
		if (opcode == Opcodes.INVOKESPECIAL && name.equals(CONSTR_NAME) && bcClassName.equals(owner)) {
			foundInvokeThis = true;
		}
		super.visitMethodInsn(opcode, owner, name, desc, itf);
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode == Opcodes.RETURN && !foundInvokeThis) {
			super.visitVarInsn(Opcodes.ALOAD, 0);
			if (classId <= Byte.MAX_VALUE) {
				super.visitIntInsn(Opcodes.BIPUSH, classId);
			} else if (classId <= Short.MAX_VALUE){
				super.visitIntInsn(Opcodes.SIPUSH, classId);
			} else {
				assert false : "Current limit of managed classes is 32767";
				super.visitIntInsn(Opcodes.BIPUSH, -1);
			}

			super.visitMethodInsn(Opcodes.INVOKESTATIC,
					"com/gilecode/xmx/boot/XmxProxy",
					"registerObject",
					"(Ljava/lang/Object;I)V",
					false);
		}
		super.visitInsn(opcode);
	}

}

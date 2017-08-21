// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package am.xmx.core;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class XmxInstructionsAdder extends ClassVisitor {
	
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

	public XmxInstructionsAdder(ClassVisitor cv, int classId, String bcClassName) {
		super(Opcodes.ASM5, cv);
		this.classId = classId;
		this.bcClassName = bcClassName;
	}

	@Override
	public MethodVisitor visitMethod(final int access, String name, final String desc, String signature, String[] exceptions) {
		MethodVisitor parentVisitor = super.visitMethod(access, name, desc, signature, exceptions);
		
		if (name.startsWith(CONSTR_NAME)) {
			return new MethodVisitor(Opcodes.ASM5, parentVisitor) {
				
				/**
				 * Whether invocation of another this(...) constructor is found.
				 * <p/>
				 * registerObject() is added only to constructors which do not invoke this()
				 * to prevent duplicate registrations.
				 */
				private boolean foundInvokeThis = false;
				
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
						
						
						super.visitMethodInsn(Opcodes.INVOKESTATIC, "am/xmx/boot/XmxProxy", "registerObject", "(Ljava/lang/Object;I)V", false);
					}
					super.visitInsn(opcode);
				}
			};
		} else {
			return parentVisitor;
		}
	}


}

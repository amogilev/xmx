package am.xmx.core;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class XmxInstructionsAdder extends ClassVisitor {

	public XmxInstructionsAdder(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public MethodVisitor visitMethod(final int access, String name, final String desc, String signature, String[] exceptions) {
		MethodVisitor parentVisitor = super.visitMethod(access, name, desc, signature, exceptions);
		
		// TODO: optimization: skip transformation if constructor calls this(args)
		if (name.startsWith("<init>")) {
			return new MethodVisitor(Opcodes.ASM5, parentVisitor) {
				@Override
				public void visitInsn(int opcode) {
					if (opcode == Opcodes.RETURN) {
						super.visitVarInsn(Opcodes.ALOAD, 0);
						super.visitMethodInsn(Opcodes.INVOKESTATIC, "am/xmx/loader/XmxLoader", "registerObject", "(Ljava/lang/Object;)V", false);
					}
					super.visitInsn(opcode);
				}
			};
		} else {
			return parentVisitor;
		}
	}


}

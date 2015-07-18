package am.xmx.core;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class XmxInstructionsAdder extends ClassVisitor {
	
	private final int classId;

	public XmxInstructionsAdder(ClassVisitor cv, int classId) {
		super(Opcodes.ASM5, cv);
		this.classId = classId;
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
						if (classId <= Byte.MAX_VALUE) {
							super.visitIntInsn(Opcodes.BIPUSH, classId);
						} else if (classId <= Short.MAX_VALUE){
							super.visitIntInsn(Opcodes.SIPUSH, classId);
						} else {
							assert false : "Current limit of managed classes is 32767";
							super.visitIntInsn(Opcodes.BIPUSH, -1);
						}
						
						
						super.visitMethodInsn(Opcodes.INVOKESTATIC, "am/xmx/loader/XmxLoader", "registerObject", "(Ljava/lang/Object;I)V", false);
					}
					super.visitInsn(opcode);
				}
			};
		} else {
			return parentVisitor;
		}
	}


}

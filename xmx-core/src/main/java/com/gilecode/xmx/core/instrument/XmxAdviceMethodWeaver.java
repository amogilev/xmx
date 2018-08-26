// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.impl.InterceptedArgument;
import com.gilecode.xmx.aop.data.WeavingAdviceInfo;
import com.gilecode.xmx.aop.impl.WeavingContext;
import com.gilecode.xmx.boot.XmxAopProxy;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.List;
import java.util.Map;

/**
 * Transformer of a method bytecode which weaves the method with all of the specified
 * advices.
 */
public class XmxAdviceMethodWeaver extends AdviceAdapter {

	private static final Type OBJECT_TYPE = Type.getType(Object.class);
	private static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);
	private static final Type MAP_TYPE = Type.getType(Map.class);
	private static final Type THROWABLE_TYPE = Type.getType(Throwable.class);

	private static final String PROXY_BEFORE_DESC = Type.getMethodDescriptor(MAP_TYPE,
			Type.INT_TYPE, OBJECT_TYPE, OBJECT_ARRAY_TYPE);
	private static final String PROXY_AFTER_RETURN_DESC = Type.getMethodDescriptor(OBJECT_TYPE,
			OBJECT_TYPE, Type.INT_TYPE, MAP_TYPE, OBJECT_TYPE, OBJECT_ARRAY_TYPE);
	private static final String PROXY_AFTER_THROW_DESC = Type.getMethodDescriptor(Type.VOID_TYPE,
			THROWABLE_TYPE, Type.INT_TYPE, MAP_TYPE, OBJECT_TYPE, OBJECT_ARRAY_TYPE);

	private static final String PROXY_NAME = Type.getInternalName(XmxAopProxy.class);

	private final WeavingContext ctx;

	private int interceptedArgsLocal = -1;
	private int adviceInstancesMapLocal = -1;

	private final Label start = new Label(), handler = new Label();
	private final boolean hasAfterThrowAdvices;
	private final Type[] paramTypes;

	public XmxAdviceMethodWeaver(int access, String name, String desc, MethodVisitor mv,
	                             WeavingContext ctx) {
		super(Opcodes.ASM5, mv, access, name, desc);
		this.ctx = ctx;
		this.hasAfterThrowAdvices = hasAdvices(AdviceKind.AFTER_THROW);
		paramTypes = getArgumentTypes();
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		if (hasAfterThrowAdvices) {
			visitLabel(handler);
/*
	The code to calculate the required "handler" frame is experimental.
	While potentially can decrease startup time, it is over-complicated and requires a lot of testing.
	In particular, it is unclear if we can rely on the fact that locals for initial parameters are alive and not
	re-assigned to some other place by LocalsVariableSorter.

	If used, the following changes in other fcode required:
	- remove ClassWriter.COMPUTE_FRAMES
	- change ClassReader.SKIP_FRAMES to ClassReader.EXPAND_FRAMES
	- propagate the class name to use instead "sample/SampleClass"
*/
/*
			boolean isStaticMethod = (methodAccess & Opcodes.ACC_STATIC) != 0;
			Object[] locals = new Object[paramTypes.length + (isStaticMethod ? 0 : 1)];
			int startLocal = 0;
			if (!isStaticMethod) {
				locals[0] = "sample/SampleClass"; // shall be this class name instead
				startLocal = 1;
			}
			for (int i = startLocal; i < locals.length; i++) {
				// use TOP as locals bound to parameters potentially may be re-used
				locals[i] = Opcodes.TOP;
			}
			visitFrame(Opcodes.F_NEW, locals.length, locals, 1, new Object[]{"java/lang/Throwable"});
/**/
			// save exception for further re-throw
			dup();

			int joinPointId = ctx.getJoinpointId();
			push(joinPointId);
			loadOptLocal(adviceInstancesMapLocal);
			loadOptThis();
			loadLocal(interceptedArgsLocal);
			visitMethodInsn(INVOKESTATIC,
					PROXY_NAME,
					"afterThrow",
					PROXY_AFTER_THROW_DESC,
					false);
			// re-throw the same exception
			throwException();

			visitTryCatchBlock(start,
					handler,
					handler,
					null /* "java/lang/Throwable" */);
		}
		super.visitMaxs(maxStack, maxLocals);
	}

	@Override
	protected void onMethodEnter() {
		List<InterceptedArgument> interceptedArguments = ctx.getInterceptedArguments();
		int joinPointId = ctx.getJoinpointId();
		boolean hasAfterAdvices = hasAfterThrowAdvices || hasAdvices(AdviceKind.AFTER_RETURN);

		// 1) prepare array of intercepted arguments
		push(interceptedArguments.size()); // interested args array
		newArray(OBJECT_TYPE);
		for (int i = 0; i < interceptedArguments.size(); i++) {
			dup();
			push(i);

			InterceptedArgument interceptedArgument = interceptedArguments.get(i);
			int paramIdx = interceptedArgument.getTargetMethodParameterIdx();
			assert paramIdx >= 0 && paramIdx < paramTypes.length;

			loadArg(paramIdx);
			box(paramTypes[paramIdx]);
			arrayStore(OBJECT_TYPE);
		}
		interceptedArgsLocal = newLocal(OBJECT_ARRAY_TYPE);
		storeLocal(interceptedArgsLocal);

		// 2) if 'before' advices exists, invoke XmxAopProxy.before(int joinPointId, Object thisArg, Object[] interestedArgs)
		if (hasAdvices(AdviceKind.BEFORE)) {
			push(joinPointId);
			loadOptThis();
			loadLocal(interceptedArgsLocal);
			visitMethodInsn(INVOKESTATIC,
					PROXY_NAME,
					"before",
					PROXY_BEFORE_DESC,
					false);

			// 3) saved advice instances map for 'after' advices, if any
			if (hasAfterAdvices) {
				adviceInstancesMapLocal = newLocal(MAP_TYPE);
				storeLocal(adviceInstancesMapLocal);
			} else {
				pop();
			}

			// 4) overwrite modified arguments, if any
			for (int i = 0; i < interceptedArguments.size(); i++) {
				InterceptedArgument interceptedArgument = interceptedArguments.get(i);
				if (interceptedArgument.isModifiableAtBefore()) {
					int paramIdx = interceptedArgument.getTargetMethodParameterIdx();

					loadLocal(interceptedArgsLocal);
					push(i);
					arrayLoad(OBJECT_TYPE);

					unbox(paramTypes[paramIdx]);
					storeArg(paramIdx);
				}
			}
		}

		if (hasAfterThrowAdvices) {
			visitLabel(start);
		}

		super.onMethodEnter();
	}

	private boolean hasAdvices(AdviceKind kind) {
		List<WeavingAdviceInfo> advices = ctx.getAdviceInfoByKind().get(kind);
		return advices != null && !advices.isEmpty();
	}

	private void loadOptThis() {
		if ((methodAccess & Opcodes.ACC_STATIC) != 0) {
			pushNull();
		} else {
			loadThis();
		}
	}

	private void pushNull() {
		visitInsn(Opcodes.ACONST_NULL);
	}

	private void loadOptLocal(int local) {
		if (local < 0) {
			pushNull();
		} else {
			loadLocal(local);
		}
	}

	@Override
	protected void onMethodExit(int opcode) {
		if (opcode != ATHROW) {
			onAfterReturn(opcode);
		}
		// do nothing for throws at this point, as it will be caught in our block later
	}

	private void onAfterReturn(int opcode) {
		if (hasAdvices(AdviceKind.AFTER_RETURN)) {
			// depending on return type, 0 or 1 or 2 slots on stack are return value
			boolean isVoid = opcode == Opcodes.RETURN;
			boolean isPrimitiveRetVal = !isVoid && opcode != Opcodes.ARETURN;
			if (isVoid) {
				pushNull();
			} else if (isPrimitiveRetVal) {
				// primitive type returned, use boxing
				box(getReturnType());
			}

			int joinPointId = ctx.getJoinpointId();
			push(joinPointId);
			loadOptLocal(adviceInstancesMapLocal);
			loadOptThis();
			loadLocal(interceptedArgsLocal);
			visitMethodInsn(INVOKESTATIC,
					PROXY_NAME,
					"afterReturn",
					PROXY_AFTER_RETURN_DESC,
					false);
			// use return value from advices (if not overridden, same value will be returned)
			if (isPrimitiveRetVal) {
				unbox(getReturnType());
			} else if (isVoid) {
				pop();
			} else {
				if (!getReturnType().equals(OBJECT_TYPE)) {
					dup();
					mv.visitTypeInsn(Opcodes.CHECKCAST, getReturnType().getInternalName());
				}
			}
		}
	}
}

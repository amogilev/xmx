// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import com.gilecode.xmx.aop.impl.AopTestUtils;
import com.gilecode.xmx.core.params.IParamNamesConsumer;
import com.gilecode.xmx.core.params.ParamNamesCache;
import org.junit.Test;
import org.objectweb.asm.*;
import sample.SampleClass;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class TestLocalVariableTableParamNamesExtractor {

	@Test
	public void getArgumentNames() throws Exception {
		final ParamNamesCache cache = new ParamNamesCache();
		final String className = SampleClass.class.getName();
		extractParamNames(cache, className);

		String[] empty = new String[0];

		assertArrayEquals(empty, getParamNames(cache, "simpleThrow"));
		assertArrayEquals(empty, getParamNames(cache, "emptyStatic"));
		assertArrayEquals(new String[]{"arg1", "arg2"}, getParamNames(cache, "primitiveRet"));
		assertArrayEquals(new String[]{"i", "d", "l", "d2", "l2", "s"},
				getParamNames(cache, "params1"));

		assertNull(cache.getParameterNames(AopTestUtils.findMethod(Object.class, "equals")));

	}

	private void extractParamNames(final ParamNamesCache cache, final String className) throws IOException {
		ClassReader cr = new ClassReader(className);
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, null) {
			@Override
			public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
				return new LocalVariableTableParamNamesExtractor(access, null,
						Type.getArgumentTypes(desc),
						new IParamNamesConsumer() {
							@Override
							public void consume(String[] argNames) {
								cache.store(className, name, desc, argNames);
							}
						});
			}
		};
		cr.accept(cv, ClassReader.SKIP_FRAMES);
	}

	private String[] getParamNames(ParamNamesCache cache, String methodName) {
		return cache.getParameterNames(AopTestUtils.findMethod(SampleClass.class, methodName));
	}
}
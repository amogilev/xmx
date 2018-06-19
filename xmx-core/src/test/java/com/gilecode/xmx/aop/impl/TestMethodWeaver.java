// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.boot.IXmxAopService;
import com.gilecode.xmx.boot.XmxAopProxy;
import com.gilecode.xmx.core.instrument.XmxAdviceMethodWeaver;
import com.gilecode.xmx.model.XmxRuntimeException;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.*;
import sample.SampleClass;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.gilecode.xmx.aop.impl.AopTestUtils.findMethod;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class TestMethodWeaver {

	private XmxAopManager aopManager;

	static Capture<String> testEventsCap = Capture.newInstance(CaptureType.ALL);
	static Capture<Object[]> testArgsCap = Capture.newInstance(CaptureType.ALL);
	static Capture<Object> testObjectsCap = Capture.newInstance(CaptureType.ALL);
	static Capture<Object> testInstanceCap = Capture.newInstance(CaptureType.ALL);

	@Before
	public void setUp() throws Exception {
		aopManager = new XmxAopManager(null, null);

		Method setAopService = XmxAopProxy.class.getDeclaredMethod("setAopService", IXmxAopService.class);
		setAopService.setAccessible(true);
		setAopService.invoke(null, aopManager);

		testEventsCap.reset();
		testArgsCap.reset();
		testObjectsCap.reset();
		testInstanceCap.reset();
	}


	private static class TestClassLoader extends ClassLoader {

		public TestClassLoader(ClassLoader parent) {
			super(parent);
		}

		public Class<?> defineTestClassWithWeaving(String className,
		                                           String targetMethodName, WeavingContext wtcx) throws ClassFormatError, IOException {
			try {
				URL testClassesLocation = SampleClass.class.getProtectionDomain().getCodeSource().getLocation();
				URI classFileURI = testClassesLocation.toURI().resolve(className.replace('.', '/') + ".class");
				byte[] original = Files.readAllBytes(Paths.get(classFileURI));

				byte[] transformed = transformClassMethod(original, targetMethodName, wtcx);
				// uncomment out next line if need to check actual transformed byte code with some tools
				// Files.write(Paths.get("C://temp/SampleClass.class"), transformed);
				return defineClass(className, transformed, 0, transformed.length);
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}

		private byte[] transformClassMethod(byte[] ba, final String targetMethodName, final WeavingContext wctx) {
			ClassReader cr = new ClassReader(ba);
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, cw) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					MethodVisitor parentVisitor = super.visitMethod(access, name, desc, signature, exceptions);
					if (name.equals(targetMethodName)) {
						return new XmxAdviceMethodWeaver(access, name, desc, parentVisitor, wctx);
					} else {
						return parentVisitor;
					}
				}
			};

			cr.accept(cv, ClassReader.SKIP_FRAMES);
			return cw.toByteArray();
		}
	}

	private Class<?> weaveClass(XmxAopManager aopManager, Class<?> targetClass, String targetMethodName,
	                            Class<?>...adviceClasses) throws Exception {
		ClassLoader thisCL = TestMethodWeaver.class.getClassLoader();
		TestClassLoader testCL = new TestClassLoader(thisCL);

		// do not call aopManager.loadAndVerifyAdvices() as the advice class is not in jar
		for (Class<?> adviceClass : adviceClasses) {
			aopManager.getAdviceVerifier().verifyAdviceClass(adviceClass);
		}

		Method targetMethod = findMethod(targetClass, targetMethodName);
		assertNotNull(targetMethod);

		WeavingContext wctx = AopTestUtils.prepareTestWeavingContext(aopManager, targetMethod, adviceClasses);
		Class<?> advisedClass = testCL.defineTestClassWithWeaving(targetClass.getName(),
				targetMethodName, wctx);
		return advisedClass;
	}

	private static class SampleAdvice_Empty {

		@Advice(AdviceKind.BEFORE)
		public void before() {
			testEventsCap.setValue("EmptyAdvice.before");
			testInstanceCap.setValue(this);
		}

		@Advice(AdviceKind.AFTER_RETURN)
		public void afterReturn() {
			testEventsCap.setValue("EmptyAdvice.afterReturn");
			testInstanceCap.setValue(this);
		}

		@Advice(AdviceKind.AFTER_THROW)
		public void afterThrow() {
			testEventsCap.setValue("EmptyAdvice.afterThrow");
			testInstanceCap.setValue(this);
		}
	}

	private static class SampleAdvice_Generic_Before {
		@Advice(AdviceKind.BEFORE)
		public void before(@AllArguments Object[] args, @This Object target) {
			testEventsCap.setValue("UniversalAdvice_Before");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(args);
			testObjectsCap.setValue(target);
		}
	}

	private static class SampleAdvice_Generic_Before_Static {
		@Advice(AdviceKind.BEFORE)
		public static void before(@AllArguments Object[] args, @This Object target) {
			testEventsCap.setValue("UniversalAdvice_Before_Static");
			testArgsCap.setValue(args);
			testObjectsCap.setValue(target);
		}
	}

	private static class SampleAdvice_Generic_Before_Mod {
		@Advice(AdviceKind.BEFORE)
		public void before(@AllArguments(modifiable = true) Object[] args, @This Object target) {
			testEventsCap.setValue("UniversalAdvice_Before");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(args);
			testObjectsCap.setValue(target);
		}
	}

	private static class SampleAdvice_Generic_AfterRet {
		@Advice(AdviceKind.AFTER_RETURN)
		public void afterReturn(@AllArguments Object[] args, @This Object target, @RetVal Object retVal) {
			testEventsCap.setValue("UniversalAdvice_AfterRet");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(args);
			testObjectsCap.setValue(target);
			testObjectsCap.setValue(retVal);
		}
	}

	private static class SampleAdvice_Generic_AfterRet_Mod {
		@Advice(AdviceKind.AFTER_RETURN)
		public @OverrideRetVal Object afterReturn(@AllArguments Object[] args, @This Object target, @RetVal Object retVal) {
			testEventsCap.setValue("UniversalAdvice_AfterRet");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(args);
			testObjectsCap.setValue(target);
			testObjectsCap.setValue(retVal);
			return retVal;
		}
	}

	private static class SampleAdvice_Generic_AfterThrow {
		@Advice(AdviceKind.AFTER_THROW)
		public void afterThrow(@AllArguments Object[] args, @This Object target, @Thrown Throwable ex) {
			testEventsCap.setValue("UniversalAdvice_AfterThrow");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(args);
			testObjectsCap.setValue(target);
			testObjectsCap.setValue(ex);
		}
	}

	/*
	Test 1: empty method and void advices of all kinds
	 */
	@Test
	public void testEmpty() throws Exception {
		String methodName = "empty";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_Empty.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object result = advisedClass.getDeclaredMethod(methodName).invoke(sampleInst);
		assertNull(result);
		assertTrue(testEventsCap.hasCaptured());
		assertEquals(2, testEventsCap.getValues().size());
		assertEquals(asList("EmptyAdvice.before", "EmptyAdvice.afterReturn"), testEventsCap.getValues());
		checkSameAdviceInstance();
	}

	private void checkSameAdviceInstance() {
		assertTrue(testInstanceCap.hasCaptured());
		List<Object> instances = testInstanceCap.getValues();
		Object first = instances.get(0);
		for (int i = 1; i < instances.size(); i++) {
			assertSame("Advice instances not equal", first, instances.get(i));
		}
	}

	/*
	Test 2: throw-only method and void advices of all kinds
	 */
	@Test
	public void testSimpleThrow() throws Exception {
		String methodName = "simpleThrow";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_Empty.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		try {
			advisedClass.getDeclaredMethod(methodName).invoke(sampleInst);
			fail("Exception expected");
		} catch (InvocationTargetException e) {
			assertEquals(XmxRuntimeException.class, e.getCause().getClass());
			assertEquals("sampleException", e.getCause().getMessage());
		}
		assertTrue(testEventsCap.hasCaptured());
		assertEquals(2, testEventsCap.getValues().size());
		assertEquals(asList("EmptyAdvice.before", "EmptyAdvice.afterThrow"), testEventsCap.getValues());
		checkSameAdviceInstance();
	}

	/*
	Test 3: check @This with static methods
	 */
	@Test
	public void testStaticThis() throws Exception {
		String methodName = "emptyStatic";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_Generic_Before.class, SampleAdvice_Generic_AfterRet.class);

		Object retVal = advisedClass.getDeclaredMethod(methodName).invoke(null);
		assertEquals("emptyStatic", retVal);

		assertTrue(testEventsCap.hasCaptured());
		assertEquals(2, testEventsCap.getValues().size());
		assertEquals(asList("UniversalAdvice_Before", "UniversalAdvice_AfterRet"), testEventsCap.getValues());

		assertTrue(testObjectsCap.hasCaptured());
		assertEquals(3, testObjectsCap.getValues().size());
		assertNull(testObjectsCap.getValues().get(0));
		assertNull(testObjectsCap.getValues().get(1));
		assertEquals("emptyStatic", testObjectsCap.getValues().get(2));
	}

	/*
	Test 4: check various arguments for BEFORE
	 */

	private static class SampleAdvice_CustomArgsBefore1 {
		@Advice(AdviceKind.BEFORE)
		public void before(@AllArguments Object[] args, @This Object target,
		                   @ModifiableArgument(0) long[] arg0, @Argument(1) Long arg1) {
			testEventsCap.setValue("CustomArgsBefore1");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(new Object[]{args, arg0, arg1});
			testObjectsCap.setValue(target);
			arg0[0] += 100L;
		}
	}
	private static class SampleAdvice_CustomArgsBefore2 {
		@Advice(AdviceKind.BEFORE)
		public void before(@AllArguments(modifiable = true) Object[] args, @This Object target, @Argument(0) long arg0) {
			testEventsCap.setValue("CustomArgsBefore2");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(new Object[]{args, arg0});
			testObjectsCap.setValue(target);
			args[0] = 200L + (long)args[0];
		}
	}

	@Test
	public void testBeforeArgs1() throws Exception {
		String methodName = "primitiveRet";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_CustomArgsBefore1.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object retVal = findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		assertEquals(121L, retVal);

		assertTrue(testArgsCap.hasCaptured());
		Object[] args = (Object[]) testArgsCap.getValue();
		assertEquals(3, args.length);
		assertArrayEquals(new Object[]{101L, 20L}, (Object[])args[0]);
		assertArrayEquals(new long[]{101L}, (long[])args[1]);
		assertEquals(20L, args[2]);
	}

	@Test
	public void testBeforeArgs2() throws Exception {
		String methodName = "primitiveRet";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_CustomArgsBefore2.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object retVal = findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		assertEquals(221L, retVal);

		assertTrue(testArgsCap.hasCaptured());
		Object[] args = (Object[]) testArgsCap.getValue();
		assertEquals(2, args.length);
		assertArrayEquals(new Object[]{201L, 20L}, (Object[])args[0]);
		assertEquals(1L, args[1]);
	}

	/*
	Test 5: check various arguments for AFTER_RETURN
	 */

	private static class SampleAdvice_CustomAfterReturn1 {
		@Advice(AdviceKind.AFTER_RETURN)
		public long afterReturn(@AllArguments Object[] args, @This Object target, @RetVal long retVal) {
			testEventsCap.setValue("CustomAfterRet1");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(new Object[]{args, retVal});
			testObjectsCap.setValue(target);
			return retVal + 1000;
		}
	}
	private static class SampleAdvice_CustomAfterReturn2 {
		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal
		public long afterReturn(@AllArguments Object[] args, @This Object target, @RetVal long retVal) {
			testEventsCap.setValue("CustomAfterRet2");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(new Object[]{args, retVal});
			testObjectsCap.setValue(target);
			return retVal + 2000;
		}
	}

	private static class SampleAdvice_CustomAfterReturn3 {
		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal
		public Long afterReturn(@AllArguments Object[] args, @This Object target, @RetVal Long retVal) {
			testEventsCap.setValue("CustomAfterRet3");
			testInstanceCap.setValue(this);
			testArgsCap.setValue(new Object[]{args, retVal});
			testObjectsCap.setValue(target);
			return retVal + 3000;
		}
	}

	@Test
	public void testAfterRet1() throws Exception {
		String methodName = "primitiveRet";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_CustomAfterReturn1.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object retVal = findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		assertEquals(21L, retVal);

		assertTrue(testArgsCap.hasCaptured());
		Object[] args = (Object[]) testArgsCap.getValue();
		assertEquals(2, args.length);
		assertArrayEquals(new Object[]{1L, 20L}, (Object[])args[0]);
		assertEquals(21L, args[1]);

		assertTrue(testObjectsCap.hasCaptured());
		assertEquals(sampleInst, testObjectsCap.getValue());
	}

	@Test
	public void testAfterRet2() throws Exception {
		String methodName = "primitiveRet";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_CustomAfterReturn2.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object retVal = findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		assertEquals(2021L, retVal);

		assertTrue(testArgsCap.hasCaptured());
		Object[] args = (Object[]) testArgsCap.getValue();
		assertEquals(2, args.length);
		assertArrayEquals(new Object[]{1L, 20L}, (Object[])args[0]);
		assertEquals(21L, args[1]);
	}

	@Test
	public void testAfterRet3() throws Exception {
		String methodName = "boxedRet";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_CustomAfterReturn3.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object retVal = findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		assertEquals(3021L, retVal);

		assertTrue(testArgsCap.hasCaptured());
		Object[] args = (Object[]) testArgsCap.getValue();
		assertEquals(2, args.length);
		assertArrayEquals(new Object[]{1L, 20L}, (Object[])args[0]);
		assertEquals(21L, args[1]);
	}

	/*
	Test 6: check complicated afterThrow advice
	 */
	@Test
	public void testAfterThrow() throws Exception {
		String methodName = "complexThrow";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_Generic_AfterThrow.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		try {
			findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		} catch (InvocationTargetException e) {
			assertEquals(XmxRuntimeException.class, e.getCause().getClass());
			assertEquals("sampleException2", e.getCause().getMessage());
		}
		assertTrue(testEventsCap.hasCaptured());
		assertEquals("UniversalAdvice_AfterThrow", testEventsCap.getValue());

		assertTrue(testObjectsCap.hasCaptured());
		assertEquals(2, testObjectsCap.getValues().size());
		assertEquals(sampleInst, testObjectsCap.getValues().get(0));
		Throwable ex = (Throwable) testObjectsCap.getValues().get(1);
		assertTrue(ex instanceof XmxRuntimeException);
		assertEquals("sampleException2", ex.getMessage());

		assertTrue(testArgsCap.hasCaptured());
		assertArrayEquals(new Object[]{1L, 20L}, testArgsCap.getValue());
	}

	/*
	Test 7: afterThrow advice with a caught throw
	 */
	@Test
	public void testCaughtThrow() throws Exception {
		String methodName = "caughtThrow";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_Generic_AfterThrow.class, SampleAdvice_Generic_AfterRet.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		Object result = findMethod(advisedClass, methodName).invoke(sampleInst, 1L, 20L);
		assertEquals(21L, result);

		assertTrue(testEventsCap.hasCaptured());
		assertEquals("UniversalAdvice_AfterRet", testEventsCap.getValue());

		assertTrue(testArgsCap.hasCaptured());
		assertArrayEquals(new Object[]{1L, 20L}, testArgsCap.getValue());
	}

	/*
	Test 8: static advice
	 */
	@Test
	public void testStaticAdvice() throws Exception {
		String methodName = "empty";
		Class<?> advisedClass = weaveClass(aopManager, SampleClass.class, methodName,
				SampleAdvice_Generic_Before_Static.class);

		Object sampleInst = advisedClass.getDeclaredConstructor().newInstance();
		findMethod(advisedClass, methodName).invoke(sampleInst);

		assertTrue(testEventsCap.hasCaptured());
		assertEquals("UniversalAdvice_Before_Static", testEventsCap.getValue());
	}
}
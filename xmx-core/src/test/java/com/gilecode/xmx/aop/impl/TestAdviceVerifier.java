// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.aop.data.MethodDeclarationInfo;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class TestAdviceVerifier {

	private AdviceVerifier uut = new AdviceVerifier();

	//
	// Tests for verifyAdviceClass()
	//

	private static class SampleGoodAdvice1 {

		@Advice(AdviceKind.BEFORE)
		public static void foo1(@This Object aThis, @Argument(0) String a0, @ModifiableArgument(0) String[] arg1,
				@TargetMethod Method target,
				@AllArguments(modifiable = true) Object[] args) {
		}

		@Advice(AdviceKind.BEFORE)
		public static void foo2() {
		}

		@Advice(AdviceKind.AFTER_RETURN)
		public static @OverrideRetVal
		String foo3(@This Object aThis, @Argument(0) String a0,
				@AllArguments Object[] args, @RetVal String retVal) {
			return retVal + "1";
		}

		@Advice(AdviceKind.AFTER_RETURN)
		public static void foo4() {
		}

		@Advice(AdviceKind.AFTER_THROW)
		public static String foo5(@This Object aThis, @Argument(0) String a0, @TargetMethod Method target,
				@AllArguments Object[] args, @Thrown Throwable ex) {
			return "1";
		}

		@Advice(AdviceKind.AFTER_THROW)
		public static void foo6() {
		}
	}

	// bad: two argument annotations
	private static class SampleBadAdvice_MultiAnno {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@This @Argument(1) Object arg1) {
		}
	}

	// bad: no argument annotation
	private static class SampleBadAdvice_NoAnno {
		@Advice(AdviceKind.BEFORE)
		public static void foo(Object arg1) {
		}
	}

	// bad: annotation not allowed for BEFORE
	private static class SampleBadAdvice_NotBeforeAnno1 {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@RetVal Object arg1) {
		}
	}
	private static class SampleBadAdvice_NotBeforeAnno2 {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@Thrown Object arg1) {
		}
	}
	private static class SampleBadAdvice_NotBeforeAnno3 {
		@Advice(AdviceKind.BEFORE)
		public static @OverrideRetVal Object foo() {
			return null;
		}
	}

	// bad: annotation not allowed for AFTER_RETURN
	private static class SampleBadAdvice_NotAfterRetAnno {
		@Advice(AdviceKind.AFTER_RETURN)
		public static void foo(@Thrown Object arg1) {
		}
	}

	// bad: annotation not allowed for AFTER_THROW
	private static class SampleBadAdvice_NotAfterThrowAnno1 {
		@Advice(AdviceKind.AFTER_THROW)
		public static void foo(@RetVal Object arg1) {
		}
	}
	private static class SampleBadAdvice_NotAfterThrowAnno2 {
		@Advice(AdviceKind.AFTER_THROW)
		public static @OverrideRetVal Object foo() {
			return null;
		}
	}

	// bad: array is required for modifiable argument
	private static class SampleBadAdvice_ModArgNotArray1 {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@ModifiableArgument(1) Object arg1) {
		}
	}
	private static class SampleBadAdvice_ModArgNotArray2 {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@AllArguments(modifiable = true) List<Object> arg1) {
		}
	}

	// bad: modifiable annotation is allowed only for BEFORE
	private static class SampleBadAdvice_ModNotAtBefore1 {
		@Advice(AdviceKind.AFTER_RETURN)
		public static void foo(@ModifiableArgument(1) Object[] arg1) {
		}
	}
	private static class SampleBadAdvice_ModNotAtBefore2 {
		@Advice(AdviceKind.AFTER_THROW)
		public static void foo(@ModifiableArgument(1) Object[] arg1) {
		}
	}
	private static class SampleBadAdvice_ModNotAtBefore3 {
		@Advice(AdviceKind.AFTER_RETURN)
		public static void foo(@AllArguments(modifiable = true) Object[] arg1) {
		}
	}

	// bad: @Thrown requires Throwable type
	private static class SampleBadAdvice_ThrownNotThrowable {
		@Advice(AdviceKind.AFTER_THROW)
		public static void foo(@Thrown Object arg1) {
		}
	}

	// bad: @targetMethod requires Method type
	private static class SampleBadAdvice_TargetNotMethod {
		@Advice(AdviceKind.AFTER_THROW)
		public static void foo(@TargetMethod Object arg1) {
		}
	}

	// bad: invalid arg index
	private static class SampleBadAdvice_BadArgIndex1 {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@Argument(-1) Object arg1) {
		}
	}
	private static class SampleBadAdvice_BadArgIndex2 {
		@Advice(AdviceKind.AFTER_RETURN)
		public static void foo(@Argument(1000) Object arg1) {
		}
	}
	private static class SampleBadAdvice_BadArgIndex3 {
		@Advice(AdviceKind.BEFORE)
		public static void foo(@ModifiableArgument(-1) Object arg1) {
		}
	}

	private void verify(Class<?> c) throws IOException, BadAdviceException {
		uut.verifyAdviceClass(AopTestUtils.getClassAsStream(c));
	}

	@Test
	public void testVerifyGoodAdvice1() throws Exception {
		verify(SampleGoodAdvice1.class);
	}

	@Test
	public void testVerifyBad_MultipleArgAnno() throws Exception {
		try {
			verify(SampleBadAdvice_MultiAnno.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " overwrites annotation");
		}
	}

	@Test
	public void testVerifyBad_NoArgAnno() throws Exception {
		try {
			verify(SampleBadAdvice_NoAnno.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " no argument annotation");
		}
	}

	@Test
	public void testVerifyBad_IllegalAnnoAtBefore() throws Exception {
		Class<?>[] adviceClasses = {SampleBadAdvice_NotBeforeAnno1.class, SampleBadAdvice_NotBeforeAnno2.class,
				SampleBadAdvice_NotBeforeAnno3.class};
		for (Class<?> c : adviceClasses) {
			try {
				verify(c);
				fail("Expected BadAdviceException");
			} catch (BadAdviceException e) {
				checkMessage(e, " is not allowed for advice kind BEFORE");
			}
		}
	}

	@Test
	public void testVerifyBad_IllegalAnnoAtAfterRet() throws Exception {
		try {
			verify(SampleBadAdvice_NotAfterRetAnno.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " is not allowed for advice kind AFTER_RETURN");
		}
	}

	@Test
	public void testVerifyBad_IllegalAnnoAtAfterThrow() throws Exception {
		Class<?>[] adviceClasses = {SampleBadAdvice_NotAfterThrowAnno1.class, SampleBadAdvice_NotAfterThrowAnno2.class};
		for (Class<?> c : adviceClasses) {
			try {
				verify(c);
				fail("Expected BadAdviceException");
			} catch (BadAdviceException e) {
				checkMessage(e, " is not allowed for advice kind AFTER_THROW");
			}
		}
	}

	@Test
	public void testVerifyBad_ModifiableRequiresArray() throws Exception {
		try {
			verify(SampleBadAdvice_ModArgNotArray1.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " requires array type");
		}
		try {
			verify(SampleBadAdvice_ModArgNotArray2.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " requires Object[] type");
		}
	}

	@Test
	public void testVerifyBad_ModifiableRequiresBefore() throws IOException {
		Class<?>[] adviceClasses = {SampleBadAdvice_ModNotAtBefore1.class, SampleBadAdvice_ModNotAtBefore1.class,
				SampleBadAdvice_ModNotAtBefore3.class};
		for (Class<?> c : adviceClasses) {
			try {
				verify(c);
				fail("Expected BadAdviceException");
			} catch (BadAdviceException e) {
				String expectedMsgPart = " is not allowed for advice kind";
				checkMessage(e, expectedMsgPart);
			}
		}
	}

	@Test
	public void testVerifyBad_ThrownNotThrowable() throws IOException {
		try {
			verify(SampleBadAdvice_ThrownNotThrowable.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " requires java.lang.Throwable type");
		}
	}

	@Test
	public void testVerifyBad_TargetNotMethod() throws IOException {
		try {
			verify(SampleBadAdvice_TargetNotMethod.class);
			fail("Expected BadAdviceException");
		} catch (BadAdviceException e) {
			checkMessage(e, " requires java.lang.reflect.Method type");
		}
	}

	@Test
	public void testVerifyBad_IllegalArgIndex() throws IOException {
		Class<?>[] adviceClasses = {SampleBadAdvice_BadArgIndex1.class, SampleBadAdvice_BadArgIndex2.class,
				SampleBadAdvice_BadArgIndex3.class};
		for (Class<?> c : adviceClasses) {
			try {
				verify(c);
				fail("Expected BadAdviceException");
			} catch (BadAdviceException e) {
				String expectedMsgPart = " has invalid parameter index";
				checkMessage(e, expectedMsgPart);
			}
		}
	}

	private void checkMessage(BadAdviceException e, String expectedMsgPart) {
		String msg = e.getMessage();
		if (!msg.contains(expectedMsgPart)) {
			fail("The exception message shall contain \"" + expectedMsgPart +"\":\n" + msg);
		}
		assertTrue(e.getMessage().contains(expectedMsgPart));
	}

	//
	// Tests for isAdviceCompatibleMethod()
	//

	private static void target1() {}

	private int target2(Long arg1, double arg2) {
		return 0;
	}

	private static class SampleAdvice_TypeMatch {

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal int matchExact(@Argument(0) Long arg1, @Argument(1) double arg2, @RetVal int ret) { return 0; }

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal int matchReordered(@Argument(1) double arg2, @RetVal int ret, @Argument(0) Long arg1) { return 0; }

		@Advice(AdviceKind.BEFORE)
		void matchExactMod(@ModifiableArgument(0) Long[] arg1, @ModifiableArgument(1) double[] arg2) {}

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal int matchCorrectBoxing(@Argument(0) Long arg1, @Argument(1) Double arg2, @RetVal Integer ret) { return 0; }

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal Integer matchBadBoxing1(@Argument(0) Long arg1, @Argument(1) double arg2) { return 0; }

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal int matchBadBoxing2(@Argument(0) long arg1, @Argument(1) double arg2) { return 0; }

		@Advice(AdviceKind.BEFORE)
		void matchBadType1(@ModifiableArgument(1) Double[] arg2) { }

		@Advice(AdviceKind.AFTER_RETURN)
		void matchBadType2(@RetVal String ret) { }
	}

	@Test
	public void testCompatibility_TypeMatch() throws Exception {
		Class<?> c = SampleAdvice_TypeMatch.class;

		assertTrue(checkCompatibility(c, "matchExact", "target2"));
		assertTrue(checkCompatibility(c, "matchExactMod", "target2"));
		assertTrue(checkCompatibility(c, "matchCorrectBoxing", "target2"));
		assertTrue(checkCompatibility(c, "matchReordered", "target2"));

		assertFalse(checkCompatibility(c, "matchBadBoxing1", "target2"));
		assertFalse(checkCompatibility(c, "matchBadBoxing2", "target2"));

		assertFalse(checkCompatibility(c, "matchBadType1", "target2"));
		assertFalse(checkCompatibility(c, "matchBadType2", "target2"));
	}

	private static class SampleAdvice_Empty {

		@Advice(AdviceKind.BEFORE)
		void adviceBefore() {}

		@Advice(AdviceKind.AFTER_RETURN)
		void adviceAfterReturn() {}

		@Advice(AdviceKind.AFTER_THROW)
		void adviceAfterThrow() {}
	}

	@Test
	public void testCompatibility_Empty() throws Exception {
		assertTrue(checkCompatibility(SampleAdvice_Empty.class, "adviceBefore", "target1"));
		assertTrue(checkCompatibility(SampleAdvice_Empty.class, "adviceAfterReturn", "target1"));
		assertTrue(checkCompatibility(SampleAdvice_Empty.class, "adviceAfterThrow", "target1"));

		assertTrue(checkCompatibility(SampleAdvice_Empty.class, "adviceBefore", "target2"));
		assertTrue(checkCompatibility(SampleAdvice_Empty.class, "adviceAfterReturn", "target2"));
		assertTrue(checkCompatibility(SampleAdvice_Empty.class, "adviceAfterThrow", "target2"));
	}

	private static class SampleAdvice_Generic {

		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@This Object target, @TargetMethod Method targetMethod, @AllArguments(modifiable = true) Object[] args) {}

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal
		Object adviceAfterReturn(@This Object target, @TargetMethod Method targetMethod, @AllArguments Object[] args, @RetVal Object retVal) {
			return retVal;
		}

		@Advice(AdviceKind.AFTER_THROW)
		void adviceAfterThrow(@This Object target, @TargetMethod Method targetMethod, @AllArguments Object[] args, @Thrown Throwable ex) {}
	}

	@Test
	public void testCompatibility_Universal() throws Exception {
		assertTrue(checkCompatibility(SampleAdvice_Generic.class, "adviceBefore", "target1"));
		assertTrue(checkCompatibility(SampleAdvice_Generic.class, "adviceAfterReturn", "target1"));
		assertTrue(checkCompatibility(SampleAdvice_Generic.class, "adviceAfterThrow", "target1"));

		assertTrue(checkCompatibility(SampleAdvice_Generic.class, "adviceBefore", "target2"));
		assertTrue(checkCompatibility(SampleAdvice_Generic.class, "adviceAfterReturn", "target2"));
		assertTrue(checkCompatibility(SampleAdvice_Generic.class, "adviceAfterThrow", "target2"));
	}

	private static class SampleAdvice_ArgNum {

		@Advice(AdviceKind.BEFORE)
		void arg0(@ModifiableArgument(0) Object[] arg0) {}

		@Advice(AdviceKind.AFTER_RETURN)
		void arg1(@Argument(1) Object arg1, @Argument(0) Object arg0) {}

		@Advice(AdviceKind.AFTER_RETURN)
		void arg2(@Argument(2) Object arg2) {}
	}

	@Test
	public void testCompatibility_ArgNum() throws Exception {
		assertFalse(checkCompatibility(SampleAdvice_ArgNum.class, "arg0", "target1"));
		assertFalse(checkCompatibility(SampleAdvice_ArgNum.class, "arg1", "target1"));
		assertFalse(checkCompatibility(SampleAdvice_ArgNum.class, "arg2", "target1"));

		assertTrue(checkCompatibility(SampleAdvice_ArgNum.class, "arg0", "target2"));
		assertTrue(checkCompatibility(SampleAdvice_ArgNum.class, "arg1", "target2"));

		assertFalse(checkCompatibility(SampleAdvice_ArgNum.class, "arg2", "target2"));
	}

	private boolean checkCompatibility(Class<?> adviceClass, String adviceMethodName, String testMethodName) throws Exception {
		verify(adviceClass);
		Method target = AopTestUtils.findMethod(TestAdviceVerifier.class, testMethodName);
		MethodDeclarationInfo advice = AopTestUtils.findMethodDeclaration(adviceClass, adviceMethodName);
		return uut.isAdviceCompatibleMethod(advice, Type.getArgumentTypes(target), Type.getReturnType(target),
				TestAdviceVerifier.class.getName(), testMethodName);
	}

}

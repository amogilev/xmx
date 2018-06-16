// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.TestUtils;
import com.gilecode.xmx.aop.*;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class TestXmxAopManager {

	private XmxAopManager uut = new XmxAopManager();

	private int target(Long arg0, String arg1, double arg2) {
		return 0;
	}

	//
	// Tests for prepareMethodAdvicesWeaving()
	//

	private static class SampleAdvice_CheckFast1 {

		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@AllArguments Object[] args) {}

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal
		Object adviceAfterReturn(@Argument(0) Long arg0, @Argument(1) String arg1, @Argument(2) double arg2) {
			return null;
		}

		// not fast
		@Advice(AdviceKind.AFTER_THROW)
		void adviceAfterThrow(@AllArguments Object[] args, @Thrown Throwable ex) {}
	}

	@Test
	public void testPrepareCheckFast1() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CheckFast1.class);
		List<AdviceArgument> args;
		InterceptedArgument iarg;

		assertNotNull(ctx);
		assertEquals(3, ctx.getAdviceInfoByKind().size());
		List<WeavingAdviceInfo> beforeAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE);
		List<WeavingAdviceInfo> afterRetAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_RETURN);
		List<WeavingAdviceInfo> afterThrowAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_THROW);
		assertEquals(1, beforeAdvicesInfo.size());
		assertEquals(1, afterRetAdvicesInfo.size());
		assertEquals(1, afterThrowAdvicesInfo.size());

		WeavingAdviceInfo beforeAdviceInfo = beforeAdvicesInfo.get(0);
		WeavingAdviceInfo afterRetAdviceInfo = afterRetAdvicesInfo.get(0);
		WeavingAdviceInfo afterThrowAdviceInfo = afterThrowAdvicesInfo.get(0);

		assertTrue(beforeAdviceInfo.isFastProxyArgsAllowed());
		assertTrue(afterRetAdviceInfo.isFastProxyArgsAllowed());
		assertFalse(afterThrowAdviceInfo.isFastProxyArgsAllowed());

		args = afterRetAdviceInfo.getAdviceArguments();
		assertEquals(3, args.size());
		assertEquals(AdviceArgument.Kind.ARGUMENT, args.get(0).getKind());
		iarg = args.get(0).getInterceptedArgument();
		assertNotNull(iarg);
		assertEquals(0, iarg.getIdxInProxyArgsArray());
		assertEquals(0, iarg.getTargetMethodParameterIdx());
	}

	private static class SampleAdvice_CheckFast2 {

		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@Argument(2) double arg2, @Argument(0) Long arg0) {}

		@Advice(AdviceKind.AFTER_RETURN)
		// not fast
		void adviceAfterReturn(@Argument(2) double arg2) {}
	}

	private static class SampleAdvice_CheckFast3 {
		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@Argument(2) double arg2) {}
	}

	private static class SampleAdvice_CheckFast4 {
		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@Argument(1) String arg1) {}
	}

	@Test
	public void testPrepareCheckFast2() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CheckFast2.class);
		List<AdviceArgument> args;
		InterceptedArgument iarg;

		assertNotNull(ctx);
		assertEquals(2, ctx.getAdviceInfoByKind().size());
		List<WeavingAdviceInfo> beforeAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE);
		List<WeavingAdviceInfo> afterRetAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_RETURN);
		assertEquals(1, beforeAdvicesInfo.size());
		assertEquals(1, afterRetAdvicesInfo.size());

		WeavingAdviceInfo beforeAdviceInfo = beforeAdvicesInfo.get(0);
		WeavingAdviceInfo afterRetAdviceInfo = afterRetAdvicesInfo.get(0);

		assertTrue(beforeAdviceInfo.isFastProxyArgsAllowed());
		assertFalse(afterRetAdviceInfo.isFastProxyArgsAllowed());

		args = beforeAdviceInfo.getAdviceArguments();
		assertEquals(2, args.size());
		assertEquals(AdviceArgument.Kind.ARGUMENT, args.get(0).getKind());
		assertEquals(AdviceArgument.Kind.ARGUMENT, args.get(1).getKind());
		iarg = args.get(0).getInterceptedArgument();
		assertEquals(0, iarg.getIdxInProxyArgsArray());
		assertEquals(2, iarg.getTargetMethodParameterIdx());
		iarg = args.get(1).getInterceptedArgument();
		assertEquals(1, iarg.getIdxInProxyArgsArray());
		assertEquals(0, iarg.getTargetMethodParameterIdx());
	}

	@Test
	public void testPrepareCheckFast23() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CheckFast2.class, SampleAdvice_CheckFast3.class);

		List<WeavingAdviceInfo> beforeAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE);
		assertEquals(2, beforeAdvicesInfo.size());
		assertEquals(SampleAdvice_CheckFast2.class, beforeAdvicesInfo.get(0).getAdvice().getDeclaringClass());
		assertTrue(beforeAdvicesInfo.get(0).isFastProxyArgsAllowed());
		assertFalse(beforeAdvicesInfo.get(1).isFastProxyArgsAllowed());
	}

	@Test
	public void testPrepareCheckFast24() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CheckFast2.class, SampleAdvice_CheckFast4.class);

		List<WeavingAdviceInfo> beforeAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE);
		for (WeavingAdviceInfo info : beforeAdvicesInfo) {
			assertFalse(info.isFastProxyArgsAllowed());
		}
	}

	private static class SampleAdvice_Empty {
	}

	@Test
	public void testPrepareEmpty1() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_Empty.class);
		assertTrue(ctx.getAdviceInfoByKind().isEmpty());
	}

	@Test
	public void testPrepareEmpty2() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target);
		assertTrue(ctx.getAdviceInfoByKind().isEmpty());
	}

	private static class SampleAdvice_Incompatible {
		@Advice(AdviceKind.BEFORE)
		void arg0(@ModifiableArgument(4) Object[] arg0) {}
	}

	@Test
	public void testPrepareIncompatible() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_Incompatible.class);
		assertTrue(ctx.getAdviceInfoByKind().isEmpty());
	}

	private static class SampleAdvice_Generic {

		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@This Object target, @AllArguments(modifiable = true) Object[] args) {}

		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal
		Object adviceAfterReturn(@This Object target, @AllArguments Object[] args, @RetVal Object retVal) {
			return retVal;
		}

		@Advice(AdviceKind.AFTER_THROW)
		void adviceAfterThrow(@This Object target, @AllArguments Object[] args, @Thrown Throwable ex) {}
	}

	@Test
	public void testPrepareGeneric() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_Generic.class,
				SampleAdvice_Incompatible.class, SampleAdvice_Empty.class);
		List<AdviceArgument> args;

		assertNotNull(ctx);
		assertEquals(3, ctx.getAdviceInfoByKind().size());
		List<WeavingAdviceInfo> beforeAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE);
		List<WeavingAdviceInfo> afterRetAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_RETURN);
		List<WeavingAdviceInfo> afterThrowAdvicesInfo = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_THROW);
		assertEquals(1, beforeAdvicesInfo.size());
		assertEquals(1, afterRetAdvicesInfo.size());
		assertEquals(1, afterThrowAdvicesInfo.size());

		WeavingAdviceInfo beforeAdviceInfo = beforeAdvicesInfo.get(0);
		WeavingAdviceInfo afterRetAdviceInfo = afterRetAdvicesInfo.get(0);
		WeavingAdviceInfo afterThrowAdviceInfo = afterThrowAdvicesInfo.get(0);

		assertEquals(AdviceKind.BEFORE, beforeAdviceInfo.getAdviceKind());
		assertEquals("adviceBefore", beforeAdviceInfo.getAdvice().getName());
		assertFalse(beforeAdviceInfo.hasOverrideRetVal());
		assertFalse(beforeAdviceInfo.isFastProxyArgsAllowed());
		args = beforeAdviceInfo.getAdviceArguments();
		assertEquals(2, args.size());
		assertEquals(AdviceArgument.Kind.THIS, args.get(0).getKind());
		assertNull(args.get(0).getInterceptedArgument());
		assertFalse(args.get(0).isModifiable());
		assertEquals(AdviceArgument.Kind.ALL_ARGUMENTS, args.get(1).getKind());
		assertTrue(args.get(1).isModifiable());
		assertNull(args.get(1).getInterceptedArgument());

		assertEquals(AdviceKind.AFTER_RETURN, afterRetAdviceInfo.getAdviceKind());
		assertEquals("adviceAfterReturn", afterRetAdviceInfo.getAdvice().getName());
		assertTrue(afterRetAdviceInfo.hasOverrideRetVal());
		assertFalse(afterRetAdviceInfo.isFastProxyArgsAllowed());
		args = afterRetAdviceInfo.getAdviceArguments();
		assertEquals(3, args.size());
		assertEquals(AdviceArgument.Kind.THIS, args.get(0).getKind());
		assertNull(args.get(0).getInterceptedArgument());
		assertFalse(args.get(0).isModifiable());
		assertEquals(AdviceArgument.Kind.ALL_ARGUMENTS, args.get(1).getKind());
		assertFalse(args.get(1).isModifiable());
		assertNull(args.get(1).getInterceptedArgument());
		assertEquals(AdviceArgument.Kind.RETVAL, args.get(2).getKind());
		assertFalse(args.get(2).isModifiable());
		assertNull(args.get(2).getInterceptedArgument());
	}

	private WeavingContext doPrepareWeavingContext(Method target, Class<?>...adviceClasses) {
		List<String> adviceDescs = new ArrayList<>();
		Map<String, Class<?>> adviceClassesByDesc = new HashMap<>();
		for (Class<?> adviceClass : adviceClasses) {
			String desc = ":" + adviceClass.getName();
			adviceDescs.add(desc);
			adviceClassesByDesc.put(desc, adviceClass);
		}

		return uut.prepareMethodAdvicesWeaving(adviceDescs, adviceClassesByDesc,
				Type.getArgumentTypes(target), Type.getReturnType(target),
				target.getDeclaringClass().getName(), target.getName());
	}

	//
	// Tests for before() and afterX() proxying
	//

	static Capture<Object[]> argsCap = Capture.newInstance(CaptureType.ALL);

	private static class SampleAdvice_CapturingAllBefore {
		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@AllArguments Object[] args) {
			argsCap.setValue(args);
		}
	}

	private static class SampleAdvice_CapturingFastAfter {
		@Advice(AdviceKind.AFTER_RETURN)
		@OverrideRetVal
		Object adviceAfterReturn(@Argument(0) Long arg0, @Argument(1) String arg1, @Argument(2) double arg2) {
			argsCap.setValue(new Object[]{arg0, arg1, arg2});
			return 123;
		}
	}

	private static class SampleAdvice_AfterNoRet {
		@Advice(AdviceKind.AFTER_RETURN)
		void adviceAfterReturn() {
		}
	}

	@Test
	public void testProxy_NoRet() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target,
				SampleAdvice_AfterNoRet.class);
		Object[] args = {};
		Object retVal = uut.afterReturn(ctx.getJoinpointId(), null, this, args, 1);
		assertEquals(1, retVal);
	}

	@Test
	public void testProxy_OverrideRet() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target,
				SampleAdvice_AfterNoRet.class, SampleAdvice_CapturingFastAfter.class, SampleAdvice_AfterNoRet.class);
		Object[] args = {1L, "2", 3.0};
		Object retVal = uut.afterReturn(ctx.getJoinpointId(), null, this, args, 1);
		assertEquals(123, retVal);
	}

	@Test
	public void testProxy_Fast() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target,
				SampleAdvice_CapturingAllBefore.class, SampleAdvice_CapturingFastAfter.class);
		assertTrue(ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE).get(0).isFastProxyArgsAllowed());
		assertTrue(ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_RETURN).get(0).isFastProxyArgsAllowed());

		argsCap.reset();

		Object[] args = {1L, "2", 3.0};
		Map<Class<?>, Object> adviceInstances = uut.before(ctx.getJoinpointId(), this, args);
		assertNotNull(adviceInstances);
		assertEquals(1, adviceInstances.size());
		Object adviceInstance = adviceInstances.get(SampleAdvice_CapturingAllBefore.class);
		assertTrue(adviceInstance instanceof SampleAdvice_CapturingAllBefore);

		Object retVal = uut.afterReturn(ctx.getJoinpointId(), adviceInstances, this, args, 1);
		assertEquals(123, retVal);

		assertTrue(argsCap.hasCaptured());
		Object[] captured;
		List<Object[]> allCaptured = argsCap.getValues();
		assertEquals(2, allCaptured.size());
		captured = allCaptured.get(0);
		assertEquals(3, captured.length);
		assertEquals(1L, captured[0]);
		assertEquals("2", captured[1]);
		assertEquals(3.0, captured[2]);

		captured = allCaptured.get(1);
		assertEquals(3, captured.length);
		assertEquals(1L, captured[0]);
		assertEquals("2", captured[1]);
		assertEquals(3.0, captured[2]);
	}

	private static class SampleAdvice_CapturingMod1 {
		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@AllArguments(modifiable = true) Object[] args) {
			argsCap.setValue(new Object[]{args[0], args[1], args[2]});
			args[0] = 1 + (Long)args[0];
			args[1] = "mod1_" + args[1];
			args[2] = 1.1 + (Double)args[2];
		}
	}

	private static class SampleAdvice_CapturingMod2 {

		@Advice(AdviceKind.BEFORE)
		static void adviceBefore(@ModifiableArgument(0) Long[] arg0, @ModifiableArgument(1) String[] arg1,
		                  @ModifiableArgument(2) double[] arg2) {
			argsCap.setValue(new Object[]{arg0[0], arg1[0], arg2[0]});
			arg0[0] = 20 + (Long)arg0[0];
			arg1[0] = "mod2_" + arg1[0];
			arg2[0] = 20.0 + (Double)arg2[0];
		}
	}

	private static class SampleAdvice_CapturingMod3 {

		@Advice(AdviceKind.BEFORE)
		void adviceBefore(@ModifiableArgument(1) Object[] arg1, @ModifiableArgument(0) Object[] arg0,
		                  @ModifiableArgument(2) Object[] arg2) {
			argsCap.setValue(new Object[]{arg0[0], arg1[0], arg2[0]});
			arg0[0] = 300 + (Long)arg0[0];
			arg1[0] = "mod3_" + arg1[0];
			arg2[0] = 300.0 + (Double)arg2[0];
		}
	}

	@Test
	public void testProxy_Mod1() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CapturingMod1.class,
				SampleAdvice_CapturingAllBefore.class);
		argsCap.reset();

		Object[] args = {1L, "2", 3.0};
		Map<Class<?>, Object> adviceInstances = uut.before(ctx.getJoinpointId(), this, args);
		assertTrue(argsCap.hasCaptured());
		Object[] captured;
		List<Object[]> allCaptured = argsCap.getValues();
		assertEquals(2, allCaptured.size());
		captured = allCaptured.get(0);
		assertEquals(3, captured.length);
		assertEquals(1L, captured[0]);
		assertEquals("2", captured[1]);
		assertEquals(3.0, captured[2]);

		captured = allCaptured.get(1);
		assertEquals(3, captured.length);
		assertEquals(2L, captured[0]);
		assertEquals("mod1_2", captured[1]);
		assertEquals(4.1, captured[2]);
	}

	@Test
	public void testProxy_Mod2() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CapturingMod2.class,
				SampleAdvice_CapturingAllBefore.class);
		argsCap.reset();

		Object[] args = {1L, "2", 3.0};
		Map<Class<?>, Object> adviceInstances = uut.before(ctx.getJoinpointId(), this, args);
		assertTrue(argsCap.hasCaptured());
		Object[] captured;
		List<Object[]> allCaptured = argsCap.getValues();
		assertEquals(2, allCaptured.size());
		captured = allCaptured.get(0);
		assertEquals(3, captured.length);
		assertEquals(1L, captured[0]);
		assertEquals("2", captured[1]);
		assertEquals(3.0, captured[2]);

		captured = allCaptured.get(1);
		assertEquals(3, captured.length);
		assertEquals(21L, captured[0]);
		assertEquals("mod2_2", captured[1]);
		assertEquals(23.0, captured[2]);
	}

	@Test
	public void testProxy_Mod3() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CapturingMod3.class,
				SampleAdvice_CapturingAllBefore.class);
		argsCap.reset();

		Object[] args = {"2", 1L, 3.0};
		Map<Class<?>, Object> adviceInstances = uut.before(ctx.getJoinpointId(), this, args);
		assertTrue(argsCap.hasCaptured());
		Object[] captured;
		List<Object[]> allCaptured = argsCap.getValues();
		assertEquals(2, allCaptured.size());
		captured = allCaptured.get(0);
		assertEquals(3, captured.length);
		assertEquals(1L, captured[0]);
		assertEquals("2", captured[1]);
		assertEquals(3.0, captured[2]);

		captured = allCaptured.get(1);
		assertEquals(3, captured.length);
		assertEquals("mod3_2", captured[0]);
		assertEquals(301L, captured[1]);
		assertEquals(303.0, captured[2]);
	}

	@Test
	public void testProxy_ModAll() {
		Method target = TestUtils.findMethod(this.getClass(), "target");
		WeavingContext ctx = doPrepareWeavingContext(target, SampleAdvice_CapturingMod1.class,
				SampleAdvice_CapturingMod2.class, SampleAdvice_CapturingMod3.class,
				SampleAdvice_CapturingAllBefore.class);
		argsCap.reset();

		Object[] args = {1L, "2", 3.0};
		Map<Class<?>, Object> adviceInstances = uut.before(ctx.getJoinpointId(), this, args);
		assertTrue(argsCap.hasCaptured());
		Object[] captured;
		List<Object[]> allCaptured = argsCap.getValues();
		assertEquals(4, allCaptured.size());

		captured = allCaptured.get(1);
		assertEquals(3, captured.length);
		assertEquals(2L, captured[0]);
		assertEquals("mod1_2", captured[1]);
		assertEquals(4.1, captured[2]);

		captured = allCaptured.get(2);
		assertEquals(3, captured.length);
		assertEquals(22L, captured[0]);
		assertEquals("mod2_mod1_2", captured[1]);
		assertEquals(24.1, captured[2]);

		captured = allCaptured.get(3);
		assertEquals(3, captured.length);
		assertEquals(322L, captured[0]);
		assertEquals("mod3_mod2_mod1_2", captured[1]);
		assertEquals(324.1, captured[2]);
	}

}

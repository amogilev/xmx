// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.TypeSpec;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTypeMatcher {

	private class Foo {}

	Class<?>[] matchClasses = {int.class, long[].class, double[][][].class, Integer.class, Integer[].class, Foo.class};

	private void checkMatch(Class<?> expectedMatch, TypeMatcher uut) {
		checkMatchClassSpec(expectedMatch, uut);
		checkMatchDescSpec(expectedMatch, uut);
	}

	private void checkMatchClassSpec(Class<?> expectedMatch, TypeMatcher uut) {
		assertTrue(uut.matches(TypeSpec.of(expectedMatch)));
		for (Class<?> matchClass : matchClasses) {
			assertEquals(matchClass == expectedMatch, uut.matches(TypeSpec.of(matchClass)));
		}
	}

	private void checkMatchDescSpec(Class<?> expectedMatch, TypeMatcher uut) {
		assertTrue(uut.matches(TypeSpec.of(Type.getType(expectedMatch).getDescriptor())));
		for (Class<?> matchClass : matchClasses) {
			assertEquals(matchClass == expectedMatch, uut.matches(TypeSpec.of(
					Type.getType(matchClass).getDescriptor())));
		}
	}

	@Test
	public void testMatchPrimitives() {
		for (boolean fq : Arrays.asList(false, true)) {
			checkMatch(int.class, new TypeMatcher(fq, "int", 0));
			checkMatch(long[].class, new TypeMatcher(fq, "long", 1));
			checkMatch(double[][][].class, new TypeMatcher(fq, "double", 3));
		}
	}

	@Test
	public void testMatchQualified() {
		checkMatch(Integer.class, new TypeMatcher(true, "java.lang.Integer", 0));
		checkMatch(Integer[].class, new TypeMatcher(true, "java.lang.Integer", 1));
		checkMatch(Foo.class, new TypeMatcher(true,
				"com.gilecode.xmx.cfg.pattern.impl.TestTypeMatcher$Foo", 0));
	}

	@Test
	public void testMatchUnqualified() {
		checkMatch(Integer.class, new TypeMatcher(false, "Integer", 0));
		checkMatch(Integer[].class, new TypeMatcher(false, "Integer", 1));
		checkMatch(Foo.class, new TypeMatcher(false, "TestTypeMatcher$Foo", 0));
	}

	@Test
	public void testMatchExplicitDesc() {
		TypeMatcher uut;

		uut = new TypeMatcher(false, "int", 0);
		assertTrue(uut.matches(TypeSpec.of("I")));

		uut = new TypeMatcher(false, "long", 2);
		assertTrue(uut.matches(TypeSpec.of("[[J")));

		uut = new TypeMatcher(true, "com.gilecode.xmx.cfg.pattern.impl.TestTypeMatcher$Foo", 0);
		assertTrue(uut.matches(TypeSpec.of("Lcom/gilecode/xmx/cfg/pattern/impl/TestTypeMatcher$Foo;")));
	}
}

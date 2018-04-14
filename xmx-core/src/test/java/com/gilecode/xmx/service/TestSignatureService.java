// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class TestSignatureService {

	private SignatureService uut = new SignatureService();

	@Test
	public void testGetTypeSignature_Primitives() {
		assertEquals("Z", uut.getTypeSignature(boolean.class));
		assertEquals("B", uut.getTypeSignature(byte.class));
		assertEquals("C", uut.getTypeSignature(char.class));
		assertEquals("S", uut.getTypeSignature(short.class));
		assertEquals("I", uut.getTypeSignature(int.class));
		assertEquals("J", uut.getTypeSignature(long.class));
		assertEquals("F", uut.getTypeSignature(float.class));
		assertEquals("D", uut.getTypeSignature(double.class));
		assertEquals("V", uut.getTypeSignature(void.class));

		assertEquals("I", uut.getTypeSignature(Integer.TYPE));
	}

	private class LocalClass {
		String foo(int[][] arg1, String...arg2) {
			return null;
		}
	}

	@Test
	public void testGetTypeSignature_Classes() {
		assertEquals("Ljava/lang/String;", uut.getTypeSignature(String.class));
		assertEquals("Lcom/gilecode/xmx/service/TestSignatureService$LocalClass;",
				uut.getTypeSignature(LocalClass.class));
	}

	@Test
	public void testGetTypeSignature_Arrays() {
		assertEquals("[Ljava/lang/String;", uut.getTypeSignature(String[].class));
		assertEquals("[[I", uut.getTypeSignature(int[][].class));
	}

	@Test
	public void testFindTypeBySignature_Primitives() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		assertEquals(boolean.class, uut.findTypeBySignature(cl, "Z"));
		assertEquals(byte.class, uut.findTypeBySignature(cl, "B"));
		assertEquals(char.class, uut.findTypeBySignature(cl, "C"));
		assertEquals(short.class, uut.findTypeBySignature(cl, "S"));
		assertEquals(int.class, uut.findTypeBySignature(cl, "I"));
		assertEquals(long.class, uut.findTypeBySignature(cl, "J"));
		assertEquals(float.class, uut.findTypeBySignature(cl, "F"));
		assertEquals(double.class, uut.findTypeBySignature(cl, "D"));
		assertEquals(void.class, uut.findTypeBySignature(cl, "V"));
	}

	@Test
	public void testFindTypeBySignature_Classes() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		assertEquals(String.class, uut.findTypeBySignature(cl, "Ljava/lang/String;"));
		assertEquals(LocalClass.class, uut.findTypeBySignature(cl,
				"Lcom/gilecode/xmx/service/TestSignatureService$LocalClass;"));
	}

	@Test
	public void testFindTypeBySignature_Arrays() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		assertEquals(String[].class, uut.findTypeBySignature(cl, "[Ljava/lang/String;"));
		assertEquals(int[][].class, uut.findTypeBySignature(cl, "[[I"));
	}

	@Test
	public void testGetMethodSignature() throws NoSuchMethodException {
		Method m = LocalClass.class.getDeclaredMethods()[0];
		assertEquals("com.gilecode.xmx.service.TestSignatureService$LocalClass.foo([[I[Ljava/lang/String;)",
				uut.getMethodSignature(m));
		assertEquals("java.lang.String.valueOf(J)", uut.getMethodSignature(
				String.class.getDeclaredMethod("valueOf", long.class)));
		assertEquals("java.lang.Object.hashCode()", uut.getMethodSignature(
				Object.class.getDeclaredMethod("hashCode")));
	}

	@Test
	public void testFindMethodBySignature() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		Method m;

		m = uut.findMethodBySignature(cl,
				"com.gilecode.xmx.service.TestSignatureService$LocalClass.foo([[I[Ljava/lang/String;)");
		assertEquals("foo", m.getName());

		m = uut.findMethodBySignature(cl,
				"java.lang.String.valueOf(J)");
		assertEquals("public static java.lang.String java.lang.String.valueOf(long)", m.toString());

		m = uut.findMethodBySignature(cl,
				"java.lang.Object.hashCode()");
		assertEquals("public native int java.lang.Object.hashCode()", m.toString());
	}

	@Test(expected = ClassNotFoundException.class)
	public void testFindMethodBySignature_MissingClass() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		uut.findMethodBySignature(cl,
				"com.gilecode.xmx.service.TestSignatureService$LocalClass2.foo([[I[Ljava/lang/String;)");
	}

	@Test(expected = NoSuchMethodException.class)
	public void testFindMethodBySignature_MissingMethod() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader();
		uut.findMethodBySignature(cl,
				"com.gilecode.xmx.service.TestSignatureService$LocalClass.foo()");
	}
}
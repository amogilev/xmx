// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SignatureService implements ISignatureService {

	private static final Map<Class<?>, String> primitiveTypeSignatures;
	private static final Map<String, Class<?>> primitiveTypeBySignature;
	static {
		primitiveTypeSignatures = new HashMap<>();
		primitiveTypeBySignature = new HashMap<>();

		primitiveTypeSignatures.put(boolean.class, "Z");
		primitiveTypeSignatures.put(byte.class, "B");
		primitiveTypeSignatures.put(char.class, "C");
		primitiveTypeSignatures.put(short.class, "S");
		primitiveTypeSignatures.put(int.class, "I");
		primitiveTypeSignatures.put(long.class, "J");
		primitiveTypeSignatures.put(float.class, "F");
		primitiveTypeSignatures.put(double.class, "D");
		primitiveTypeSignatures.put(void.class, "V");

		for (Map.Entry<Class<?>, String> e : primitiveTypeSignatures.entrySet()) {
			primitiveTypeBySignature.put(e.getValue(), e.getKey());
		}
	}

	@Override
	public String getTypeSignature(Class<?> c) {
		StringBuilder sb = new StringBuilder();
		appendTypeSignature(sb, c);
		return sb.toString();
	}

	private void appendTypeSignature(StringBuilder sb, Class<?> c) {
		while (c.isArray()) {
			sb.append('[');
			c = c.getComponentType();
		}
		if (c.isPrimitive()) {
			sb.append(primitiveTypeSignatures.get(c));
		} else {
			sb.append('L').append(c.getName().replace('.', '/')).append(';');
		}
	}

	private Class<?> parseNextType(String s, AtomicInteger nextPos, ClassLoader cl) throws ClassNotFoundException {
		int i = nextPos.get();
		int nArrays = 0;
		for ( ; i < s.length() && s.charAt(i) == '['; i++)
			nArrays++;

		if (i >= s.length()) {
			throw new IllegalArgumentException("Wrong type signature: " + s.substring(nextPos.get()));
		}

		int ch = s.charAt(i);
		Class<?> component = null;
		int end;
		if (ch == 'L') {
			int iSemicolon = s.indexOf(';', i + 1);
			if (iSemicolon < 0) {
				throw new IllegalArgumentException("Wrong type signature: " + s.substring(nextPos.get()));
			}
			end = iSemicolon + 1;
			String name = s.substring(i + 1, iSemicolon).replace('/', '.');
			component = Class.forName(name, false, cl);
		} else {
			end = i + 1;
			String primitiveSignature = s.substring(i, end);
			component = primitiveTypeBySignature.get(primitiveSignature);
			if (component == null) {
				throw new IllegalArgumentException("Wrong type signature: " + primitiveSignature);
			}
		}
		Class<?> result = component;
		if (nArrays > 0) {
			result = Array.newInstance(result, new int[nArrays]).getClass();
		}
		nextPos.set(end);
		return result;
	}

	@Override
	public Class<?> findTypeBySignature(ClassLoader cl, String typeSignature) throws ClassNotFoundException {
		AtomicInteger i = new AtomicInteger(0);
		Class<?> result = parseNextType(typeSignature, i, cl);
		if (i.get() != typeSignature.length()) {
			throw new IllegalArgumentException("Wrong type signature: " + typeSignature);
		}
		return result;
	}

	@Override
	public String getMethodSignature(Method m) {
		StringBuilder sb = new StringBuilder(128);
		sb.append(m.getDeclaringClass().getName())
			.append('.')
			.append(m.getName())
			.append('(');
		for (Class<?> paramType : m.getParameterTypes()) {
			appendTypeSignature(sb, paramType);
		}
		sb.append(')');
		return sb.toString();
	}

	@Override
	public Method findMethodBySignature(ClassLoader cl, String methodSignature) throws ClassNotFoundException, NoSuchMethodException {
		String s = methodSignature;
		int iLastDot = s.lastIndexOf('.');
		if (iLastDot < 0) {
			throw new IllegalArgumentException("Wrong method signature (no declaring class): " + methodSignature);
		}
		String className = s.substring(0, iLastDot);
		Class<?> declaringClass = Class.forName(className, false, cl);
		int iPar = s.indexOf('(', iLastDot);
		if (iPar < 0) {
			throw new IllegalArgumentException("Wrong method signature (no parenthesis): " + methodSignature);
		}
		String name = s.substring(iLastDot + 1, iPar);
		Class<?>[] parameterTypes = parseParameterTypes(s, iPar + 1, declaringClass.getClassLoader());
		return declaringClass.getDeclaredMethod(name, parameterTypes);
	}

	private Class<?>[] parseParameterTypes(String s, int from, ClassLoader cl) throws ClassNotFoundException {
		if (from >= s.length() || s.charAt(from) == ')') {
			return new Class<?>[0];
		}
		List<Class<?>> types = new ArrayList<>();
		AtomicInteger nextPos = new AtomicInteger(from);
		while (nextPos.get() + 1 < s.length()) {
			types.add(parseNextType(s, nextPos, cl));
		}
		if (s.charAt(nextPos.get()) != ')') {
			throw new IllegalArgumentException("Wrong method signature: " + s);
		}
		return types.toArray(new Class[0]);
	}
}

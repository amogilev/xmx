// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.data.AdviceClassInfo;
import com.gilecode.xmx.aop.data.MethodDeclarationInfo;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AopTestUtils {

	/**
	 * Find declared method by a unique name. (no arg types required)
	 */
	public static Method findMethod(Class<?> c, String uniqueMethodName) {
		Method[] methods = c.getDeclaredMethods();
		Method found = null;
		for (Method m : methods) {
			if (m.getName().equals(uniqueMethodName)) {
				assertNull("Expected unique method name", found);
				found = m;
			}
		}
		assertNotNull("Method not found: " + uniqueMethodName, found);
		return found;
	}

	/**
	 * Find declared method by a unique name. (no arg types required)
	 */
	public static MethodDeclarationInfo findMethodDeclaration(Class<?> c, String uniqueMethodName) throws IOException {
		List<MethodDeclarationInfo> methodDeclarations = getMethodDeclarations(c);
		MethodDeclarationInfo found = null;
		for (MethodDeclarationInfo m : methodDeclarations) {
			if (m.getMethodName().equals(uniqueMethodName)) {
				assertNull("Expected unique method name", found);
				found = m;
			}
		}
		assertNotNull("Method not found: " + uniqueMethodName, found);
		return found;
	}

	public static  WeavingContext prepareTestWeavingContext(XmxAopManager aopManager, final Method target,
	                                                        final Class<?>...adviceClasses) throws IOException {
		List<String> adviceDescs = new ArrayList<>();
		Map<String, AdviceClassInfo> adviceClassesByDesc = new HashMap<>();
		for (int i = 0; i < adviceClasses.length; i++) {
			Class<?> adviceClass = adviceClasses[i];
			String desc = ":" + adviceClass.getName();
			adviceDescs.add(desc);
			final int classIdx = i;
			WeakCachedSupplier<Class<?>> adviceClassSupplier = new WeakCachedSupplier<Class<?>>() {
				@Override
				protected Class<?> load() {
					return adviceClasses[classIdx];
				}
			};
			adviceClassesByDesc.put(desc, new AdviceClassInfo(adviceClassSupplier, getMethodDeclarations(adviceClass),
					desc));
		}


		WeakCachedSupplier<Class<?>> targetClassSupplier = new WeakCachedSupplier<Class<?>>() {
			@Override
			protected Class<?> load() {
				return target.getDeclaringClass();
			}
		};
		return aopManager.prepareMethodAdvicesWeaving(adviceDescs, adviceClassesByDesc,
				Type.getArgumentTypes(target), Type.getReturnType(target),
				target.getDeclaringClass().getName(), target.getName(), targetClassSupplier);
	}

	public static List<MethodDeclarationInfo> getMethodDeclarations(Class<?> adviceClass) throws IOException {
		String classDesc = "test:" + adviceClass.getName();
		try (InputStream classStream = getClassAsStream(adviceClass)) {
			return MethodDeclarationAsmReader.readMethodDeclarations(classStream, classDesc);
		}
	}

	public static InputStream getClassAsStream(Class<?> adviceClass) throws IOException {
		URL classFile = adviceClass.getClassLoader().getResource(Type.getInternalName(adviceClass) + ".class");
		assertNotNull("Broken test: class file not found", classFile);
		return classFile.openStream();
	}

}

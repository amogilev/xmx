// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
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

	public static  WeavingContext prepareTestWeavingContext(XmxAopManager aopManager, Method target, final Class<?>...adviceClasses) {
		List<String> adviceDescs = new ArrayList<>();
		Map<String, WeakCachedSupplier<Class<?>>> adviceClassesByDesc = new HashMap<>();
		for (int i = 0; i < adviceClasses.length; i++) {
			Class<?> adviceClass = adviceClasses[i];
			String desc = ":" + adviceClass.getName();
			adviceDescs.add(desc);
			final int classIdx = i;
			adviceClassesByDesc.put(desc, new WeakCachedSupplier<Class<?>>() {
				@Override
				protected Class<?> load() {
					return adviceClasses[classIdx];
				}
			});
		}


		return aopManager.prepareMethodAdvicesWeaving(adviceDescs, adviceClassesByDesc,
				Type.getArgumentTypes(target), Type.getReturnType(target),
				target.getDeclaringClass().getName(), target.getName());
	}

}

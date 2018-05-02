// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TestUtils {

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
}

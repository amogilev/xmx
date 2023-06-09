// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;

/**
 * Collection of utility methods for Reflection information -
 * Classes, Methods and Fields.
 * 
 * @author Andrey Mogilev
 */
public class ReflectionUtils {

	private final static Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

	public static Comparator<Class<?>> CLASSNAME_COMPARATOR = new Comparator<Class<?>>() {
		@Override
		public int compare(Class<?> c1, Class<?> c2) {
			return c1.getName().compareTo(c2.getName());
		}
	};
	
	public static Comparator<Method> METHOD_COMPARATOR = new Comparator<Method>() {
		@Override
		public int compare(Method m1, Method m2) {
			int cmp = m1.getName().compareTo(m2.getName());
			if (cmp != 0) {
				return cmp;
			}
			
			Class<?>[] params1 = m1.getParameterTypes();
			Class<?>[] params2 = m2.getParameterTypes();
			cmp = params1.length - params2.length; 
			for (int i = 0; i < params1.length && cmp == 0; i++) {
				cmp = CLASSNAME_COMPARATOR.compare(params1[i], params2[i]); 
			}
			
			return cmp;
		}
	};
	
	public static Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
		@Override
		public int compare(Field f1, Field f2) {
			return f1.getName().compareTo(f2.getName());
		}
	};

	public static Object safeFindInvokeMethod(Object obj, String className, String methodName) {
		Method m = safeFindMethod(obj, className, methodName);
		return safeInvokeMethod(m, obj);
	}


	public static Object safeFindInvokeMethodWithIgnoredExceptions(Object obj, String className, String methodName,
			Collection<? extends Class<? extends Exception>> ignored) {
		Method m = safeFindMethod(obj, className, methodName);
		return safeInvokeMethodWithIgnoredExceptions(m, ignored, obj);
	}

	public static <T> T safeFindGetField(Object obj, String className, String fieldName, Class<T> fieldClass) {
		Field f = safeFindField(obj, className, fieldName);
		if (f == null) {
			return null;
		}
		try {
			f.setAccessible(true);
			Object fieldObj = f.get(obj);
			return fieldClass.cast(fieldObj);
		} catch (Exception e) {
			logger.warn("Failed to get field " + f + " on obj " + obj, e);
			return null;
		}
	}

	public static Object safeInvokeMethod(Method m, Object obj, Object...args) {
		if (m == null) {
			return null;
		}
		try {
			return m.invoke(obj, args);
		} catch (Exception e) {
			logger.warn("Failed to invoke method " + m + " on obj " + obj, e);
			return null;
		}
	}

	public static Object safeInvokeMethodWithIgnoredExceptions(Method m, Collection<? extends Class<? extends Exception>> ignored,
			Object obj, Object...args) {
		if (m == null) {
			return null;
		}
		try {
			return m.invoke(obj, args);
		} catch (Exception e) {
			if (!ignored.contains(e.getClass())) {
				logger.warn("Failed to invoke method " + m + " on obj " + obj, e);
			}
			return null;
		}
	}

	public static Method safeFindMethod(Object obj, String className, String methodName, Class<?>...parameterTypes) {
		Class<?> c = findInstanceOrParentClass(obj, className);
		try {
			return c.getDeclaredMethod(methodName, parameterTypes);
		} catch (Exception e) {
			logger.warn("Failed to find method " + className + "::" + methodName + " on obj " + obj, e);
			return null;
		}
	}

	private static Class<?> findInstanceOrParentClass(Object obj, String className) {
		if (obj == null) {
			return null;
		}
		Class<?> c = obj.getClass();
		while (c != null && !c.getName().equals(className)) {
			c = c.getSuperclass();
		}
		return c;
	}

	public static Field safeFindField(Object obj, String className, String fieldName) {
		Class<?> c = findInstanceOrParentClass(obj, className);
		if (c == null) {
			return null;
		}

		try {
			return c.getDeclaredField(fieldName);
		} catch (Exception e) {
			logger.warn("Failed to find field " + className + "::" + fieldName + " on obj " + obj, e);
			return null;
		}
	}

	public static Method safeFindClassAndMethod(ClassLoader cl, String className, String methodName, Class<?>...parameterTypes) {
		Class<?> c = null;
		try {
			c = Class.forName(className, false, cl);
		} catch (ClassNotFoundException e) {
            logger.warn("Failed to find class " + className + " in the class loader " + cl);
			return null;
		}

		try {
			return c.getDeclaredMethod(methodName, parameterTypes);
		} catch (Exception e) {
			logger.warn("Failed to find method " + className + "::" + methodName, e);
			return null;
		}
	}

}

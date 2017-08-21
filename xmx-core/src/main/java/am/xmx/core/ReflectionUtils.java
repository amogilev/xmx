// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Collection of utility methods for Reflection information -
 * Classes, Methods and Fields.
 * 
 * @author Andrey Mogilev
 */
public class ReflectionUtils {
	
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

}

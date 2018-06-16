// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Modification of Asm ClassWriter which uses the specified class loader during the frame computation.
 * <br/>
 * As of Asm 6.2, this requires override of {@link #getCommonSuperClass(String, String)}. In future releases,
 * it shall be simplified to overriding of getClassLoader()
 */
public class ClassWriterWithCustomLoader extends ClassWriter {

	private final ClassLoader loader;

	public ClassWriterWithCustomLoader(int flags, ClassLoader loader) {
		super(flags);
		this.loader = loader;
	}

	public ClassWriterWithCustomLoader(ClassReader classReader, int flags, ClassLoader loader) {
		super(classReader, flags);
		this.loader = loader;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		ClassLoader classLoader = loader;
		Class<?> class1;
		try {
			class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
		} catch (Exception e) {
			throw new TypeNotPresentException(type1, e);
		}
		Class<?> class2;
		try {
			class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
		} catch (Exception e) {
			throw new TypeNotPresentException(type2, e);
		}
		if (class1.isAssignableFrom(class2)) {
			return type1;
		}
		if (class2.isAssignableFrom(class1)) {
			return type2;
		}
		if (class1.isInterface() || class2.isInterface()) {
			return "java/lang/Object";
		} else {
			do {
				class1 = class1.getSuperclass();
			} while (!class1.isAssignableFrom(class2));
			return class1.getName().replace('.', '/');
		}
	}
}

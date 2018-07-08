// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.params;

import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;
import com.gilecode.xmx.core.instrument.LocalVariableTableParamNamesExtractingClassVisitor;
import com.gilecode.xmx.service.IXmxCoreService;
import com.gilecode.xmx.service.XmxServiceRegistry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class ParamNamesFetcher implements IParamNamesFetcher {

	private final static Logger logger = LoggerFactory.getLogger(ParamNamesFetcher.class);

	public static final String[] EMPTY = new String[0];

	// TODO: maybe create array instead
	private final IParamNamesFetcher reflFetcher;
	private final IXmxCoreService coreService;

	public ParamNamesFetcher(IXmxCoreService coreService) {
		this.coreService = coreService;
		if (XmxServiceRegistry.getMajorJavaVersion() >= 8) {
			reflFetcher = tryCreateInstance("com.gilecode.xmx.core.params.j8.Java8ReflectionParamNamesFetcher");
		} else {
			reflFetcher = null;
		}
	}

	@Override
	public String[] getMethodParameterNames(Method m) {
		if (m.getParameterTypes().length == 0) {
			return EMPTY;
		}

		String[] result = null;
		if (reflFetcher != null) {
			result = reflFetcher.getMethodParameterNames(m);
		}
		if (result == null) {
			ManagedClassLoaderWeakRef clInfo = coreService.getOrInitManagedClassLoader(m.getDeclaringClass().getClassLoader());
			ParamNamesCache paramNamesCache = clInfo.getParamNamesCache();
			result = paramNamesCache.getParameterNames(m);

			String className = m.getDeclaringClass().getName();
			if (result == null && !paramNamesCache.isClassWithMissingInfo(className)) {
				if (tryReadParamNamesInfoFromClassFile(paramNamesCache, m.getDeclaringClass())) {
					result = paramNamesCache.getParameterNames(m);
				}
			}
		}
		return result;
	}

	private boolean tryReadParamNamesInfoFromClassFile(ParamNamesCache paramNamesCache, Class<?> c) {
		String className = c.getName();
		String classNameNoPackage = className.substring(1 + className.lastIndexOf('.'));
		InputStream is = c.getResourceAsStream(classNameNoPackage + ".class");
		if (is == null) {
			paramNamesCache.storeMissingClassInfo(className);
			return false;
		}
		try {
			ClassReader cr = new ClassReader(is);
			ClassVisitor cv = new LocalVariableTableParamNamesExtractingClassVisitor(className, paramNamesCache, true);
			cr.accept(cv, ClassReader.SKIP_FRAMES);
			return true;
		} catch (IOException e) {
			logger.warn("Failed to read class file for " + c, e);
			paramNamesCache.storeMissingClassInfo(className);
			return false;
		}
	}


	@SuppressWarnings("unchecked")
	private static <T> T tryCreateInstance(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			return (T)clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			return null;
		}
	}


}

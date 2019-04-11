// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.aop.data.*;
import com.gilecode.xmx.boot.IXmxAopService;
import com.gilecode.xmx.boot.XmxURLClassLoader;
import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;
import com.gilecode.xmx.model.XmxRuntimeException;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Manages loading of advice classes, weaving and invocation of advice methods.
 */
public class XmxAopManager extends BasicAdviceArgumentsProcessor implements IXmxAopService, IXmxAopLoader {

	private final static Logger logger = LoggerFactory.getLogger(XmxAopManager.class);

	private final AdviceVerifier adviceVerifier = new AdviceVerifier();
	private final AtomicInteger joinPointsCounter = new AtomicInteger(1);

	// NOTE: ideally, joinpoints with GC'ed target classes need to be periodically cleared. However, it seems that
	//   the extra efforts for implementing that (RefQueue, WeakRefs etc.) would be just useless overhead for
	//   the most of the applications.
	private final ConcurrentMap<Integer, WeavingContext> joinpointsWeavingInfo = new ConcurrentHashMap<>();

	/**
	 * The directory for advices JARs under XMX Home.
	 */
	private final File homeAdvicesDir;

	/**
	 * The directory for advices JARs under XMX Configuration.
	 */
	private final File configAdvicesDir;

	/**
	 * Creates the manager instance, with the specified home and config directories.
	 */
	public XmxAopManager(File homeDir, File configDir) {
		this.homeAdvicesDir = new File(homeDir, "lib/advices/");
		this.configAdvicesDir = new File(configDir, "advices/");
	}

	@Override
	public AdviceLoadResult loadAndVerifyAdvices(Collection<String> adviceDescs, ManagedClassLoaderWeakRef classLoaderRef) {
		Map<String, AdviceClassInfo> adviceClassesByDesc = new HashMap<>(adviceDescs.size());
		for (String desc : adviceDescs) {
			AdviceClassInfo adviceClassInfo = verifyAdviceClass(desc, classLoaderRef);
			if (adviceClassInfo != null) {
				adviceClassesByDesc.put(desc, adviceClassInfo);
			}
		}
		return new AdviceLoadResult(adviceClassesByDesc);
	}

	private static class TargetMethodSupplier extends WeakCachedSupplier<Method> {
		private final WeakCachedSupplier<Class<?>> classSupplier;
		private final String methodName;
		private final Type[] paramTypes;

		public TargetMethodSupplier(WeakCachedSupplier<Class<?>> classSupplier, String methodName, Type[] paramTypes) {
			this.classSupplier = classSupplier;
			this.methodName = methodName;
			this.paramTypes = paramTypes;
		}

		@Override
		protected Method load() throws BadAdviceException {
			Class<?> targetClass = classSupplier.get();
			if (targetClass == null) {
				return null;
			}
			for (Method m : targetClass.getDeclaredMethods()) {
				if (matches(m)) {
					return m;
				}
			}
			return null;
		}

		private boolean matches(Method m) {
			if (m.getName().equals(methodName)) {
				Class<?>[] parameterTypes = m.getParameterTypes();
				if (parameterTypes.length == paramTypes.length) {
					for (int i = 0; i < parameterTypes.length; i++) {
						Class<?> actualParamType = parameterTypes[i];
						Type expectedParamType = paramTypes[i];
						if (!Type.getType(actualParamType).equals(expectedParamType)) {
							return false;
						}
					}
					// found matching (by name and parameters)
					return true;
				}
			}
			return false;
		}
	}

	private class JarClassLoadingSupplier extends WeakCachedSupplier<Class<?>> {

		private final String jarName;
		private final String className;
		private final ManagedClassLoaderWeakRef targetCLRef;

		public JarClassLoadingSupplier(String jarName, String className, ManagedClassLoaderWeakRef targetCLRef) {
			this.jarName = jarName;
			this.className = className;
			this.targetCLRef = targetCLRef;
		}

		@Override
		protected Class<?> load() throws BadAdviceException {
			return loadClass(targetCLRef, jarName, className);
		}
	}

	private static class AdviceMethodSupplier extends WeakCachedSupplier<Method> {

		private final ISupplier<Class<?>> classSup;
		private final MethodDeclarationInfo methodInfo;

		public AdviceMethodSupplier(ISupplier<Class<?>> classSup, MethodDeclarationInfo methodInfo) {
			super();
			this.classSup = classSup;
			this.methodInfo = methodInfo;
		}

		@Override
		protected Method load() throws BadAdviceException {
			Class<?> adviceClass = classSup.get();
			Method[] declaredMethods = adviceClass.getDeclaredMethods();

			for (Method declaredMethod : declaredMethods) {
				if (methodMatches(declaredMethod)) {
					return declaredMethod;
				}
			}
			throw new BadAdviceException("Failed to match the advice method declaration with an actual method. " +
					"Probably the advice class has changed: " + adviceClass);
		}

		private boolean methodMatches(Method declaredMethod) {
			if (!declaredMethod.getName().equals(methodInfo.getMethodName())) {
				return false;
			}
			Class<?>[] declaredParamTypes = declaredMethod.getParameterTypes();
			AnnotatedTypeInfo[] params = methodInfo.getParameters();
			return parametersMatches(declaredParamTypes, params);
		}

		private boolean parametersMatches(Class<?>[] declaredParamTypes, AnnotatedTypeInfo[] params) {
			if (declaredParamTypes.length != params.length) {
				return false;
			}
			for (int i = 0; i < params.length; i++) {
				AnnotatedTypeInfo param = params[i];
				if (!param.getType().equals(Type.getType(declaredParamTypes[i]))) {
					return false;
				}
			}
			return true;
		}
	}

	private AdviceClassInfo verifyAdviceClass(String desc, ManagedClassLoaderWeakRef classLoaderRef) {
		AdviceClassInfo adviceClassInfo = classLoaderRef.getVerifiedAdvicesByDesc().get(desc);
		if (adviceClassInfo == null && classLoaderRef.getKnownBadAdviceDescs().contains(desc)) {
			return null;
		} else if (adviceClassInfo != null) {
			// found cached
			return adviceClassInfo;
		}

		// load class and verify it if loaded for the first time
		try {
			int n = desc.lastIndexOf(':');
			String jarName = n > 0 ? desc.substring(0, n) : null;
			String className = n >= 0 ? desc.substring(n + 1) : desc;

			if (jarName == null || className.isEmpty()) {
				throw new BadAdviceException("Missing JAR or class name");
			}

			List<MethodDeclarationInfo> adviceMethods;
			File f = getJarFile(jarName);
			try {
				JarFile jf = new JarFile(f);
				String entryName = className.replace('.', '/') + ".class";
				ZipEntry classEntry = jf.getJarEntry(entryName);
				if (classEntry == null) {
					throw new BadAdviceException("Entry " + entryName + " is not found in the JAR file");
				}

				try (InputStream is = jf.getInputStream(classEntry)) {
					adviceMethods = adviceVerifier.verifyAdviceClass(is, desc);
				}

			} catch (IOException e) {
				throw new BadAdviceException("Failed to read the class file from JAR", e);
			}

			// NOTE: the advice class shall not be loaded at this moment, as it may cause too early
			//   or duplicate loading of dependent classes!
			WeakCachedSupplier<Class<?>> sup = new JarClassLoadingSupplier(jarName, className, classLoaderRef);

			adviceClassInfo = new AdviceClassInfo(sup, adviceMethods, desc);
			classLoaderRef.getVerifiedAdvicesByDesc().put(desc, adviceClassInfo);
			return adviceClassInfo;

		} catch (BadAdviceException e) {
			classLoaderRef.getKnownBadAdviceDescs().add(desc);
			logger.warn("Bad advice '" + desc + "' is skipped!", e);
			return null;
		}
	}

	Class<?> loadClass(ManagedClassLoaderWeakRef classLoaderRef, String jarName, String className) throws BadAdviceException {
		ClassLoader adviceJarLoader = getOrCreateAdviceJarLoader(classLoaderRef, jarName);
		try {
			return Class.forName(className, true, adviceJarLoader);
		} catch (ClassNotFoundException e) {
			throw new BadAdviceException("Failed to load advice class " + className, e);
		}
	}

	private ClassLoader getOrCreateAdviceJarLoader(ManagedClassLoaderWeakRef classLoaderRef, String jarName) throws BadAdviceException {
		ConcurrentMap<String, ManagedClassLoaderWeakRef.SmartReference<ClassLoader>> adviceJarLoaders =
				classLoaderRef.getAdviceJarLoaders();
		ManagedClassLoaderWeakRef.SmartReference<ClassLoader> jarLoaderRef = adviceJarLoaders.get(jarName);
		ClassLoader jarLoader = jarLoaderRef == null ? null : jarLoaderRef.get();
		if (jarLoader == null) {
			URL jarUrl = toUrl(getJarFile(jarName));
			ClassLoader targetCL = classLoaderRef.get();
			ClassLoader newJarLoader = new XmxURLClassLoader(new URL[]{jarUrl}, targetCL);
			ManagedClassLoaderWeakRef.SmartReference<ClassLoader> newJarLoaderRef =
					classLoaderRef.createSmartReference(newJarLoader);
			logger.debug("Loaded advice JAR {} for target CL {}", jarUrl, targetCL);

			while (true) {
				jarLoaderRef = adviceJarLoaders.putIfAbsent(jarName, newJarLoaderRef);
				if (jarLoaderRef != null) {
					if ((jarLoader = jarLoaderRef.get()) != null) {
						// use the first CL added to adviceJarLoaders, remove the extra created smart reference
						classLoaderRef.removeSmartReference(jarLoaderRef);
						return jarLoader;
					} else if (!adviceJarLoaders.replace(jarName, jarLoaderRef, newJarLoaderRef)) {
						// the stored reference is GC'ed, but replacing it failed because of the concurrent change,
						//  so try again
						continue;
					}
				}
				// OK, use new created class loader
				return newJarLoader;
			}
		}
		return jarLoader;
	}

	private URL toUrl(File f) {
		try {
			return f.toURI().toURL();
		} catch (MalformedURLException e) {
			// not expected
			throw new XmxRuntimeException(e);
		}
	}

	/**
	 * Finds a JAR file by its name and return the file URL. If file is missing, throws exception.
	 * The JAR file is searched by name in XMX_HOME/lib/advices/ and XMX_CONFIG_HOME/advices
	 *
	 * @param jarName the name or path to advices JAR file
	 *
	 * @return the URL of the found JAR file
	 *
	 * @throws BadAdviceException if not found
	 */
	private File getJarFile(String jarName) throws BadAdviceException {
		List<File> candidateFiles = new ArrayList<>();
		boolean isPath = jarName.contains("/") || jarName.contains(File.separator);
		if (isPath) {
			// look by absolute path
			candidateFiles.add(new File(jarName));
		} else {
			// look in <xmx_home>/lib/advices and in <xmx_config>/advices
			candidateFiles.add(new File(homeAdvicesDir, jarName));
			candidateFiles.add(new File(configAdvicesDir, jarName));
		}
		for (File candidate : candidateFiles) {
			if (candidate.isFile()) {
				return candidate;
			}
		}
		// not found
		String message = "Jar file '" + jarName + "' is not found!";
		if (!isPath) {
			message += " It may be added either to " + homeAdvicesDir + " or to " + configAdvicesDir + " directory.";
		}
		throw new BadAdviceException(message);
	}

	@Override
	public WeavingContext prepareMethodAdvicesWeaving(Collection<String> adviceDescs,
	                                                  Map<String, AdviceClassInfo> adviceClassesByDesc,
	                                                  Type[] targetParamTypes, Type targetReturnType,
	                                                  String targetClassName, String targetMethodName,
	                                                  WeakCachedSupplier<Class<?>> targetClassSupplier) {
		WeakCachedSupplier<Method> targetMethodSupplier = new TargetMethodSupplier(targetClassSupplier,
				targetMethodName, targetParamTypes);
		WeavingContext ctx = new WeavingContext(joinPointsCounter.getAndIncrement(), targetMethodSupplier);
		Map<AdviceKind, List<WeavingAdviceInfo>> adviceInfoByKind = ctx.getAdviceInfoByKind();
		for (String desc : adviceDescs) {
			AdviceClassInfo adviceClassInfo = adviceClassesByDesc.get(desc);
			WeakCachedSupplier<Class<?>> adviceClassSup = adviceClassInfo.getClassSupplier();

			for (MethodDeclarationInfo advice : adviceClassInfo.getAdviceMethods()) {
				AnnotationInfo adviceAnnotation = advice.getAnnotation(Advice.class);
				if (adviceAnnotation == null || !adviceVerifier.isAdviceCompatibleMethod(advice,
						targetParamTypes, targetReturnType, targetClassName, targetMethodName)) {
					continue;
				}

				AdviceMethodSupplier adviceSup = new AdviceMethodSupplier(adviceClassSup, advice);
				AdviceKind adviceKind = adviceAnnotation.value();
				WeavingAdviceInfo adviceInfo = prepareWeaving(adviceSup, advice, adviceKind, ctx, targetParamTypes.length);

				List<WeavingAdviceInfo> compatibleAdvices = adviceInfoByKind.get(adviceKind);
				if (compatibleAdvices == null) {
					compatibleAdvices = new ArrayList<>(2);
					adviceInfoByKind.put(adviceKind, compatibleAdvices);
				}
				compatibleAdvices.add(adviceInfo);
			}
		}

		if (!adviceInfoByKind.isEmpty()) {
			joinpointsWeavingInfo.put(ctx.getJoinpointId(), ctx);
			int interceptedArgumentsCount = ctx.getInterceptedArguments().size();
			for (List<WeavingAdviceInfo> weavingAdviceInfos : adviceInfoByKind.values()) {
				for (WeavingAdviceInfo info : weavingAdviceInfos) {
					info.setFastProxyArgsAllowed(isFastProxyArgsAllowed(info.getAdviceArguments(), interceptedArgumentsCount));
				}
			}
		}
		return ctx;
	}

	// NOTE: advice class shall be verified and advice method shall be compatible! Checks are not duplicated!
	private WeavingAdviceInfo prepareWeaving(AdviceMethodSupplier adviceSup, MethodDeclarationInfo advice,
			AdviceKind adviceKind, WeavingContext ctx, int nTargetParams) {

		AnnotatedTypeInfo[] parameters = advice.getParameters();
		List<AdviceArgument> arguments = new ArrayList<>(parameters.length);
		boolean hasOverrideRetVal;

		for (AnnotatedTypeInfo parameter : parameters) {
			AnnotationInfo argAnnotation = findArgumentAnnotation(parameter);
			assert argAnnotation != null;

			Class<? extends Annotation> argAnnoClass = argAnnotation.getAnnotationClass();
			AdviceArgument arg;

			if (argAnnoClass == Argument.class || argAnnoClass == ModifiableArgument.class) {
				int argumentIdx = getArgumentIdx(argAnnotation);
				boolean modifiable = argAnnoClass == ModifiableArgument.class;
				arg = AdviceArgument.interceptedArgument(ctx.addInterceptedArgument(argumentIdx, modifiable), modifiable);
			} else if (argAnnoClass == AllArguments.class) {
				boolean modifiable = argAnnotation.isFlagSet("modifiable");
				arg = AdviceArgument.specialArgument(getAdviceArgumentKind(argAnnotation), modifiable);
				ctx.makeAllArgumentsIntercepted(modifiable, nTargetParams);
			} else {
				arg = AdviceArgument.specialArgument(getAdviceArgumentKind(argAnnotation));
			}
			arguments.add(arg);
		}

		// check @OverrideRetVal if present
		hasOverrideRetVal = advice.getAnnotation(OverrideRetVal.class) != null;
		return new WeavingAdviceInfo(ctx, adviceSup, adviceKind, arguments, hasOverrideRetVal, advice.getMethodDesc());
	}

	private static boolean isFastProxyArgsAllowed(List<AdviceArgument> adviceArguments, int interceptedArgumentsCount) {
		if (adviceArguments.size() == 1 && adviceArguments.get(0).getKind() == AdviceArgument.Kind.ALL_ARGUMENTS &&
				!adviceArguments.get(0).isModifiable()) {
			return true;
		}
		if (interceptedArgumentsCount != adviceArguments.size()) {
			return false;
		}
		for (int i = 0; i < adviceArguments.size(); i++) {
			AdviceArgument arg = adviceArguments.get(i);
			switch (arg.getKind()) {
			case ARGUMENT:
				InterceptedArgument interceptedArgument = arg.getInterceptedArgument();
				if (interceptedArgument.getIdxInProxyArgsArray() != i || arg.isModifiable()) {
					return false;
				}
				break;
			default:
				return false;
			}
		}
		return true;
	}

	public AdviceVerifier getAdviceVerifier() {
		return adviceVerifier;
	}

	//
	// IXmxAopService implementation
	//

	@Override
	public Map<Class<?>, Object> before(int joinPointId, Object thisArg, Object[] interestedArgs) {
		WeavingContext ctx = joinpointsWeavingInfo.get(joinPointId);
		List<WeavingAdviceInfo> advices = ctx.getAdviceInfoByKind().get(AdviceKind.BEFORE);
		Map<Class<?>, Object> adviceInstances = new HashMap<>(advices.size());
		for (WeavingAdviceInfo adviceInfo : advices) {
			try {
				Method advice = adviceInfo.getAdvice();
				List<AdviceArgument> argInfos = adviceInfo.getAdviceArguments();
				Object[] adviceArgs = fillAdviceArguments(adviceInfo, interestedArgs, thisArg, null);
				Object adviceInstance = getOrCreateAdviceInstance(adviceInstances, advice);
				advice.invoke(adviceInstance, adviceArgs);
				for (int i = 0; i < argInfos.size(); i++) {
					AdviceArgument argInfo = argInfos.get(i);
					switch (argInfo.getKind()) {
						case ARGUMENT:
							if (argInfo.isModifiable()) {
								interestedArgs[argInfo.getInterceptedArgument().getIdxInProxyArgsArray()] = Array.get(adviceArgs[i], 0);
							}
							break;
						case ALL_ARGUMENTS:
							if (argInfo.isModifiable()) {
								assert adviceArgs[i] instanceof Object[];
								assert ((Object[])adviceArgs[i]).length == interestedArgs.length;
								System.arraycopy((Object[]) adviceArgs[i], 0, interestedArgs, 0, interestedArgs.length);
							}
							break;
					}
				}
			} catch (Exception e) {
				logger.warn("Failed to invoke advice " + adviceInfo.getAdvice(), e);
			}
		}
		return adviceInstances;
	}

	@Override
	public Object afterReturn(int joinPointId, Map<Class<?>, Object> adviceInstances,
	                          Object thisArg, Object[] interestedArgs, Object retVal) {
		WeavingContext ctx = joinpointsWeavingInfo.get(joinPointId);
		List<WeavingAdviceInfo> advices = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_RETURN);
		for (WeavingAdviceInfo adviceInfo : advices) {
			try {
				Method advice = adviceInfo.getAdvice();
				Object[] adviceArgs = fillAdviceArguments(adviceInfo, interestedArgs, thisArg, retVal);
				if (adviceInstances == null) {
					adviceInstances = new HashMap<>();
				}
				Object adviceInstance = getOrCreateAdviceInstance(adviceInstances, advice);

				Object newRetVal = advice.invoke(adviceInstance, adviceArgs);
				if (adviceInfo.hasOverrideRetVal()) {
					retVal = newRetVal;
				}
			} catch (Exception e) {
				logger.warn("Failed to invoke advice " + adviceInfo.getAdvice(), e);
				return null;
			}
		}
		return retVal;
	}

	@Override
	public void afterThrow(int joinPointId, Map<Class<?>, Object> adviceInstances,
	                       Object thisArg, Object[] interestedArgs, Throwable ex) {
		WeavingContext ctx = joinpointsWeavingInfo.get(joinPointId);
		List<WeavingAdviceInfo> advices = ctx.getAdviceInfoByKind().get(AdviceKind.AFTER_THROW);
		for (WeavingAdviceInfo adviceInfo : advices) {
			try {
				Method advice = adviceInfo.getAdvice();
				Object[] adviceArgs = fillAdviceArguments(adviceInfo, interestedArgs, thisArg, ex);
				if (adviceInstances == null) {
					adviceInstances = new HashMap<>();
				}
				Object adviceInstance = getOrCreateAdviceInstance(adviceInstances, advice);

				advice.invoke(adviceInstance, adviceArgs);
			} catch (Exception e) {
				logger.warn("Failed to invoke advice " + adviceInfo.getAdvice(), e);
			}
		}
	}

	private Object getOrCreateAdviceInstance(Map<Class<?>, Object> adviceInstances, Method advice)
			throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		Object adviceInstance = null;
		if (!Modifier.isStatic(advice.getModifiers())) {
			Class<?> adviceClass = advice.getDeclaringClass();
			adviceInstance = adviceInstances.get(adviceClass);
			if (adviceInstance == null) {
				Constructor<?> constr = adviceClass.getDeclaredConstructor();
				constr.setAccessible(true);
				adviceInstance = constr.newInstance();
				adviceInstances.put(adviceClass, adviceInstance);
			}
		}
		return adviceInstance;
	}

	/**
	 * Fills input arguments of various kind into the array of arguments required by advice method.
	 * @param adviceInfo the advice information
	 * @param interestedArgs the intercepted target arguments
	 * @param thisArg 'this' argument of target method
	 * @param afterArg additional argument for 'AFTER' advices - either @RetVal or @Thrown, depending of kind
	 * @return the array of arguments to pass to the advice method
	 */
	private Object[] fillAdviceArguments(WeavingAdviceInfo adviceInfo, Object[] interestedArgs, Object thisArg, Object afterArg) {
		Object[] adviceArgs;
		List<AdviceArgument> argInfos = adviceInfo.getAdviceArguments();
		Method advice = adviceInfo.getAdvice();
		if (adviceInfo.isFastProxyArgsAllowed()) {
			if (argInfos.size() == 1 && argInfos.get(0).getKind() == AdviceArgument.Kind.ALL_ARGUMENTS) {
				adviceArgs = new Object[]{interestedArgs};
			} else {
				adviceArgs = interestedArgs;
			}
		} else {
			adviceArgs = new Object[argInfos.size()];
			for (int i = 0; i < argInfos.size(); i++) {
				AdviceArgument argInfo = argInfos.get(i);
				switch (argInfo.getKind()) {
				case ARGUMENT:
					Object interceptedArg = interestedArgs[argInfo.getInterceptedArgument().getIdxInProxyArgsArray()];
					Object arg = argInfo.isModifiable() ?
							wrapToArray(interceptedArg, advice.getParameterTypes()[i]) :
							interceptedArg;
					adviceArgs[i] = arg;
					break;
				case THIS:
					adviceArgs[i] = thisArg;
					break;
				case ALL_ARGUMENTS:
					adviceArgs[i] = interestedArgs;
					break;
				case RETVAL:
					assert adviceInfo.getAdviceKind() == AdviceKind.AFTER_RETURN;
					adviceArgs[i] = afterArg;
					break;
				case THROWN:
					assert adviceInfo.getAdviceKind() == AdviceKind.AFTER_THROW;
					adviceArgs[i] = afterArg;
					break;
				case TARGET:
					adviceArgs[i] = adviceInfo.getContext().getTargetMethodSupplier().getSilently();
					break;
				default:
					assert false : "Unknown argument kind";
				}
			}
		}
		return adviceArgs;
	}

	private Object wrapToArray(Object interceptedArg, Class<?> arrType) {
		// wrap into array of required type - either Object[] or ExactType[]
		assert arrType.isArray();
		Object arr = Array.newInstance(arrType.getComponentType(), 1);
		Array.set(arr, 0, interceptedArg);
		return arr;
	}
}

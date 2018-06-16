// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.*;
import com.gilecode.xmx.boot.IXmxAopService;
import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class XmxAopManager extends BasicAdviceArgumentsProcessor implements IXmxAopService, IXmxAopLoader {

	private final static Logger logger = LoggerFactory.getLogger(XmxAopManager.class);

	private final Set<String> knownBadDescs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	private final AdviceVerifier adviceVerifier = new AdviceVerifier();
	private final AtomicInteger joinPointsCounter = new AtomicInteger(1);
	private final ConcurrentMap<Integer, WeavingContext> joinpointsWeavingInfo = new ConcurrentHashMap<>();

	@Override
	public Map<String, Class<?>> loadAndVerifyAdvices(Collection<String> adviceDescs, ManagedClassLoaderWeakRef classLoaderRef) {
		Map<String, Class<?>> adviceClassesByDesc = new HashMap<>(adviceDescs.size());
		for (String desc : adviceDescs) {
			if (knownBadDescs.contains(desc)) {
				continue;
			}
			try {
				int n = desc.lastIndexOf(':');
				String jarName = n > 0 ? desc.substring(0, n) : null;
				String className = n >= 0 ? desc.substring(n + 1) : desc;

				Class<?> adviceClass = loadClass(classLoaderRef, jarName, className);
				// FIXME cache verified advice classes somewhere (in ManagedClassLoaderWeakRef)?
				adviceVerifier.verifyAdviceClass(adviceClass);

				adviceClassesByDesc.put(desc, adviceClass);

			} catch (BadAdviceException e) {
				knownBadDescs.add(desc);
				logger.warn("Bad advice '" + desc + "' is skipped!", e);
			}
		}
		return adviceClassesByDesc;
	}

	private static Class<?> loadClass(ManagedClassLoaderWeakRef classLoaderRef, String jarName, String className) throws BadAdviceException {
		ClassLoader adviceJarLoader = getOrCreateAdviceJarLoader(classLoaderRef, jarName);
		try {
			return Class.forName(className, true, adviceJarLoader);
		} catch (ClassNotFoundException e) {
			throw new BadAdviceException("Failed to load advice class " + className, e);
		}
	}

	private static ClassLoader getOrCreateAdviceJarLoader(ManagedClassLoaderWeakRef classLoaderRef, String jarName) throws BadAdviceException {
		ConcurrentMap<String, ClassLoader> adviceJarLoaders = classLoaderRef.getAdviceJarLoaders();
		ClassLoader jarLoader = adviceJarLoaders.get(jarName);
		if (jarLoader == null) {
			URL jarUrl = findJar(jarName);
			if (jarUrl == null) {
				throw new BadAdviceException("Jar file '" + jarName + "' is not found");
			}
			// FIXME use special class loader, make sure that classes loaded by it are not managed
			jarLoader = adviceJarLoaders.putIfAbsent(jarName, new URLClassLoader(new URL[]{jarUrl}, classLoaderRef.get()));
		}
		return jarLoader;
	}

	private static URL findJar(String jarName) {
		// TODO get home dir (where??)
		return null;
	}

	@Override
	public WeavingContext prepareMethodAdvicesWeaving(Collection<String> adviceDescs,
	                                                  Map<String, Class<?>> adviceClassesByDesc,
	                                                  Type[] targetParamTypes, Type targetReturnType,
	                                                  String targetClassName, String targetMethodName) {
		WeavingContext ctx = new WeavingContext(joinPointsCounter.getAndIncrement());
		Map<AdviceKind, List<WeavingAdviceInfo>> adviceInfoByKind = ctx.getAdviceInfoByKind();
		for (String desc : adviceDescs) {
			Class<?> adviceClass = adviceClassesByDesc.get(desc);
			if (adviceClass != null) {
				for (Method advice : adviceClass.getDeclaredMethods()) {
					Advice adviceAnnotation = advice.getAnnotation(Advice.class);
					if (adviceAnnotation == null || !adviceVerifier.isAdviceCompatibleMethod(advice,
							targetParamTypes, targetReturnType, targetClassName, targetMethodName)) {
						continue;
					}

					AdviceKind adviceKind = adviceAnnotation.value();
					WeavingAdviceInfo adviceInfo = prepareWeaving(advice, adviceKind, ctx, targetParamTypes.length);

					List<WeavingAdviceInfo> compatibleAdvices = adviceInfoByKind.get(adviceKind);
					if (compatibleAdvices == null) {
						compatibleAdvices = new ArrayList<>(2);
						adviceInfoByKind.put(adviceKind, compatibleAdvices);
					}
					compatibleAdvices.add(adviceInfo);
				}
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
	private WeavingAdviceInfo prepareWeaving(Method advice, AdviceKind adviceKind, WeavingContext ctx, int nTargetParams) {
		Annotation[][] parametersAnnotations = advice.getParameterAnnotations();

		List<AdviceArgument> arguments = new ArrayList<>(parametersAnnotations.length);
		boolean hasOverrideRetVal;

		for (Annotation[] parameterAnnotations : parametersAnnotations) {
			Annotation argAnnotation = findArgumentAnnotation(parameterAnnotations);
			assert argAnnotation != null;
			AdviceArgument arg;

			if (argAnnotation instanceof Argument || argAnnotation instanceof ModifiableArgument) {
				int argumentIdx = getArgumentIdx(argAnnotation);
				boolean modifiable = argAnnotation instanceof ModifiableArgument;
				arg = AdviceArgument.interceptedArgument(ctx.addInterceptedArgument(argumentIdx, modifiable), modifiable);
			} else if (argAnnotation instanceof AllArguments) {
				boolean modifiable = ((AllArguments) argAnnotation).modifiable();
				arg = AdviceArgument.specialArgument(getAdviceArgumentKind(argAnnotation), modifiable);
				ctx.makeAllArgumentsIntercepted(modifiable, nTargetParams);
			} else {
				arg = AdviceArgument.specialArgument(getAdviceArgumentKind(argAnnotation));
			}
			arguments.add(arg);
		}

		// check @OverrideRetVal if present
		hasOverrideRetVal = advice.getAnnotation(OverrideRetVal.class) != null;
		return new WeavingAdviceInfo(advice, adviceKind, arguments, hasOverrideRetVal);
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

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import com.gilecode.xmx.aop.data.AdviceClassInfo;
import com.gilecode.xmx.aop.data.AdviceLoadResult;
import com.gilecode.xmx.aop.impl.WeakCachedSupplier;
import com.gilecode.xmx.aop.impl.WeavingContext;
import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.Map;

public interface IXmxAopLoader {

	/**
	 * Load advice classes by given descriptors (jarName:className) and perform initial verification.
	 * Return map of loaded and verified classes by descriptor.
	 * @param adviceDescs descriptors of advice classes, with the name of jar and class
	 * @param classLoaderRef the (managed) class loader of target classes; used as a parent loader for actual loader of
	 */
	AdviceLoadResult loadAndVerifyAdvices(Collection<String> adviceDescs, ManagedClassLoaderWeakRef classLoaderRef);

	/**
	 * Among all pre-loaded advices configured for the specified target method, selects the compatible advice methods
	 * and prepares weaving.
	 * <p/>
	 * In particular, the preparation determines which target arguments need to be intercepted, to what advice
	 * methods they shall be proxied etc.
	 */
	WeavingContext prepareMethodAdvicesWeaving(Collection<String> adviceDescs,
	                                           Map<String, AdviceClassInfo> adviceClassesByDesc,
	                                           Type[] targetParamTypes, Type targetReturnType,
	                                           String targetClassName, String targetMethodName, WeakCachedSupplier<Class<?>> targetClassSupplier);
}

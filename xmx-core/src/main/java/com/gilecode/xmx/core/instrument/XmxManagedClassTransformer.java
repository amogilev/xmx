// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import com.gilecode.xmx.aop.IXmxAopLoader;
import com.gilecode.xmx.aop.data.AdviceLoadResult;
import com.gilecode.xmx.aop.impl.AdviceVerifier;
import com.gilecode.xmx.aop.impl.WeakCachedSupplier;
import com.gilecode.xmx.aop.impl.WeavingContext;
import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.cfg.PropertyValue;
import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.core.ManagedClassLoaderWeakRef;
import com.gilecode.xmx.core.params.IParamNamesConsumer;
import com.gilecode.xmx.core.params.ParamNamesCache;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;

public class XmxManagedClassTransformer extends ClassVisitor {
	
	private static final String CONSTR_NAME = "<init>";
	
	/**
	 * The internal class ID assigned to the class being transformed.
	 * <p/>
	 * registerObject() uses this ID to prevent duplicate or undesired registration 
	 * from superclasses. 
	 */
	private final int classId;
	
	/**
	 * The name of the class being transformed, in bytecode format (e.g. "java/lang/Object").
	 */
	private final String bcClassName;

	/**
	 * Config which is used to check which methods need to be intercepted with advices.
	 */
	private final IAppPropertiesSource appConfig;

	private final String javaClassName;
	/**
	 * The potential advice classes for this class, mapped by their "jar:name" descriptor.
	 * Which advice classes from this map shall be applied to which method is defined by the
	 * configuration.
	 * <br/>
	 * All advice classes here are loaded and verified (by {@link AdviceVerifier#verifyAdviceClass(java.io.InputStream)}
	 * <br/>
	 * The use of {@link AdviceLoadResult} prevents early GC of weak refs to the advice classes.
	 */
	private final AdviceLoadResult loadedAdvices;

	/**
	 * AOP manager for the current session.
	 */
	private final IXmxAopLoader xmxAopManager;

	/**
	 * The reference to class loader which loads the target class.
	 */
	private final ManagedClassLoaderWeakRef classLoaderRef;

	/**
	 * Whether parameter names shall be extracted, if provided in LVT (and not available in Reflection parameters)
	 */
	private boolean extractParamNames;

	/**
	 * Supplier of the target class, initialized lazily. Used for implementing @TargetMethod advice arguments.
	 */
	private WeakCachedSupplier<Class<?>> targetClassSupplier;

	public XmxManagedClassTransformer(ClassVisitor cv, int classId, String bcClassName,
			String javaClassName,
			AdviceLoadResult loadedAdvices,
			IAppPropertiesSource appConfig,
			IXmxAopLoader xmxAopManager,
			ManagedClassLoaderWeakRef classLoaderRef,
			boolean extractParamNames) {
		super(Opcodes.ASM5, cv);
		this.classId = classId;
		this.bcClassName = bcClassName;
		this.javaClassName = javaClassName;
		this.loadedAdvices = loadedAdvices;
		this.appConfig = appConfig;
		this.xmxAopManager = xmxAopManager;
		this.classLoaderRef = classLoaderRef;
		this.extractParamNames = extractParamNames;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature, String[] exceptions) {
		MethodVisitor parentVisitor = super.visitMethod(access, name, desc, signature, exceptions);

		if (name.startsWith(CONSTR_NAME)) {
			// add registering managed objects to constructors
			return new XmxManagedConstructorTransformer(classId, bcClassName, parentVisitor);
		}

		Type[] argumentTypes = Type.getArgumentTypes(desc);
		if (extractParamNames && argumentTypes.length > 0) {
			parentVisitor = new LocalVariableTableParamNamesExtractor(access, parentVisitor, argumentTypes,
					new IParamNamesConsumer() {
				@Override
				public void consume(String[] argNames) {
					ParamNamesCache paramNamesCache = classLoaderRef.getParamNamesCache();
					boolean found = argNames != null && argNames.length > 0 && argNames[0] != null;
					if (found) {
						paramNamesCache.store(javaClassName, name, desc, argNames);
					} else {
						paramNamesCache.storeMissingClassInfo(javaClassName);
						extractParamNames = false;
					}
				}
			});
		}

		if (!loadedAdvices.isEmpty()) {
			// weave advices
			MethodSpec spec = MethodSpec.of(access, name, desc);
			PropertyValue advices = appConfig.getMethodProperty(javaClassName, spec, Properties.M_ADVICES);
			if (advices != null) {
				String[] adviceDescs = advices.asString().split(",");
				WeavingContext ctx = xmxAopManager.prepareMethodAdvicesWeaving(Arrays.asList(adviceDescs),
						loadedAdvices.getAdviceClassesByDesc(),
						Type.getArgumentTypes(desc), Type.getReturnType(desc),
						javaClassName, name, getTargetClassSupplier());

				if (!ctx.getAdviceInfoByKind().isEmpty()) {
					return new XmxAdviceMethodWeaver(access, name, desc, parentVisitor, ctx);
				}
			}
		}

		return parentVisitor;
	}

	private WeakCachedSupplier<Class<?>> getTargetClassSupplier() {
		if (targetClassSupplier == null) {
			targetClassSupplier = new WeakCachedSupplier<Class<?>>() {
				@Override
				protected Class<?> load() {
					ClassLoader cl = classLoaderRef.get();
					if (cl != null) {
						try {
							return cl.loadClass(javaClassName);
						} catch (ClassNotFoundException e) {
							// unexpected, give up silently
						}
					}
					// unexpected, give up silently
					return null;
				}
			};
		}
		return targetClassSupplier;
	}
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.instrument;

import com.gilecode.xmx.aop.IXmxAopLoader;
import com.gilecode.xmx.aop.impl.WeavingContext;
import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.cfg.PropertyValue;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Map;

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
	 * <p/>
	 * All advice classes here are loaded and verified (by {@link com.gilecode.xmx.aop.impl.AdviceVerifier#verifyAdviceClass(Class)}
	 */
	private final Map<String, Class<?>> potentialAdviceClassesByDesc;

	/**
	 * AOP manager for the current session.
	 */
	private final IXmxAopLoader xmxAopManager;

	public XmxManagedClassTransformer(ClassVisitor cv, int classId, String bcClassName,
	                                  String javaClassName,
	                                  Map<String, Class<?>> potentialAdviceClassesByDesc,
	                                  IAppPropertiesSource appConfig, IXmxAopLoader xmxAopManager) {
		super(Opcodes.ASM5, cv);
		this.classId = classId;
		this.bcClassName = bcClassName;
		this.javaClassName = javaClassName;
		this.potentialAdviceClassesByDesc = potentialAdviceClassesByDesc;
		this.appConfig = appConfig;
		this.xmxAopManager = xmxAopManager;
	}


	@Override
	public MethodVisitor visitMethod(final int access, String name, final String desc, String signature, String[] exceptions) {
		MethodVisitor parentVisitor = super.visitMethod(access, name, desc, signature, exceptions);
		
		if (name.startsWith(CONSTR_NAME)) {
			// add registering managed objects to constructors
			return new XmxManagedConstructorTransformer(classId, bcClassName, parentVisitor);

		} else if (!potentialAdviceClassesByDesc.isEmpty()) {
			// weave advices

			// FIXME need special support of methodPatterns in config! And pass name + access + signature
			PropertyValue advices = appConfig.getMemberProperty(javaClassName, name, Properties.M_ADVICES);
			if (advices != null) {
				String[] adviceDescs = advices.asString().split(",");
				WeavingContext ctx = xmxAopManager.prepareMethodAdvicesWeaving(adviceDescs,
						potentialAdviceClassesByDesc, Type.getArgumentTypes(desc), Type.getReturnType(desc),
						javaClassName, name);

				if (!ctx.getAdviceInfoByKind().isEmpty()) {
					return new XmxAdviceMethodWeaver(access, name, desc, parentVisitor, ctx);
				}
			}
		}

		return parentVisitor;
	}
}

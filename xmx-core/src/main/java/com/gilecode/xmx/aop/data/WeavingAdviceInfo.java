// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.BadAdviceException;
import com.gilecode.xmx.aop.ISupplier;
import com.gilecode.xmx.aop.impl.AdviceArgument;
import com.gilecode.xmx.aop.impl.WeavingContext;
import com.gilecode.xmx.model.XmxRuntimeException;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Information required for weaving a single advice method to a single target.
 */
public class WeavingAdviceInfo {

	/**
	 * The parent context which holds all advices for the given joinpoint.
	 */
	private final WeavingContext context;

	/**
	 * The advice method.
	 */
	private final ISupplier<Method> adviceSupplier;

	/**
	 * The advice kind, e.g. {@link AdviceKind#BEFORE}.
	 */
	private final AdviceKind adviceKind;

	/**
	 * The arguments required for calling advice methods.
	 */
	private final List<AdviceArgument> adviceArguments;

	private final boolean hasOverrideRetVal;

	private boolean fastProxyArgsAllowed;

	public WeavingAdviceInfo(WeavingContext context, ISupplier<Method> adviceSupplier, AdviceKind adviceKind, List<AdviceArgument> adviceArguments,
	                         boolean hasOverrideRetVal) {
		this.context = context;
		this.adviceSupplier = adviceSupplier;
		this.adviceKind = adviceKind;
		this.adviceArguments = adviceArguments;
		this.hasOverrideRetVal = hasOverrideRetVal;
	}

	public Method getAdvice() {
		try {
			return adviceSupplier.get();
		} catch (BadAdviceException e) {
			// unexpected: advice jar changed?
			throw new XmxRuntimeException("Unexpected: advice method failed to re-load!", e);
		}
	}

	public WeavingContext getContext() {
		return context;
	}

	public ISupplier<Method> getAdviceSupplier() {
		return adviceSupplier;
	}

	public AdviceKind getAdviceKind() {
		return adviceKind;
	}

	public List<AdviceArgument> getAdviceArguments() {
		return adviceArguments;
	}

	public boolean hasOverrideRetVal() {
		return hasOverrideRetVal;
	}

	public boolean isFastProxyArgsAllowed() {
		return fastProxyArgsAllowed;
	}

	public void setFastProxyArgsAllowed(boolean fastProxyArgsAllowed) {
		this.fastProxyArgsAllowed = fastProxyArgsAllowed;
	}
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.AdviceKind;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Information required for weaving a single advice method to a single target.
 */
public class WeavingAdviceInfo {

	/**
	 * The advice method.
	 */
	private final Method advice;

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

	public WeavingAdviceInfo(Method advice, AdviceKind adviceKind, List<AdviceArgument> adviceArguments,
	                         boolean hasOverrideRetVal) {
		this.advice = advice;
		this.adviceKind = adviceKind;
		this.adviceArguments = adviceArguments;
		this.hasOverrideRetVal = hasOverrideRetVal;
	}

	public Method getAdvice() {
		return advice;
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

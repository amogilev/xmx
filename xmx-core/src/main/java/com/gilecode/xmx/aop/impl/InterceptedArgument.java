// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

public class InterceptedArgument {

	/**
	 * An index of an explicit (i.e.not 'this') parameter in a target method.
	 */
	private final int targetMethodParameterIdx;

	/**
	 * Index of this argument in the array of all intercepted arguments passed to proxy.
	 */
	private final int idxInProxyArgsArray;

	/**
	 * Whether this argument is modifiable at any of 'before' advice.
	 */
	private boolean isModifiableAtBefore;

	public InterceptedArgument(int targetMethodParameterIdx, int idxInProxyArgsArray, boolean isModifiableAtBefore) {
		this.targetMethodParameterIdx = targetMethodParameterIdx;
		this.idxInProxyArgsArray = idxInProxyArgsArray;
		this.isModifiableAtBefore = isModifiableAtBefore;
	}

	public int getTargetMethodParameterIdx() {
		return targetMethodParameterIdx;
	}

	public boolean isModifiableAtBefore() {
		return isModifiableAtBefore;
	}

	public void setModifiableAtBefore(boolean modifiableAtBefore) {
		isModifiableAtBefore = modifiableAtBefore;
	}

	public int getIdxInProxyArgsArray() {
		return idxInProxyArgsArray;
	}
}
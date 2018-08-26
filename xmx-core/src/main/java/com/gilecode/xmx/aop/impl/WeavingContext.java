// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

import com.gilecode.xmx.aop.AdviceKind;
import com.gilecode.xmx.aop.data.WeavingAdviceInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * All information required for weaving advices to a single target.
 * <p/>
 * In particular, includes all matching advice methods mapped by advice kinds,
 * along with their arguments information, merged arrays of 'interested' target arguments etc.
 */
public class WeavingContext {

	/**
	 * All matching advices, for each of advice kinds.
	 */
	private final Map<AdviceKind, List<WeavingAdviceInfo>> adviceInfoByKind = new EnumMap<>(AdviceKind.class);

	/**
	 * List of all intercepted arguments. These arguments shall be passed as array to all proxy methods.
	 */
	private final List<InterceptedArgument> interceptedArguments = new ArrayList<>();

	/**
	 * Unique ID of the intercepted method
	 */
	private final int joinpointId;

	/**
	 * Supplier of the target method, used to implement @TargetMethod arguments.
	 */
	private final WeakCachedSupplier<Method> targetMethodSupplier;

	public WeavingContext(int joinpointId, WeakCachedSupplier<Method> targetMethodSupplier) {
		this.joinpointId = joinpointId;
		this.targetMethodSupplier = targetMethodSupplier;
	}

	private InterceptedArgument findInterceptedArgument(int targetMethodParameterIdx) {
		for (InterceptedArgument arg : interceptedArguments) {
			if (arg.getTargetMethodParameterIdx() == targetMethodParameterIdx) {
				return arg;
			}
		}
		return null;
	}

	public InterceptedArgument addInterceptedArgument(int targetMethodParameterIdx, boolean isModifiableAtBefore) {
		// check if already marked as interested
		InterceptedArgument arg = findInterceptedArgument(targetMethodParameterIdx);

		if (arg == null ) {
			arg = new InterceptedArgument(targetMethodParameterIdx, interceptedArguments.size(), isModifiableAtBefore);
			interceptedArguments.add(arg);
		} else if (isModifiableAtBefore) {
			arg.setModifiableAtBefore(true);
		}

		return arg;
	}

	public void makeAllArgumentsIntercepted(boolean modifiable, int nArgs) {
		for (int i = 0; i < nArgs; i++) {
			addInterceptedArgument(i, modifiable);
		}
	}

	public Map<AdviceKind, List<WeavingAdviceInfo>> getAdviceInfoByKind() {
		return adviceInfoByKind;
	}

	public List<InterceptedArgument> getInterceptedArguments() {
		return interceptedArguments;
	}

	public int getJoinpointId() {
		return joinpointId;
	}

	public WeakCachedSupplier<Method> getTargetMethodSupplier() {
		return targetMethodSupplier;
	}
}

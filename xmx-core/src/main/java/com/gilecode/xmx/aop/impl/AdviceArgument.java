// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.impl;

/**
 * Information about a single argument of the advice method, which includes mapping to a target parameter(s) or
 * special data.
 */
public class AdviceArgument {

	public enum Kind {
		ARGUMENT, ALL_ARGUMENTS, THIS, RETVAL, THROWN, TARGET
	}

	/**
	 * If {@code kind==ARGUMENT}, contains information about the corresponding intercepted (target) argument.
	 */
	private final InterceptedArgument interceptedArgument;

	/**
	 * The kind of the argument, i.e. simple 'single target argument' or special like 'thrown exception',
	 * 'return value' etc.
	 */
	private final Kind kind;

	/**
	 * Whether the argument may be modified by the advice. Currently supported for
	 * {@link Kind#ARGUMENT} and {@link Kind#ALL_ARGUMENTS}
	 */
	private final boolean modifiable;

	private AdviceArgument(Kind kind, InterceptedArgument interceptedArgument, boolean modifiable) {
		this.kind = kind;
		this.interceptedArgument = interceptedArgument;
		this.modifiable = modifiable;
	}

	public static AdviceArgument interceptedArgument(InterceptedArgument interceptedArgument, boolean modifiable) {
		return new AdviceArgument(Kind.ARGUMENT, interceptedArgument, modifiable);
	}

	public static AdviceArgument specialArgument(Kind kind, boolean modifiable) {
		assert kind != Kind.ARGUMENT;
		return new AdviceArgument(kind, null, modifiable);
	}

	public static AdviceArgument specialArgument(Kind kind) {
		assert kind != Kind.ARGUMENT && kind != Kind.ALL_ARGUMENTS;
		return new AdviceArgument(kind, null, false);
	}

	public Kind getKind() {
		return kind;
	}

	public InterceptedArgument getInterceptedArgument() {
		return interceptedArgument;
	}

	public boolean isModifiable() {
		return modifiable;
	}
}

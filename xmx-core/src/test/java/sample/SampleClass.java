// Copyright © 2018 Andrey Mogilev. All rights reserved.

package sample;

import com.gilecode.xmx.model.XmxRuntimeException;

import java.io.IOException;

// sample class used for AOP testing
public class SampleClass {

	public void empty() {}

	public void simpleThrow() {
		throw new XmxRuntimeException("sampleException");
	}

	public static String emptyStatic() {
		return "emptyStatic";
	}

	public long primitiveRet(long arg1, Long arg2) {
		return arg1 + arg2;
	}

	public Long boxedRet(long arg1, Long arg2) {
		return arg1 + arg2;
	}

	@SuppressWarnings("all")
	public Long complexThrow(long arg1, Long arg2) {
		try {
			try {
				throw new IOException();
			} catch (Exception e) {
				try {
					throw new XmxRuntimeException("sampleException1");
				} catch (IllegalArgumentException e2) {
					return 1L;
				} finally {
					return 2L;
				}
			}
		} finally {
			try {
				throw new XmxRuntimeException("sampleException2");
			} catch (IllegalArgumentException e) {
				return 3L;
			} catch (RuntimeException e) {
				throw e;
			}
		}
	}

	public Long caughtThrow(long arg1, Long arg2) {
		try {
			throw new IOException();
		} catch (Exception e) {
			return arg1 + arg2;
		}
	}

	public void params1(int i, double d, long l, Double d2, Long l2, String...s) {}
}

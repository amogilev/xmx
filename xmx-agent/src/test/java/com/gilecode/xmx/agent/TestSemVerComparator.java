// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.agent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSemVerComparator {

	SemVerComparator uut = new SemVerComparator();

	@Test
	public void testCompare() {
		checkLess("", "1");
		checkLess("1", "1.0");
		checkLess("1.10", "1.11");
		checkLess("1.11", "1.101");
		checkLess("1.11", "1.110");

		checkEquals("", "");
		checkEquals("1.0.0-alpha.1", "1.0.0-alpha.1");
		checkEquals("0.011", "0.11");

		// samples from https://semver.org/spec/v2.0.0.html
		checkLess("1.0.0-alpha", "1.0.0-alpha.1");
		checkLess("1.0.0-alpha.1", "1.0.0-alpha.beta");
		checkLess("1.0.0-alpha.beta", "1.0.0-beta");
		checkLess("1.0.0-beta", "1.0.0-beta.2");
		checkLess("1.0.0-beta.2", "1.0.0-beta.11");
		checkLess("1.0.0-beta.11", "1.0.0-rc.1");
		checkLess("1.0.0-rc.1", "1.0.0");
	}

	private void checkLess(String v1, String v2) {
		assertTrue(uut.compare(v1, v2) < 0);
		assertTrue(uut.compare(v2, v1) > 0);
	}

	private void checkEquals(String v1, String v2) {
		assertEquals(0, uut.compare(v1, v2));
		assertEquals(0, uut.compare(v2, v1));
	}
}
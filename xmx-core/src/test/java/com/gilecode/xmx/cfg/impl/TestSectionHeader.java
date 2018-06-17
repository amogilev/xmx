// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.CfgEntityLevel;
import com.gilecode.xmx.cfg.pattern.MethodSpec;
import org.junit.Before;
import org.junit.Test;

import static com.gilecode.xmx.cfg.pattern.PatternsSupport.parse;
import static com.gilecode.xmx.cfg.pattern.PatternsSupport.parseMethodPattern;
import static org.junit.Assert.*;

public class TestSectionHeader {

	private MethodSpec msHashCode;
	private MethodSpec msEquals;

	@Before
	public void setUp() throws Exception {
		msHashCode = MethodSpec.of(Object.class.getDeclaredMethod("hashCode"));
		msEquals = MethodSpec.of(Object.class.getDeclaredMethod("equals", Object.class));
	}

	@Test
	public void testAppHeader1() {
		SectionHeader uut = new SectionHeader(parse("*"), null);
		assertEquals(CfgEntityLevel.APP, uut.level);
		
		assertTrue(uut.appMatches("foo"));

		assertFalse(uut.isMatchingClassSection("foo", "bar"));
		assertFalse(uut.isMatchingClassSection("bar"));
		assertFalse(uut.isMatchingFieldSection("bar", "baz"));
		assertFalse(uut.isMatchingMethodSection("bar", msHashCode));
	}
	
	@Test
	public void testAppHeader2() {
		SectionHeader uut = new SectionHeader(parse("*app"), null);
		assertEquals(CfgEntityLevel.APP, uut.level);
		
		assertTrue(uut.appMatches("myapp"));
		assertTrue(uut.appMatches("app"));
		
		assertFalse(uut.appMatches("App"));
	}

	@Test
	public void testClassHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"));
		assertEquals(CfgEntityLevel.CLASS, uut.level);
		
		assertTrue(uut.appMatches("app"));
		assertTrue(uut.isMatchingClassSection("app", "MyService"));
		assertTrue(uut.isMatchingClassSection("MyService"));

		assertFalse(uut.isMatchingFieldSection("MyService", "baz"));
		assertFalse(uut.isMatchingMethodSection("MyService", msHashCode));
	}

	@Test
	public void testMethodHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"),
				parseMethodPattern("hashCode"), null);
		assertEquals(CfgEntityLevel.METHOD, uut.level);
		
		assertTrue(uut.appMatches("app"));
		assertTrue(uut.isMatchingMethodSection("app", "MyService", msHashCode));
		assertTrue(uut.isMatchingMethodSection("MyService", msHashCode));
		assertTrue(uut.isMatchingMethodSection("app", "MyService", null));

		assertFalse(uut.isMatchingMethodSection("app", "MyService", msEquals));
		assertFalse(uut.isMatchingFieldSection("app", "MyService", "hashCode"));
		assertFalse(uut.isMatchingFieldSection("MyService", "hashCode"));
		assertFalse(uut.isMatchingMethodSection("app", "foo", msHashCode));
	}

	@Test
	public void testFieldHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"),
				null, parse("hashCode"));
		assertEquals(CfgEntityLevel.FIELD, uut.level);

		assertTrue(uut.appMatches("app"));
		assertTrue(uut.isMatchingFieldSection("app", "MyService", "hashCode"));
		assertFalse(uut.isMatchingMethodSection("app", "MyService", msHashCode));
		assertTrue(uut.isMatchingFieldSection("MyService", "hashCode"));
		assertFalse(uut.isMatchingMethodSection("MyService", msHashCode));

		assertFalse(uut.isMatchingClassSection("app", "MyService"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadHeader() {
		new SectionHeader(parse("*"), parse("*Service*"),
				parseMethodPattern("*"), parse("cnt"));
	}
}

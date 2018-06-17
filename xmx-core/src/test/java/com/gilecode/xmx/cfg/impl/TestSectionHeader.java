// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.CfgEntityLevel;
import org.junit.Test;

import static com.gilecode.xmx.cfg.pattern.PatternsSupport.parse;
import static org.junit.Assert.*;

public class TestSectionHeader {
	
	@Test
	public void testAppHeader1() {
		SectionHeader uut = new SectionHeader(parse("*"), null);
		assertEquals(CfgEntityLevel.APP, uut.level);
		
		assertTrue(uut.appMatches("foo"));
		assertTrue(uut.matches("foo", "bar", null, false));
		assertTrue(uut.matchesAfterApp("bar", "baz", true));
		assertTrue(uut.matchesAfterApp("bar", "baz", false));
	}
	
	@Test
	public void testAppHeader2() {
		SectionHeader uut = new SectionHeader(parse("*app"), null);
		assertEquals(CfgEntityLevel.APP, uut.level);
		
		assertTrue(uut.appMatches("myapp"));
		assertTrue(uut.appMatches("app"));
		
		assertFalse(uut.appMatches("App"));
		
		assertTrue(uut.matches("app", "bar", null, true));
		assertTrue(uut.matchesAfterApp("bar", "baz", true));
	}

	@Test
	public void testClassHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"));
		assertEquals(CfgEntityLevel.CLASS, uut.level);
		
		assertTrue(uut.appMatches("app"));
		assertTrue(uut.matches("app", "MyService", null, true));
		assertTrue(uut.matchesAfterApp("MyService", "baz", false));
		
		assertFalse(uut.matches("app", null, null, false));
		assertFalse(uut.matches("app", "foo", null, false));
	}

	@Test
	public void testMethodHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"),
				parse("run"), true);
		assertEquals(CfgEntityLevel.METHOD, uut.level);
		
		assertTrue(uut.appMatches("app"));
		assertTrue(uut.matches("app", "MyService", "run", true));
		assertFalse(uut.matches("app", "MyService", "run", false));
		assertTrue(uut.matchesAfterApp("MyService", "run", true));
		assertFalse(uut.matchesAfterApp("MyService", "run", false));

		assertFalse(uut.matches("app", null, null, true));
		assertFalse(uut.matches("app", "MyService", null, true));
		assertFalse(uut.matches("app", "MyService", null, false));
		assertFalse(uut.matches("app", "MyService", "baz", true));
	}

	@Test
	public void testFieldHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"),
				parse("cnt"), false);
		assertEquals(CfgEntityLevel.FIELD, uut.level);

		assertTrue(uut.appMatches("app"));
		assertTrue(uut.matches("app", "MyService", "cnt", false));
		assertFalse(uut.matches("app", "MyService", "cnt", true));
		assertTrue(uut.matchesAfterApp("MyService", "cnt", false));
		assertFalse(uut.matchesAfterApp("MyService", "cnt", true));

		assertFalse(uut.matches("app", null, null, true));
		assertFalse(uut.matches("app", "MyService", null, true));
		assertFalse(uut.matches("app", "MyService", "run", false));
	}
}

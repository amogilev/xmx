// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg.impl;

import am.xmx.cfg.CfgEntityLevel;
import org.junit.Test;

import static am.xmx.cfg.impl.PatternsSupport.parse;
import static org.junit.Assert.*;

public class TestSectionHeader {
	
	@Test
	public void testAppHeader1() {
		SectionHeader uut = new SectionHeader(parse("*"), null, null);
		assertEquals(CfgEntityLevel.APP, uut.level);
		
		assertTrue(uut.appMatches("foo"));
		assertTrue(uut.matches("foo", "bar", null));
		assertTrue(uut.matchesAfterApp("bar", "baz"));
	}
	
	@Test
	public void testAppHeader2() {
		SectionHeader uut = new SectionHeader(parse("*app"), null, null);
		assertEquals(CfgEntityLevel.APP, uut.level);
		
		assertTrue(uut.appMatches("myapp"));
		assertTrue(uut.appMatches("app"));
		
		assertFalse(uut.appMatches("App"));
		
		assertTrue(uut.matches("app", "bar", null));
		assertTrue(uut.matchesAfterApp("bar", "baz"));
	}

	@Test
	public void testClassHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"), null);
		assertEquals(CfgEntityLevel.CLASS, uut.level);
		
		assertTrue(uut.appMatches("app"));
		assertTrue(uut.matches("app", "MyService", null));
		assertTrue(uut.matchesAfterApp("MyService", "baz"));
		
		assertFalse(uut.matches("app", null, null));
		assertFalse(uut.matches("app", "foo", null));
	}

	@Test
	public void testMemberHeader() {
		SectionHeader uut = new SectionHeader(parse("*"), parse("*Service*"), parse("run"));
		assertEquals(CfgEntityLevel.MEMBER, uut.level);
		
		assertTrue(uut.appMatches("app"));
		assertTrue(uut.matches("app", "MyService", "run"));
		assertTrue(uut.matchesAfterApp("MyService", "run"));
		
		assertFalse(uut.matches("app", null, null));
		assertFalse(uut.matches("app", "MyService", null));
		assertFalse(uut.matches("app", "MyService", "baz"));
	}
}

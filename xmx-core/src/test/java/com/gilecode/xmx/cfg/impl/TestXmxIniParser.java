// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import org.junit.Before;
import org.junit.Test;

import static com.gilecode.xmx.cfg.impl.XmxCfgSectionNameParser.findUnquotedChar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestXmxIniParser {
	
	XmxCfgSectionNameParser uut;
	
	@Before
	public void setup() {
		uut = new XmxCfgSectionNameParser();
	}
	
	@Test
	public void testFindUnquoted() {
		String str = "a,b\"c,,,,\",";
		assertEquals(1, findUnquotedChar(str, ',', 0));
		assertEquals(1, findUnquotedChar(str, ',', 1));
		assertEquals(1, findUnquotedChar(str, ',', -1000));
		assertEquals(str.length() - 1, findUnquotedChar(str, ',', 2));
		assertEquals(-1, findUnquotedChar(str, ',', str.length()));
		assertEquals(-1, findUnquotedChar(str, ',', str.length()+100));
		assertEquals(-1, findUnquotedChar(str, 'c', 0));
	}
	
	@Test
	public void testParseSectionNamePartEmpty1() {
		uut.parseSectionNamePart("App");
		assertEquals("", uut.sectionNameParts.get("App"));
	}
	
	@Test
	public void testParseSectionNamePartEmpty2() {
		uut.parseSectionNamePart("App=");
		assertEquals("", uut.sectionNameParts.get("App"));
	}

	@Test
	public void testParseSectionNamePartEmpty3() {
		uut.parseSectionNamePart(" App =  ");
		assertEquals("", uut.sectionNameParts.get("App"));
	}
	
	@Test
	public void testParseSectionNamePartNoUnquote() {
		uut.parseSectionNamePart("\"App\"=\" foo \" ");
		assertEquals("\" foo \"", uut.sectionNameParts.get("App"));
	}
	
	@Test
	public void testParseSectionNameValues() {
		uut.parseSectionNamePart("Part1=a=b");
		assertEquals("a=b", uut.sectionNameParts.get("Part1"));
		
		uut.parseSectionNamePart("\"Part2=2\"=a=b");
		assertEquals("a=b", uut.sectionNameParts.get("Part2=2"));
	}
	
	@Test
	public void testParseSectionNameParts1() {
		uut.parseSectionNameParts("Part1=a=b");
		assertEquals("a=b", uut.sectionNameParts.get("Part1"));
	}
	
	@Test
	public void testParseSectionNameParts2() {
		uut.parseSectionNameParts("Part1=a;Part2=b");
		assertEquals(2, uut.sectionNameParts.size());
		assertEquals("a", uut.sectionNameParts.get("Part1"));
		assertEquals("b", uut.sectionNameParts.get("Part2"));
	}
	
	@Test
	public void testParseSectionNameParts3() {
		uut.parseSectionNameParts("Part1=a=b;Part2=\"a;b\"; Part3;;Part4");
		assertEquals(4, uut.sectionNameParts.size());
		assertEquals("a=b", uut.sectionNameParts.get("Part1"));
		assertEquals("\"a;b\"", uut.sectionNameParts.get("Part2"));
		assertEquals("", uut.sectionNameParts.get("Part3"));
		assertEquals("", uut.sectionNameParts.get("Part4"));
		assertEquals(null, uut.sectionNameParts.get("Part5"));
	}
	
	@Test
	public void testSectionHeader() {
		SectionHeader sh = uut.parseSectionHeader("App=*;Class=com.gilecode.Foo");
		assertEquals("*", sh.appSpec);
		assertEquals("com.gilecode.Foo", sh.classSpec);
		assertTrue(sh.matches("myapp", "com.gilecode.Foo", null));
	}
	
	
}

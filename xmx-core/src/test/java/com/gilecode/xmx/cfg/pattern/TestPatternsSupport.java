// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

import com.gilecode.xmx.cfg.impl.XmxIniParseException;
import org.junit.Test;

import static com.gilecode.xmx.cfg.pattern.PatternsSupport.*;
import static org.junit.Assert.*;

public class TestPatternsSupport {
	
	@Test
	public void testUnquote() {
		// do nothing if not valid quoted
		assertEquals("", unquote(""));
		assertEquals("fo\"o", unquote("fo\"o"));
		assertEquals("\"fo\"o", unquote("\"fo\"o"));
		assertEquals("fo\"o\"", unquote("fo\"o\""));
		
		// simple unquote
		assertEquals("foo", unquote("\"foo\""));
		assertEquals("f o o", unquote("\"f o o\""));
		
		// unquote with inner quotes
		assertEquals("f\"o\"o", unquote("\"f\"\"o\"\"o\""));
	}
	
	@Test
	public void testMatchesLiteral() {
		assertTrue(matches("foo", "foo"));
		assertTrue(matches(" foo ", "foo")); // patterns are trimmed before use
		
		assertFalse(matches("foo", "bar"));
		assertFalse(matches("foo", "foo1"));
	}

	@Test
	public void testMatchesSimplePatternsWithSpace() {
		assertTrue(matches("foo bar", "foo bar"));
		assertTrue(matches("foo *", "foo bar"));
		assertTrue(matches("f*", "foo bar"));
	}

	@Test
	public void testMatchesQuotedLiteral() {
		assertTrue(matches("\"foo\"", "foo"));
		assertTrue(matches("\"f o o\"", "f o o"));
		assertTrue(matches("\" \"", " "));
		assertTrue(matches("\"f \"\"o \"\"o\"", "f \"o \"o"));
		assertTrue(matches("\"foo \"", "foo "));
		
		assertFalse(matches("\"foo \"", "foo"));
	}
	
	@Test
	public void testMatchesMask() {
		assertTrue(matches("*", "foo"));
		assertTrue(matches("*", ""));
		
		assertTrue(matches("*Impl", "Impl"));
		assertTrue(matches("*Impl", "MyImpl"));
		assertTrue(matches("*Impl", "My Impl"));
		
		assertTrue(matches("*Service*", "Service"));
		assertTrue(matches("*Service*", "MyServiceImpl"));
		assertFalse(matches("*Service*", "service"));
		
		assertTrue(matches("foo|bar", "foo"));
		assertTrue(matches("foo|bar", "bar"));
		assertFalse(matches("foo|bar", "foobar"));
		
		assertTrue(matches("*foo|bar*", "1foo"));
		assertTrue(matches("*foo|bar*", "bar1"));
		assertTrue(matches("*foo|bar*", "barfoo"));
		assertFalse(matches("*foo|bar*", "foobar"));
	}
	
	@Test
	public void testMatchesClassMask() {
		assertTrue(matches("com.gilecode.Foo", "com.gilecode.Foo"));
		assertTrue(matches("*.Foo", "com.gilecode.Foo"));
		assertTrue(matches("*.Foo$Bar", "com.gilecode.Foo$Bar"));
	}
	
	@Test
	public void testMatchesJavaPattern() {
		assertTrue(matches("^(my|our)app\\d*$", "ourapp2"));
		assertFalse(matches("^(my|our)app\\d*$", "1ourapp"));
	}
	
	@Test
	public void testDefaultManagedPattern() {
		String defaultPattern = "^.*(Service|(?<![rR]eference)Manager|Engine|DataSource)\\d*(Impl\\d*)?$";
		assertTrue(matches(defaultPattern, "com.gilecode.MyServiceImpl"));
		assertTrue(matches(defaultPattern, "com.gilecode.MyService2"));
		assertTrue(matches(defaultPattern, "com.gilecode.MyService2Impl3"));
		assertFalse(matches(defaultPattern, "com.gilecode.MyServiceImplNot"));
		assertTrue(matches(defaultPattern, "DataSource"));
		assertTrue(matches(defaultPattern, "someManager"));
		assertFalse(matches(defaultPattern, "referenceManager"));
		assertFalse(matches(defaultPattern, "someReferenceManager"));
	}

	@Test(expected=XmxIniParseException.class)
	public void testParseWrongPattern() {
		parse("^foo(bar$"); // incorrect Java pattern
	}
	
	@Test(expected=XmxIniParseException.class)
	public void testParseBracketsInMask() {
		parse("(my|our)app"); // brackets not supported in masks
	}
	

}

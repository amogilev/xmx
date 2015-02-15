package am.xmx.cfg.impl;

import static am.xmx.cfg.impl.PatternsSupport.matches;
import static am.xmx.cfg.impl.PatternsSupport.unquote;
import static am.xmx.cfg.impl.PatternsSupport.parse;
import static org.junit.Assert.*;

import org.junit.Test;

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
	public void testMatchesJavaPattern() {
		assertTrue(matches("^(my|our)app\\d*$", "ourapp2"));
		assertFalse(matches("^(my|our)app\\d*$", "1ourapp"));
	}

	@Test(expected=XmxIniParseException.class)
	public void testParseIllegalSpace() {
		parse("foo bar"); // spaces not supported in simple literals
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

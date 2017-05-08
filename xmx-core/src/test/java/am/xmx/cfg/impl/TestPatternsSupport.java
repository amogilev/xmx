package am.xmx.cfg.impl;

import org.junit.Test;

import static am.xmx.cfg.impl.PatternsSupport.*;
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
		assertTrue(matches("am.xmx.Foo", "am.xmx.Foo"));
		assertTrue(matches("*.Foo", "am.xmx.Foo"));
		assertTrue(matches("*.Foo$Bar", "am.xmx.Foo$Bar"));
	}
	
	@Test
	public void testMatchesJavaPattern() {
		assertTrue(matches("^(my|our)app\\d*$", "ourapp2"));
		assertFalse(matches("^(my|our)app\\d*$", "1ourapp"));
	}
	
	@Test
	public void testDefaultManagedPattern() {
		String defaultPattern = "^.*(Service|(?<![rR]eference)Manager|Engine|DataSource)\\d*(Impl\\d*)?$";
		assertTrue(matches(defaultPattern, "am.xmx.MyServiceImpl"));
		assertTrue(matches(defaultPattern, "am.xmx.MyService2"));
		assertTrue(matches(defaultPattern, "am.xmx.MyService2Impl3"));
		assertFalse(matches(defaultPattern, "am.xmx.MyServiceImplNot"));
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

// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest {

    @Test
    public void testQuote() {
        assertEquals("''", StringUtils.quote(""));
        assertEquals("'foo bar'", StringUtils.quote("foo bar"));
        assertEquals("''''", StringUtils.quote("'"));
        assertEquals("'''foo''''bar'''", StringUtils.quote("'foo''bar'"));
    }

    @Test
    public void testExtractQuoted() {
        String[] parts;

        parts = StringUtils.extractQuoted("''", 0);
        assertEquals(2, parts.length);
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);

        parts = StringUtils.extractQuoted("'foo'bar", 0);
        assertEquals(2, parts.length);
        assertEquals("foo", parts[0]);
        assertEquals("bar", parts[1]);

        parts = StringUtils.extractQuoted("'''foo'''bar", 0);
        assertEquals(2, parts.length);
        assertEquals("'foo'", parts[0]);
        assertEquals("bar", parts[1]);

        parts = StringUtils.extractQuoted("'foo''bar'''", 0);
        assertEquals(2, parts.length);
        assertEquals("foo'bar'", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractQuotedBad1() {
        StringUtils.extractQuoted("", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractQuotedBad2() {
        StringUtils.extractQuoted("a''", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractQuotedBad3() {
        StringUtils.extractQuoted("'a''", 0);
    }

}
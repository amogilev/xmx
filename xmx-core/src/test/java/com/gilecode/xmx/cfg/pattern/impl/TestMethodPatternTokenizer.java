// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TestMethodPatternTokenizer {

    private List<String> tokenize(String str) {
        List<MethodPatternParser.ParseToken> tokens = new MethodPatternParser.MethodPatternTokenizer(new MethodPatternParser.ParseContext(str)).tokenize();
        List<String> actualTokens = new ArrayList<>(tokens.size());
        for (MethodPatternParser.ParseToken pt : tokens) {
            actualTokens.add(pt.token);
        }
        return actualTokens;
    }

    @Test
    public void testTokenize() {
        String str = "public \t\n package.Type<Fo$o.>[] _name(java.lang.String...,...)\n";
        List<String> expected = asList("public", "package.Type", "<", "Fo$o.", ">", "[", "]",
                "_name", "(", "java.lang.String...", ",", "...", ")");
        assertEquals(expected, tokenize(str));
    }

    @Test
    public void testTokenize2() {
        String str = "public*";
        List<String> expected = asList("public*");
        assertEquals(expected, tokenize(str));
    }

    @Test
    public void testTokenizeParamsWithNames() {
        String str = "String...args";
        List<String> expected = asList("String...","args");
        assertEquals(expected, tokenize(str));
    }

    @Test
    public void testTokenizeEmpty() {
        assertEquals(0, tokenize("   ").size());
    }

    @Test
    public void testTokenRange() {
        String str = "public *";
        List<MethodPatternParser.ParseToken> tokens = new MethodPatternParser.MethodPatternTokenizer(new MethodPatternParser.ParseContext(str)).tokenize();
        assertEquals(2, tokens.size());
        assertEquals("public", tokens.get(0).token);
        assertEquals(0, tokens.get(0).pos);
        assertEquals("*", tokens.get(1).token);
        assertEquals(7, tokens.get(1).pos);
    }
}
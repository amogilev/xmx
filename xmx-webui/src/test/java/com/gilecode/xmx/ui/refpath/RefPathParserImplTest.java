// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class RefPathParserImplTest {

    RefPathParserImpl uut = new RefPathParserImpl();

    @Test
    public void testParseRoot1() throws RefPathSyntaxException {
        RefPath result = uut.parse("$1");
        RefPathRoot root = result.getRoot();
        assertThat(root, isA(RefPathIdRoot.class));
        assertEquals(1, ((RefPathIdRoot)root).getObjectId());

        assertThat(result.getSuffixes(), empty());
        assertFalse(result.isRequireProxy());
    }

    @Test
    public void testParseRoot2() throws RefPathSyntaxException {
        RefPath result = uut.parse("$:'app':my.MyClass:");
        RefPathRoot root = result.getRoot();
        assertThat(root, isA(RefPathSingletonRoot.class));
        assertEquals("app", ((RefPathSingletonRoot)root).getAppName());
        assertEquals("my.MyClass", ((RefPathSingletonRoot)root).getClassName());

        assertThat(result.getSuffixes(), empty());
        assertFalse(result.isRequireProxy());
    }

    @Test
    public void testParseRoot3() throws RefPathSyntaxException {
        RefPath result = uut.parse("$P:$:app:my.MyClass:");
        RefPathRoot root = result.getRoot();
        assertThat(root, isA(RefPathSingletonRoot.class));
        assertEquals("app", ((RefPathSingletonRoot)root).getAppName());
        assertEquals("my.MyClass", ((RefPathSingletonRoot)root).getClassName());

        assertThat(result.getSuffixes(), empty());
        assertTrue(result.isRequireProxy());
    }

    @Test
    public void testParseBeanName1() throws RefPathSyntaxException {
        RefPath result;

        result = uut.parse("$1.#'a'");
        assertThat(result.getSuffixes(), hasSize(1));
        RefPathSuffix beanSuffix = result.getSuffixes().get(0);
        assertThat(beanSuffix, isA(RefPathBeanSuffix.class));
        assertEquals("a", ((RefPathBeanSuffix)beanSuffix).getBeanName());
    }

    @Test
    public void testParseBeanName2() throws RefPathSyntaxException {
        RefPath result;

        result = uut.parse("$1.ctx.#'a&''b'''");
        assertThat(result.getSuffixes(), hasSize(2));
        RefPathSuffix beanSuffix = result.getSuffixes().get(1);
        assertThat(beanSuffix, isA(RefPathBeanSuffix.class));
        assertEquals("a&'b'", ((RefPathBeanSuffix)beanSuffix).getBeanName());
    }


    @Test
    public void testParseRefPathFieldSuffix() throws RefPathSyntaxException {
        RefPathSuffix result = uut.parseRefPathSuffix("a", "a");

        assertThat(result, isA(RefPathFieldSuffix.class));
        assertEquals("a", ((RefPathFieldSuffix)result).getFieldName());
    }

    @Test
    public void testParseRefPathArrayElementSuffix() throws RefPathSyntaxException {
        RefPathSuffix result = uut.parseRefPathSuffix("1", "1");

        assertThat(result, isA(RefPathArrayElementSuffix.class));
        assertEquals(1, ((RefPathArrayElementSuffix)result).getElementIndex());
    }

    @Test
    public void testParseRefPathBadArrayElementSuffix() {
        String path = "1b";
        try {
            uut.parseRefPathSuffix(path, path);
            fail("Exception expected");
        } catch (RefPathSyntaxException e) {
            assertEquals(path, e.getRefpath());
            assertEquals("Illegal refpath: expect integer index: '1b'", e.getMessage());
        }
    }

    @Test
    public void testParseRefPathBeanNameSuffix() throws RefPathSyntaxException {
        RefPathSuffix result;

        result = uut.parseRefPathSuffix("#a", "");
        assertThat(result, isA(RefPathBeanSuffix.class));
        assertEquals("a", ((RefPathBeanSuffix)result).getBeanName());
        assertFalse(((RefPathBeanSuffix)result).isUseDefinition());

        result = uut.parseRefPathSuffix("##a", "");
        assertThat(result, isA(RefPathBeanSuffix.class));
        assertEquals("a", ((RefPathBeanSuffix)result).getBeanName());
        assertTrue(((RefPathBeanSuffix)result).isUseDefinition());
    }

    @Test
    public void testExtractPathElements() throws RefPathSyntaxException {
        List<String> parts;

        parts = uut.extractPathElements("", "");
        assertTrue(parts.isEmpty());

        parts = uut.extractPathElements("a", "");
        assertThat(parts, contains("a"));

        parts = uut.extractPathElements("a.b.c", "");
        assertThat(parts, contains("a", "b", "c"));

        parts = uut.extractPathElements(".", "");
        assertThat(parts, contains("", ""));
    }

    @Test
    public void testExtractPathElementsQuoted() throws RefPathSyntaxException {
        List<String> parts;

        parts = uut.extractPathElements("#'foo'", "");
        assertThat(parts, contains("#foo"));

        parts = uut.extractPathElements("a.#'foo'.'bar'.''", "");
        assertThat(parts, contains("a", "#foo", "bar", ""));

        parts = uut.extractPathElements("#'A&''B'''", "");
        assertThat(parts, contains("#A&'B'"));
    }

    @Test
    public void testExtractPathElementsException1() {
        String path = "a.'''";
        try {
            uut.extractPathElements(path, path);
            fail("Exception expected");
        } catch (RefPathSyntaxException e) {
            assertEquals(path, e.getRefpath());
            assertEquals("Missing closing quote character: start=0, str='''", e.getMessage());
        }
    }
}
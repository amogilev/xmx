// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.impl.XmxIniParseException;
import com.gilecode.xmx.cfg.pattern.IMethodMatcher;
import com.gilecode.xmx.cfg.pattern.MethodSpec;
import com.gilecode.xmx.cfg.pattern.PatternsSupport;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

abstract class TestMethodPatternsDemo {
    public TestMethodPatternsDemo() {}
    public TestMethodPatternsDemo(String a) {}

    public static void main0(String[] args) {}
    public static void main0(String[] args, int n) {}
    public static int main0(String[] args, long n) { return 0; }
    public static void notMain(String[] args) {}

    int foo1(String[] args) { return 0; }
    int foo1() { return 0;}
    int foo1(int a) { return 0;}
    int foo1(Integer a) { return 0;}
    int foo1(int a, Integer b) { return 0;}

    String testRet() { return null; }
    String[] testRetArr() { return null; }
    List<String[]> testRetListArr() { return null; }

    private void bar1() {}
    void bar2() {}
    protected void bar3() {}
    public void bar4() {}
    native void bar5();
    abstract void bar6();
    synchronized strictfp void bar7() {}
}

public class TestMethodPatternMatcher {

    private void checkCount(String pattern, int expectedCount) {
    	checkCount_Refl(pattern, expectedCount);
    	checkCount_Desc(pattern, expectedCount);
    }

    private void checkCount_Refl(String pattern, int expectedCount) {
        IMethodMatcher matcher = PatternsSupport.parseMethodPattern(pattern);
        int count = 0;
        for (Method m : TestMethodPatternsDemo.class.getDeclaredMethods()) {
            if (matcher.matches(MethodSpec.of(m))) {
                count++;
            }
        }
        for (Constructor<?> c : TestMethodPatternsDemo.class.getDeclaredConstructors()) {
            if (matcher.matches(MethodSpec.of(c))) {
                count++;
            }
        }

        assertEquals(expectedCount, count);
    }

    private void checkCount_Desc(String pattern, int expectedCount) {
        IMethodMatcher matcher = PatternsSupport.parseMethodPattern(pattern);
        int count = 0;
        for (Method m : TestMethodPatternsDemo.class.getDeclaredMethods()) {
	        MethodSpec spec = MethodSpec.of(m.getModifiers(), m.getName(), Type.getType(m).getDescriptor());
            if (matcher.matches(spec)) {
                count++;
            }
        }
        for (Constructor c : TestMethodPatternsDemo.class.getDeclaredConstructors()) {
	        MethodSpec spec = MethodSpec.special(c.getModifiers(), c.getDeclaringClass().getSimpleName(),
                    Type.getType(c).getDescriptor(), "<init>");
            if (matcher.matches(spec)) {
                count++;
            }
        }

        assertEquals(expectedCount, count);
    }

	private void checkResult(String pattern, String expectedSignature) {
    	checkResult_Refl(pattern, expectedSignature);
    	checkResult_Desc(pattern, expectedSignature);
	}

    private void checkResult_Refl(String pattern, String expectedSignature) {
        IMethodMatcher matcher = PatternsSupport.parseMethodPattern(pattern);
        Method[] methods = TestMethodPatternsDemo.class.getDeclaredMethods();
        Method found = null;
        for (Method m : methods) {
            if (matcher.matches(MethodSpec.of(m))) {
                assertNull("Expected one match, but found several", found);
                found = m;
            }
        }

        assertNotNull("Expected one match, but found none", found);
        assertEquals(expectedSignature, found.toString());
    }

    private void checkResult_Desc(String pattern, String expectedSignature) {
        IMethodMatcher matcher = PatternsSupport.parseMethodPattern(pattern);
        Method[] methods = TestMethodPatternsDemo.class.getDeclaredMethods();
        Method found = null;
        for (Method m : methods) {
	        MethodSpec spec = MethodSpec.of(m.getModifiers(), m.getName(), Type.getType(m).getDescriptor());
	        if (matcher.matches(spec)) {
                assertNull("Expected one match, but found several", found);
                found = m;
            }
        }

        assertNotNull("Expected one match, but found none", found);
        assertEquals(expectedSignature, found.toString());
    }

    @Test
    public void testMain0() {
        checkCount("public static void main0(String[])", 1);
        checkCount("public static void main0(java.lang.String[])", 1);

        checkResult("public static void main0(String[])",
		        "public static void com.gilecode.xmx.cfg.pattern.impl.TestMethodPatternsDemo.main0(java.lang.String[])");
    }

    @Test
    public void testParameters() {
        checkCount("main*", 3);
        checkCount("main*(...)", 3);
        checkCount("main*()", 0);
        checkCount("main*(String,...)", 0);
        checkCount("main*(String...)", 1);
        checkCount("main*(java.lang.String...)", 1);
        checkCount("main*(String ...)", 1);
        checkCount("main*(String[]...)", 0);
        checkCount("main*(String[])", 1);
        checkCount("main*(String [])", 1);
        checkCount("main*(String[],...)", 3);
        checkCount("main*(String...,...)", 3);
        checkCount("main*(String[],double)", 0);
        checkCount("main*(String[],int)", 1);
        checkCount("main*(String[],long)", 1);
    }

    @Test
    public void testParametersWithNames() {
        checkCount("main*(String arg1,...)", 0);
        checkCount("main*(String...arg1)", 1);
        checkCount("main*(String... arg1)", 1);
        checkCount("main*(String ...arg1)", 1);
        checkCount("main*(String ... arg1)", 1);
        checkCount("main*(String[] arg1,int arg2)", 1);
    }

    @Test
    public void testVisibilities() {
        checkCount("bar*", 7);
        checkCount("public bar*", 1);
        checkCount("protected bar*", 1);
        checkCount("package bar*", 4);
        checkCount("private bar*", 1);

        checkCount("!public bar*", 6);
        checkCount("!package bar*", 3);

        checkCount("{public,protected} bar*", 2);
        checkCount("!{public,protected} bar*", 5);
        checkCount("{public, package} bar*", 5);
        checkCount("!{public, package} bar*", 2);

        checkCount("{public,protected,package,private} bar*", 7);
    }

    @Test
    public void testInvalidVisibilities() {
        try {
            checkCount("!{public,protected,package,private} bar*", 0);
            fail ("XmxIniParseException expected");
        } catch (XmxIniParseException e) {
            assertTrue(e.getMessage().startsWith("Invalid visibilities list: all visibilities are prohibited!"));
        }
    }

    @Test
    public void testModifiers() {
        int total = 19;
        checkCount("static *", 4);
        checkCount("public static *", 4);
        checkCount("protected static *", 0);
        checkCount("!static *", total - 4);
        checkCount("abstract *", 1);
        checkCount("synchronized *", 1);
        checkCount("strictfp *", 1);
        checkCount("synchronized strictfp *", 1);
        checkCount("!synchronized strictfp *", 0);
        checkCount("synchronized !strictfp *", 0);
        checkCount("!synchronized !strictfp *", total - 1);
    }

    @Test
    public void testReturnType() {
        checkCount("int main*", 1);
        checkCount("void main*", 2);

        checkCount("String testRet*", 1);
        checkCount("java.lang.String testRet*", 1);
        checkCount("some.package.String testRet*", 0);
        checkCount("not_String testRet*", 0);
        checkCount("String[] testRet*", 1);
        checkCount("java.lang.String[] testRet*", 1);
        checkCount("List testRet*", 1);
        checkCount("java.util.List testRet*", 1);
        checkCount("java.util.List<?> testRet*", 1);
        checkCount("java.util.List<java.lang.String[]> testRet*", 1);
        checkCount("java.util.List<_shall be ignored!_> testRet*", 1);
        checkCount("java.util.List<<>,<>,<<>> all ignored!> testRet*", 1);
    }

    @Test
    public void testQuotedPattern() {
        checkCount("\"int main*\"", 1);
    }

    @Test
    public void testSkipsGenerics() {
        checkCount("public <T> void main*(T)", 0);
    }

    @Test
    public void testConstructors() {
        checkCount("public <init>()", 1);
        checkCount("public TestMethodPatternsDemo()", 1);

        checkCount("public <init>(...)", 2);
        checkCount("public TestMethodPatternsDemo(...)", 2);

        // * shall not include constructors
        checkCount("public <init>(String)", 1);
        checkCount("public *(String)", 0);
    }
}

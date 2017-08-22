// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.gilecode.xmx.cfg.Properties.SP_MANAGED;
import static org.junit.Assert.*;

public class TestCustomXmxIniConfig {
	
	Path tempIniFile;
	XmxIniConfig uut;
	
	static List<String> contents = Arrays.asList(
			"[App=App1;Class=Class11]",
			"TestProp=val1",
			"Managed=false",
			
			"[App=*]",
			"ManagedClasses=Bad*",
			"TestProp=val2",
			
			"[App=App1]",
			"ManagedClasses=Class*",
			"TestProp=val3",
			
			"[App=*;Class=Class2*]",
			"TestProp=val4",
			"Managed=false",

			"[App=*;Class=Class21]",
			"TestProp=val5",
			"Managed=true",

			"[App=other;Class=Class21]",
			"TestProp=val6",
			"Managed=false"
			);
	
	@BeforeClass
	public static void setupTestProps() throws Exception {
		Field[] propnamesFields = {
				Properties.class.getDeclaredField("ALL_CLASS_PROPNAMES"),
				Properties.class.getDeclaredField("ALL_APP_PROPNAMES")};
		for (Field f : propnamesFields) {
			f.setAccessible(true);
			
			@SuppressWarnings("unchecked")
			Set<String> propnames = (Set<String>)f.get(null);
			propnames.add("TestProp");
		}
	}
	

	@Before
	public void setup() throws Exception {
		tempIniFile = Files.createTempFile("testxmx", ".ini");
		Files.write(tempIniFile, contents, Charset.defaultCharset());
		
		uut = XmxIniConfig.load(tempIniFile.toFile(), false);
	}
	
	@After
	public void clean() throws IOException {
		Files.delete(tempIniFile);
	}
	
	@Test
	public void testCustomConfig() throws IOException {
		
		assertTrue(uut.getAppConfig("App1").getClassProperty("Class11", SP_MANAGED).asBool());
		assertEquals("val3", uut.getAppConfig("App1").getClassProperty("Class11", "TestProp").asString());
		
		assertFalse(uut.getAppConfig("App1").getClassProperty("Bad", SP_MANAGED).asBool());
		assertTrue(uut.getAppConfig("AnotherApp").getClassProperty("Bad", SP_MANAGED).asBool());
		assertEquals("val2", uut.getAppConfig("AnotherApp").getAppProperty("TestProp").asString());
		
		assertTrue(uut.getAppConfig("App1").getClassProperty("Class", SP_MANAGED).asBool());
		assertTrue(uut.getAppConfig("App1").getClassProperty("Class$9", SP_MANAGED).asBool());
		
		assertFalse(uut.getAppConfig("App1").getClassProperty("Class2", SP_MANAGED).asBool());
		assertTrue(uut.getAppConfig("App1").getClassProperty("Class21", SP_MANAGED).asBool());
		assertFalse(uut.getAppConfig("App1").getClassProperty("Class22", SP_MANAGED).asBool());
		assertTrue(uut.getAppConfig("App2").getClassProperty("Class21", SP_MANAGED).asBool());
		assertFalse(uut.getAppConfig("other").getClassProperty("Class21", SP_MANAGED).asBool());
		
		assertEquals("val4", uut.getAppConfig("App1").getClassProperty("Class2", "TestProp").asString());
		assertEquals("val5", uut.getAppConfig("App1").getClassProperty("Class21", "TestProp").asString());
		assertEquals("val4", uut.getAppConfig("App1").getClassProperty("Class22", "TestProp").asString());
		assertEquals("val5", uut.getAppConfig("App2").getClassProperty("Class21", "TestProp").asString());
		assertEquals("val6", uut.getAppConfig("other").getClassProperty("Class21", "TestProp").asString());
	}
}

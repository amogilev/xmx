package am.xmx.cfg.impl;

import static am.xmx.cfg.Properties.SP_MANAGED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

	@Before
	public void setup() throws IOException {
		tempIniFile = Files.createTempFile("testxmx", ".ini");
		Files.write(tempIniFile, contents, Charset.defaultCharset());
		
		uut = XmxIniConfig.load(tempIniFile.toFile());
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

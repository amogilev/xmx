// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Checks loading from existing but incomplete file. Standard properties 
 * shall have default values even if missing
 */
public class TestIncompleteXmxIniConfig {
	
	Path tempIniFile;
	XmxIniConfig uut;

	@Before
	public void setup() throws IOException {
		tempIniFile = Files.createTempFile("testxmx", ".ini");
		Files.write(tempIniFile, 
				Arrays.asList("[System]", "EmbeddedWebServer.Port = 8083"), 
				Charset.defaultCharset());
		
		uut = XmxIniConfig.load(tempIniFile.toFile(), false);
		tempIniFile.toFile().deleteOnExit();
	}
	
	@After
	public void clean() throws IOException {
		Files.delete(tempIniFile);
	}
	
	
	@Test
	public void testIncompleteConfig() throws IOException {
		
		assertEquals(true, uut.getSystemProperty(Properties.GLOBAL_EMB_SERVER_ENABLED).asBool());
		assertEquals(8083, uut.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asInt());
		assertEquals("Jetty", uut.getSystemProperty(Properties.GLOBAL_EMB_SERVER_IMPL).asString());
		
		IAppPropertiesSource appConfig = uut.getAppConfig("SomeApp");
		assertTrue(appConfig.getAppProperty(Properties.APP_ENABLED).asBool());
		assertTrue(appConfig.getClassProperty("SomeService", Properties.SP_MANAGED).asBool());
		assertFalse(appConfig.getClassProperty("String", Properties.SP_MANAGED).asBool());
	}

}

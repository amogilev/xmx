// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.*;

public class TestDefaultXmxIniConfig {
	
	XmxIniConfig uut;
	Path testFolder;
	
	@Before
	public void setup() throws IOException {
		testFolder = Files.createTempDirectory("xmxtest");
		System.setProperty("user.home", testFolder.toString());		
		uut = XmxIniConfig.getDefault();
	}
	
	@After
	public void dispose() throws IOException {
		Files.walkFileTree(testFolder, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	@Test
	public void testDefault() throws IOException {
		// check system properties
		assertTrue(uut.getSystemProperty(Properties.GLOBAL_EMB_SERVER_ENABLED).asBool());
		assertEquals(8081, uut.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asInt());
		assertEquals("Jetty", uut.getSystemProperty(Properties.GLOBAL_EMB_SERVER_IMPL).asString());
		
		IAppPropertiesSource appConfig = uut.getAppConfig("MyApp");
		assertTrue(appConfig.getAppProperty(Properties.APP_ENABLED).asBool());
		
		assertFalse(appConfig.getClassProperty("java.lang.Integer", Properties.SP_MANAGED).asBool());
		assertFalse(appConfig.getClassProperty("com.gilecode.SomeClass", Properties.SP_MANAGED).asBool());
		assertTrue(appConfig.getClassProperty("com.gilecode.SomeService", Properties.SP_MANAGED).asBool());
		assertTrue(appConfig.getClassProperty("com.gilecode.SomeServiceImpl", Properties.SP_MANAGED).asBool());
		assertTrue(appConfig.getClassProperty("MyManager", Properties.SP_MANAGED).asBool());
		
		// check that file is created
		Path iniFile = testFolder.resolve(".xmx" + File.separator + "xmx.ini");
		assertTrue(Files.exists(iniFile));
		assertTrue(Files.size(iniFile) > 0);
		
		// check that file may be used for reading
		XmxIniConfig loadedCfg = XmxIniConfig.load(iniFile.toFile(), false);
		assertEquals(8081, loadedCfg.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asInt());
		assertTrue(uut.getAppConfig("MyApp").getClassProperty("com.gilecode.SomeServiceImpl", Properties.SP_MANAGED).asBool());
	}
}
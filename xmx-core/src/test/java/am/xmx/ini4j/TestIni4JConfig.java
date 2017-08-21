// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.ini4j;

import am.xmx.cfg.impl.ConfigDefaults;
import org.ini4j.EnhancedIniBuilder;
import org.ini4j.EnhancedIniConfig;
import org.ini4j.EnhancedIniFormatter;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.ini4j.spi.IniBuilder;
import org.ini4j.spi.IniFormatter;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestIni4JConfig {
	
	private String defaultXmxIniContents =
		"[Global]" + "\n" +
		"EmbeddedWebServer.Enable = true" + "\n" + 
		"# Only Jetty is supported now" + "\n" +
		"EmbeddedWebServer.Impl = Jetty" + "\n" +
		"EmbeddedWebServer.Port = 8081" +  "\n" +
		"\n" +
		"# Per-application settings sections follow, marked as [App=app_name_pattern],"  + "\n" +
		"# where app_name_pattern is Java RegEx pattern (or simple app name)." + "\n" +
		"#"  + "\n" +
		"# Supported are: native application names (like 'tomcat7') and web application" + "\n" +
		"# names running in supported servlet containers (e.g. 'MyWebUI')." + "\n" +
		"#" + "\n" +
		"# As the application name may match several patterns, the settings override" + "\n" + 
		"# each other, and the latest matching setting wins." + "\n" +
		"#"  + "\n" +
		"# default settings for all applications" + "\n" +                                                 
		"[App=*]" + "\n" +
		"# determines instances of which classes and interfaces will be managed by XMX" + "\n" +
		"ManagedBean.ClassNames = *Service|*Manager|*Engine|*DataSource" + "\n\n";
	
	static {
		System.setProperty(IniBuilder.class.getName(), EnhancedIniBuilder.class.getName());
		System.setProperty(IniFormatter.class.getName(), EnhancedIniFormatter.class.getName());
	}
	
	private Ini makeIni() {
		Ini ini = new Ini();
		ini.setConfig(new EnhancedIniConfig(ini));
		ini.getConfig().setLineSeparator(ConfigDefaults.LINE_SEPARATOR);
		ini.getConfig().setEmptySection(true);
		
		return ini;
	}
	
	
	@Test
	public void testIni4j() throws IOException {
		Ini cfg = makeIni();
		
		Section global = cfg.add("Global");
		global.put("EmbeddedWebServer.Enable", true);
		global.put("EmbeddedWebServer.Impl", "Jetty");
		global.putComment("EmbeddedWebServer.Impl", " Only Jetty is supported now");
		global.put("EmbeddedWebServer.Port", "8081");
		
		
		Section allApps = cfg.add("App=*");
		cfg.putComment("App=*", 
				" Per-application settings sections follow, marked as [App=app_name_pattern],\n" +
				" where app_name_pattern is Java RegEx pattern (or simple app name).\n" +
				"\n" +
				" Supported are: native application names (like 'tomcat7') and web application\n" +
				" names running in supported servlet containers (e.g. 'MyWebUI').\n" +
				"\n" +
				" As the application name may match several patterns, the settings override\n" +
				" each other, and the latest matching setting wins.\n" +
				"\n" +
				" default settings for all applications\n");
		
		allApps.add("ManagedBean.ClassNames", "*Service|*Manager|*Engine|*DataSource");
		allApps.putComment("ManagedBean.ClassNames", 
				" determines instances of which classes and interfaces will be managed by XMX");
		
//		cfg.store(System.out);
		
		StringWriter buf = new StringWriter();
		cfg.store(buf);
		
		assertEquals(defaultXmxIniContents, buf.toString());
	}
	
	@Test
	public void testIni4jReadWrite() throws IOException {
		Ini cfg = makeIni();
		
		cfg.load(new StringReader(defaultXmxIniContents));
		StringWriter buf = new StringWriter();
		cfg.store(buf);
		
		assertEquals(defaultXmxIniContents, buf.toString());
	}
	
	@Test
	public void testIni4jRead() throws IOException {
		Ini cfg = makeIni();
		
		cfg.load(new StringReader(defaultXmxIniContents));
		
		Section s = cfg.get("Global");
		assertTrue(s.get("EmbeddedWebServer.Enable", Boolean.class));
		int port = s.get("EmbeddedWebServer.Port", Integer.class);
		assertEquals(8081, port);
		assertEquals("Jetty", s.get("EmbeddedWebServer.Impl"));
	}

}

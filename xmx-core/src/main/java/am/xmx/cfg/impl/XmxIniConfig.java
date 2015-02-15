package am.xmx.cfg.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import am.xmx.cfg.CfgEntity;
import am.xmx.cfg.IAppPropertiesSource;
import am.xmx.cfg.IEntityConfigManager;
import am.xmx.cfg.IXmxConfig;
import am.xmx.cfg.Properties;
import am.xmx.cfg.PropertyValue;

/**
 * Implementation of XMX configuration based on single .ini file accessed with Ini4J.
 * 
 * @author Andrey Mogilev
 */
public class XmxIniConfig implements IXmxConfig, SectionsNamespace {
	
	private static final String XMX_INI = "xmx.ini";
	
	@SuppressWarnings("unused")
	private Ini ini;
	
	private Section systemSection;
	
	/**
	 * All sections listed in reverse order. 
	 * <p/>
	 * Due to override rules, a property from the last matching section
	 * overrides the same property in other sections. Due to reverse order
	 * of this list, the first found property in a matching section will be used. 
	 */
	private List<SectionWithHeader> sectionsReversed;
	
	private ConcurrentHashMap<String, IAppPropertiesSource> appConfigs = new ConcurrentHashMap<>();
	
	XmxIniConfig(Ini ini, Section systemSection, List<SectionWithHeader> sectionsReversed) {
		this.ini = ini;
		this.systemSection = systemSection;
		this.sectionsReversed = sectionsReversed;
	}

	/**
	 * Loads xmx.ini file from non-default place.
	 * 
	 * @param iniFile the configuration file to load
	 * 
	 * @return the loaded configuration
	 * 
	 * @throws IOException if the specified file is missing or corrupted 
	 */
	public static XmxIniConfig load(File iniFile) throws IOException {
		Ini ini = makeIni();
		ini.setFile(iniFile);
		
		try (FileInputStream in = new FileInputStream(iniFile)) {
			ini.load(in);
		}
	
		return XmxIniParser.parse(ini);
	}
	
	/**
	 * Loads xmx.ini file from ${user.home} location, or creates
	 * it and fills with default values. 
	 * 
	 * @return the configuration to use
	 * 
	 * @throws IOException if xmx.ini file in user home folder is corrupted, or cannot be written 
	 */
	public static XmxIniConfig getDefault() throws IOException {
		File userHome = new File(System.getProperty("user.home"));
		File iniFile = new File(userHome, XMX_INI);
		
		Ini ini = makeIni();
		ini.setFile(iniFile);
		
		if (iniFile.createNewFile()) {
			// new file was created
			fillMissingDefaultValues(ini);
			ini.store();
		} else {
			// avoid using Ini4J's load(File), to have more reliable streams closing
			try (FileInputStream in = new FileInputStream(iniFile)) {
				ini.load(in);
				fillMissingDefaultValues(ini);
			}
		}
		
		return XmxIniParser.parse(ini);
	}

	private static void fillMissingDefaultValues(Ini ini) {
		
		// TODO: ensure types of known properties? I.e. early report for incorrect ints etc.
		
		Section global = ensureSection(ini, SECTION_SYSTEM, null);
		checkProperty(global, Properties.GLOBAL_EMB_SERVER_ENABLED, true);
		checkProperty(global, Properties.GLOBAL_EMB_SERVER_IMPL, "Jetty", "Only Jetty is supported now");
		checkProperty(global, Properties.GLOBAL_EMB_SERVER_PORT, 8081);
		
		Section allApps = ensureSection(ini, "App=*", 
				" Per-application settings sections follow, marked as [App=app_name_pattern],\n" +
				" where app_name_pattern is Java RegEx pattern (or simple app name).\n" +
				"\n" +
				" Supported are: native application names (like 'tomcat7') and web application\n" +
				" names running in supported servlet containers (started with '/', e.g. '/MyWebUI').\n" +
				"\n" +
				" As the application name may match several patterns, the settings override\n" +
				" each other, and the latest matching setting wins.\n" +
				"\n" +
				" default settings for all applications\n");
		
		checkProperty(allApps, Properties.APP_ENABLED, true, 
				"Whether management is enabled for an application");
		checkProperty(allApps, Properties.specialClassesForm(Properties.SP_MANAGED), 
				"^.*(Service|Manager|Engine|DataSource)(Impl)?$", 
				"Determines instances of which classes and interfaces will be managed by XMX");
	}

	private static void checkProperty(Section section, String propName, Object defaultValue) {
		checkProperty(section, propName, defaultValue, null);
	}
	
	private static void checkProperty(Section section, String propName, Object defaultValue, String defaultComment) {
		if (!section.containsKey(propName)) {
			section.add(propName, defaultValue);
			if (defaultComment != null) {
				if (!defaultComment.startsWith(" ")) {
					defaultComment = " " + defaultComment;
				}
				section.putComment(propName, defaultComment);
			}
		}
	}

	private static Section ensureSection(Ini ini, String sectionName, String defaultComment) {
		Section s = ini.get(sectionName);	
		if (s == null) {
			s = ini.add(sectionName);
			if (defaultComment != null) {
				ini.putComment(sectionName, defaultComment);
			}
		}
		return s;
	}

	private static Ini makeIni() {
		Ini cfg = new Ini();
		cfg.getConfig().setLineSeparator("\n");
		return cfg;
	}
	
	@Override
	public PropertyValue getSystemProperty(String name) {
		return PropertyValueImpl.of(systemSection.get(name));
	}
	
	@Override
	public IAppPropertiesSource getAppConfig(String appName) {
		IAppPropertiesSource cfg = appConfigs.get(appName);
		if (cfg != null) {
			return cfg;
		}
		
		// although appConfigs is concurrent, we still prefer to avoid
		// multiple creation of sub-configs
		synchronized(this) {
			cfg = appConfigs.get(appName);
			if (cfg != null) {
				return cfg;
			}
			
			// create sub-config
			List<SectionWithHeader> appSections = new ArrayList<>();
			for (SectionWithHeader sh : sectionsReversed) {
				if (sh.getHeader().appMatches(appName)) {
					appSections.add(sh);
				}
			}
			
			cfg = new AppSubConfig(appName, appSections);
			appConfigs.put(appName, cfg);
			return cfg;
		}
	}
	
	//
	// Implementation of IConfigManager is not implemented yet
	// 

	@Override
	public Map<String, String> getAllSystemProperties() {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void setAllSystemProperties(Map<String, String> systemProperties) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public IEntityConfigManager getEntityConfigManager(CfgEntity entity) {
		throw new UnsupportedOperationException("Not implemented yet");
	}
}

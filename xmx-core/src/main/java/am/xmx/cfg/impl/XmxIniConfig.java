package am.xmx.cfg.impl;

import am.ucfg.IConfigInfoProvider;
import am.ucfg.IUpdatingConfigLoader;
import am.ucfg.IUpdatingConfigLoader.ConfigUpdateResult;
import am.ucfg.impl.UpdatingIniConfigLoader;
import am.xmx.cfg.IAppPropertiesSource;
import am.xmx.cfg.IXmxConfig;
import am.xmx.cfg.PropertyValue;
import am.xmx.util.Pair;
import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of XMX configuration based on single .ini file accessed with Ini4J.
 * 
 * @author Andrey Mogilev
 */
public class XmxIniConfig implements IXmxConfig, SectionsNamespace {
	
	private static final String XMX_INI = "xmx.ini";
	
	@SuppressWarnings("unused")
	private Ini ini;
	
	private Map<String, String> systemOptions;
	
	/**
	 * All sections listed in reverse order. 
	 * <p/>
	 * Due to override rules, a property from the last matching section
	 * overrides the same property in other sections. Due to reverse order
	 * of this list, the first found property in a matching section will be used. 
	 */
	private List<SectionWithHeader> sectionsReversed;
	
	private ConcurrentHashMap<String, IAppPropertiesSource> appConfigs = new ConcurrentHashMap<>();
	
	private static IConfigInfoProvider cfgInfoProvider = new ConfigDefaultsInfoProvider();
	private static IUpdatingConfigLoader<Ini> cfgLoader = new UpdatingIniConfigLoader(cfgInfoProvider);

	private XmxIniConfig(Ini ini, Map<String, String> systemOptions, List<SectionWithHeader> sectionsReversed) {
		this.ini = ini;
		this.systemOptions = systemOptions;
		this.sectionsReversed = sectionsReversed;
	}
	
	/**
	 * Loads xmx.ini file from the specified place.
	 * 
	 * @param iniFile the configuration file to load
	 * @param rewriteAllowed whether re-write of the file with new auto-comments is allowed
	 * 
	 * @return the loaded configuration
	 */
	public static XmxIniConfig load(File iniFile, boolean rewriteAllowed) {
		ConfigUpdateResult<Ini> result = cfgLoader.loadAndUpdate(iniFile, rewriteAllowed);
		
		// parse resulting merged .ini into internal format, and make non-overridden 
		// defoptions available as regular properties
		
		XmxCfgSectionNameParser sectionParser = new XmxCfgSectionNameParser();
		
		ArrayList<SectionWithHeader> sections = new ArrayList<>();
		
		Map<String, String> systemOptions = null;
		for (Pair<String, Map<String, String>> sectionEntry : result.getSectionsWithOptionsByName()) {
			String curSectionName = sectionEntry.getFirst().trim();
			Map<String, String> optionsByName = sectionEntry.getSecond();
			if (curSectionName.equals(SECTION_SYSTEM)) {
				if (systemOptions != null) {
					// unexpected - Ini4J should merge sections
					throw new XmxIniParseException("Duplicate [System] section");
				}
				systemOptions = optionsByName;
			} else {
				SectionHeader header = sectionParser.parseSectionHeader(curSectionName); 
				sections.add(new SectionWithHeader(header, optionsByName));
			}
		}
		
		sections.trimToSize();
		Collections.reverse(sections);
		
		return new XmxIniConfig(result.getRawConfig(), systemOptions, sections);
	}
	
	
	/**
	 * Loads xmx.ini file from ${user.home} location, or creates
	 * it and fills with default values. 
	 * 
	 * @return the configuration to use
	 * 
	 * @throws IOException if xmx.ini file in user home folder is corrupted, or cannot be written 
	 */
	public static XmxIniConfig getDefault() {
		File userHome = new File(System.getProperty("user.home"));
		File iniFile = new File(userHome, XMX_INI);
		
		return load(iniFile, true);
	}

	
	@Override
	public PropertyValue getSystemProperty(String name) {
		return PropertyValueImpl.of(systemOptions.get(name));
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

	@Override
	public void overrideSystemProperties(Map<String, String> properties) {
		if (properties.isEmpty()) {
			return;
		}
		
		Set<String> knownSystemProperties = ConfigDefaults.SECTION_SYSTEM_DESC.getOptionsByName().keySet();
		for (Entry<String, String> e : properties.entrySet()) {
			String name = e.getKey();
			// NOTE: as case-insensitive comparison is used, iterate all known properties. OK as there are few of them...
			for (String canonicName : knownSystemProperties) {
				if (canonicName.equalsIgnoreCase(name)) {
					systemOptions.put(canonicName, e.getValue());
					break;
				}
			}
		}
	}

	@Override
	public File getConfigurationFile() {
		return ini.getFile();
	}

	@Override
	public void onLoggingInitialized() {
		cfgInfoProvider.onLoggingInitialized();
	}

	//
	// Implementation of IConfigManager is not implemented yet
	// 

//	@Override
//	public Map<String, String> getAllSystemProperties() {
//		throw new UnsupportedOperationException("Not implemented yet");
//	}
//
//	@Override
//	public void setAllSystemProperties(Map<String, String> systemProperties) {
//		throw new UnsupportedOperationException("Not implemented yet");
//	}
//
//	@Override
//	public IEntityConfigManager getEntityConfigManager(CfgEntity entity) {
//		throw new UnsupportedOperationException("Not implemented yet");
//	}
}

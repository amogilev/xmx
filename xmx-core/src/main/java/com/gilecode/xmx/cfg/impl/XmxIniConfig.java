// Copyright © 2015-2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.ucfg.ConfigLoadStatus;
import com.gilecode.ucfg.IConfigInfoProvider;
import com.gilecode.ucfg.IUpdatingConfigLoader;
import com.gilecode.ucfg.impl.UpdatingIniConfigLoader;
import com.gilecode.xmx.cfg.IAppPropertiesSource;
import com.gilecode.xmx.cfg.IXmxConfig;
import com.gilecode.xmx.cfg.PropertyValue;
import com.gilecode.xmx.util.Pair;
import org.ini4j.Ini;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of XMX configuration based on single .ini file accessed with Ini4J.
 *
 * @author Andrey Mogilev
 */
public class XmxIniConfig implements IXmxConfig, SectionsNamespace {

	static final String XMX_INI = "xmx.ini";
	static final String CUSTOM_CONFIG_FILE = "config";

	@SuppressWarnings("unused")
	private final Ini ini;

	private final Map<String, String> systemOptions;

	/**
	 * All sections listed in reverse order.
	 * <p/>
	 * Due to override rules, a property from the last matching section
	 * overrides the same property in other sections. Due to reverse order
	 * of this list, the first found property in a matching section will be used.
	 */
	private final List<SectionWithHeader> sectionsReversed;
	private final ConfigLoadStatus loadStatus;

	private ConcurrentHashMap<String, IAppPropertiesSource> appConfigs = new ConcurrentHashMap<>();

	private static IConfigInfoProvider cfgInfoProvider = new ConfigDefaultsInfoProvider();
	private static IUpdatingConfigLoader<Ini> cfgLoader = new UpdatingIniConfigLoader(cfgInfoProvider);

	private XmxIniConfig(Ini ini, Map<String, String> systemOptions, List<SectionWithHeader> sectionsReversed,
						 ConfigLoadStatus loadStatus) {
		this.ini = ini;
		this.systemOptions = systemOptions;
		this.sectionsReversed = sectionsReversed;
		this.loadStatus = loadStatus;
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
		IUpdatingConfigLoader.ConfigUpdateResult<Ini> result = cfgLoader.loadAndUpdate(iniFile, rewriteAllowed);

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

		return new XmxIniConfig(result.getRawConfig(), systemOptions, sections, result.getStatus());
	}

	/**
	 * Loads xmx.ini file from '${user.home}/.xmx/' location, or creates
	 * it and fills with default values.
	 *
	 * @param overrideSystemProps (optional) properties to override; or {@code null}
	 * @return the configuration to use
	 */
	public static XmxIniConfig getDefault(Map<String, String> overrideSystemProps) {
		File iniFile;
		File configDir;
		if (overrideSystemProps != null && overrideSystemProps.containsKey(CUSTOM_CONFIG_FILE)) {
			iniFile = new File(overrideSystemProps.get(CUSTOM_CONFIG_FILE));
			configDir = iniFile.getParentFile();
		} else {
			configDir = defaultConfigDir();
			iniFile = new File(configDir, XMX_INI);
		}
		if (configDir != null && !configDir.exists()) {
			configDir.mkdirs();
		}
		XmxIniConfig config = load(iniFile, true);
		if (overrideSystemProps != null) {
			config.overrideSystemProperties(overrideSystemProps);
		}
		return config;
	}

	public static File defaultConfigDir() {
		return new File(System.getProperty("user.home") + File.separator + ".xmx");
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

	/**
	 * Overrides known global (XMX [System]) configuration properties
	 * <p/>
	 * Property names are compared case-insensitively, so any case of override
	 * properties is accepted.
	 * <p/>
	 * As most of properties are used only at startup, the override takes place
	 * only if done before the actual use of those properties.
	 *
	 * @param properties properties to override
	 */
	private void overrideSystemProperties(Map<String, String> properties) {
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

	@Override
	public ConfigLoadStatus getLoadStatus() {
		return loadStatus;
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

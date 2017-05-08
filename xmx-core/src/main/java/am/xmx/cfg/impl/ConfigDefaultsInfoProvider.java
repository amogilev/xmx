package am.xmx.cfg.impl;

import am.ucfg.IConfigInfoProvider;
import am.ucfg.SectionDescription;
import am.xmx.cfg.CfgEntityLevel;
import am.xmx.cfg.Properties;

import java.util.List;

/**
 * Provides information about the default XMX configuration sections and
 * options.
 * <p/>
 * This implementation is NOT thread-safe!
 * 
 * @author Andrey Mogilev
 */
public class ConfigDefaultsInfoProvider implements IConfigInfoProvider {
	
	private String lastSectionName;
	private CfgEntityLevel lastSectionLevel;

	@Override
	public boolean isSupportedOption(String sectionName, String optionName) {
		CfgEntityLevel level;
		if (!sectionName.equals(lastSectionName)) {
			SectionHeader sh = new XmxCfgSectionNameParser().parseSectionHeader(sectionName);
			lastSectionLevel = sh.level;
			lastSectionName = sectionName;
		}
		level = lastSectionLevel;

		return Properties.isKnownProperty(level, optionName);
	}

	@Override
	public String[] getFileComments() {
		return ConfigDefaults.FILE_COMMENTS;
	}
	
	@Override
	public List<SectionDescription> getAllDefaultSectionsDescriptions() {
		return ConfigDefaults.ALL_SECTIONS;
	}

	@Override
	public List<SectionDescription> getAllDefaultHiddenSectionsDescriptions() {
		return ConfigDefaults.HIDDEN_INTERNAL_SECTIONS;
	}

	@Override
	public void logError(String message) {
		System.err.println(message);
	}
	
	@Override
	public void logWarning(String message) {
		System.err.println(message);
	}

	@Override
	public String getLineSeparator() {
		return ConfigDefaults.LINE_SEPARATOR;
	}

}

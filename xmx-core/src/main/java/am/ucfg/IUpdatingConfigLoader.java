package am.ucfg;

import am.xmx.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Abstract interface for a config loader, which updates the configuration during 
 * the loading. The configuration setting and defaults are provided by 
 * {@link IConfigInfoProvider} bound to the loader implementation. 
 * 
 * @author Andrey Mogilev
 *
 * @param <T> the type of the in-memory configuration object returned within the loading result
 */
public interface IUpdatingConfigLoader<T> {
	
	/**
	 * The result of configuration update.
	 */
	class ConfigUpdateResult<T> {
		
		/**
		 * Whether the update was required.
		 */
		private boolean isUpdated;
		
		/**
		 * Ordered list of section represented as the section name and all options of that section;
		 * section options are maps of options values by name.
		 * <p/>
		 * The options and/or sections may override each other; in case of duplication, the latter one is used.
		 */
		private List<Pair<String, Map<String, String>>> sectionsWithOptionsByName;
		
		/**
		 * Loaded configuration object, with updated comments but without 
		 * the default options. The actual type is implementation-dependent.   
		 */
		private T rawConfig;
		
		public ConfigUpdateResult(boolean isUpdated,
								  List<Pair<String, Map<String, String>>> sectionsWithOptionsByName,
								  T rawConfig) {
			this.isUpdated = isUpdated;
			this.sectionsWithOptionsByName = sectionsWithOptionsByName;
			this.rawConfig = rawConfig;
		}

		public boolean isUpdated() {
			return isUpdated;
		}

		public List<Pair<String, Map<String, String>>> getSectionsWithOptionsByName() {
			return sectionsWithOptionsByName;
		}

		public T getRawConfig() {
			return rawConfig;
		}
	}
	
	/**
	 * Loads and updates the configuration from the file. If rewrite is allowed
	 * and updates are necessary, the updates are written back to the file; otherwise,
	 * they are stored only in memory.
	 * <p/>
	 * In case of any errors, including the absence of the file or failure to load or
	 * write, the default configuration is returned. The errors are reported to the 
	 * used {@link IConfigInfoProvider}.
	 *  
	 * @param cfgFile the configuration file to load
	 * @param rewriteAllowed whether to re-write the updated file
	 * 
	 * @return the loaded configuration
	 */
	ConfigUpdateResult<T> loadAndUpdate(File cfgFile, boolean rewriteAllowed);
}

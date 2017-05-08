package am.ucfg;

import java.util.List;

/**
 * Provides information about the specific format of the configuration
 * file to the configuration reader implementation, like allowed sections
 * and options, default values for the options etc.
 * 
 * @author Andrey Mogilev
 */
public interface IConfigInfoProvider {
	
	/**
	 * Returns the list of all non-hidden default sections and options.
	 */
	List<SectionDescription> getAllDefaultSectionsDescriptions();

	/**
	 * Returns the list of all default hidden sections and options.
	 * These options are not written into the config file, but override all other options
	 */
	List<SectionDescription> getAllDefaultHiddenSectionsDescriptions();

	/**
	 * Returns whether an option with the specified name is supported
	 * for the specified section.
	 * <p/>
	 * If the option is not supported, it will be removed from the configuration.
	 * 
	 * @param sectionName the name of the section where the option is found 
	 * @param optionName the option name
	 * 
	 * @return {@code true} if the option is supported and shall be kept; {@code false} otherwise
	 */
	boolean isSupportedOption(String sectionName, String optionName);
	
	/**
	 * Reports an error found during loading of the configuration file.
	 */
	void logError(String message);
	
	/**
	 * Reports a warning found during loading of the configuration file.
	 */
	void logWarning(String message);

	/**
	 * Returns the line separator to use in configuration file.
	 */
	String getLineSeparator();

	/**
	 * Returns lines of the global file comment.
	 */
	String[] getFileComments();

}

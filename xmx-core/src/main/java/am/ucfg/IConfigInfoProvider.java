// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.ucfg;

import org.slf4j.Logger;

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
	 * Returns the line separator to use in configuration file.
	 */
	String getLineSeparator();

	/**
	 * Returns lines of the global file comment.
	 */
	String[] getFileComments();

	/**
	 * Returns a logger to use for the logging class.
	 * <p/>
	 * Used to defer logging events of the configuration reader until the configuration is completely read, processed
	 * and logging is initialized (as logging configuration may depend on the configuration being read)
	 */
	Logger getLogger(Class<?> loggingClass);

	/**
	 * Invoked when logging is initialized and so the deferred logging events may be printed.
	 */
	void onLoggingInitialized();
}

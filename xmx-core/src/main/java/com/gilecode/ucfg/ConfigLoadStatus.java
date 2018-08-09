// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.ucfg;

/**
 * Enumerates possible results of configuration loading.
 *
 * @author Andrey Mogilev
 */
public enum ConfigLoadStatus {

	/**
	 * A new configuration file was created, with all default settings
	 */
	NEW,

	/**
	 * Failed to load configuration, e.g. because of syntax errors. The default configurations
	 * settings are used instead.
	 */
	FAIL,

	/**
	 * The configuration was loaded and updated, either because of unknown options found, duplicate sections merged,
	 * comments transformed, or new options added.
	 */
	UPDATED,

	/**
	 * The configuration file was read, no update required.
	 */
	SUCCESS;

	public String message() {
		return this == FAIL ? "FAIL: default options are used" : name();
	}
}

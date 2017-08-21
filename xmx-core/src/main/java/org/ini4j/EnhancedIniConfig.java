// Copyright © 2015 Andrey Mogilev. All rights reserved.

package org.ini4j;

/**
 * Ini config bound to the Ini file.
 * 
 * @author Andrey Mogilev
 */
public class EnhancedIniConfig extends Config {
	
	private static final long serialVersionUID = 3787997497305978458L;
	
	private Ini ini;

	public EnhancedIniConfig(Ini ini) {
		this.ini = ini;
		reset();
	}

	public Ini getIni() {
		return ini;
	}
}

// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

/**
 * Known section names and part of complex names.
 *  
 * @author Andrey Mogilev
 */
public interface SectionsNamespace {
	
	/** The global [System] section*/
	String SECTION_SYSTEM = "System";
	
	/** The common [App=*] section*/
	String SECTION_ALLAPPS = "App=*";
	
	/** Parts keys of the comlex section headers, like in [App=Foo;Class=Bar;Member=*] */
	String PART_APP = "App";
	String PART_CLASS = "Class";
	String PART_MEMBER = "Member";


}

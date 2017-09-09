// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

public interface UIConstants {

	// default JSON chars limit for object on 'details' page
	long OBJ_JSON_CHARS_LIMIT = 2_000_000;

	// default JSON chars limit for object's fields on 'details' page
	long OBJ_FIELDS_JSON_CHARS_LIMIT = 10_000;

	// default JSON chars limit for object selection on 'classObjects' page
	long CLASS_OBJS_JSON_CHARS_LIMIT = 2_000;

	String REFPATH_PREFIX = "$";

	// a special ID which means that an object or class is not actuallly managed, and
	// the information about it is obtained using refpaths
	// TODO: replace with nulls
	int ID_UNMANAGED = -1;

}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core.params;

/**
 * Consumer for extracted parameter names.
 */
public interface IParamNamesConsumer {

	/**
	 * Passes extracted parameter names. If all names are null, then no information was extracted.
	 */
	void consume(String[] argNames);

}

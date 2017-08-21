// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg;

public interface IEntityPropertyOverride {
	
	/**
	 * Returns part of header specification which identifies the
	 * sub-section where override property is found.
	 */
	String getSubentitySpec();
	
	/**
	 * Returns the override property value.
	 */
	String getOverrideValue();

}

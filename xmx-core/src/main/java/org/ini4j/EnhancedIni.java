package org.ini4j;

import java.io.IOException;
import java.io.Writer;

import org.ini4j.spi.IniHandler;

@SuppressWarnings("serial")
public class EnhancedIni extends Ini {
	
	@Override
	protected IniHandler newBuilder() {
		return new EnhancedIniBuilder(this);
	}

	@Override
	public void store(Writer out) throws IOException {
		super.store(EnhancedIniFormatter.newInstance(this, out));
	}


}

// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg.impl;

import java.util.Map;

class SectionWithHeader {
	
	private SectionHeader header;
	private Map<String, String> optionsByName;
	
	SectionWithHeader(SectionHeader header, Map<String, String> optionsByName) {
		this.header = header;
		this.optionsByName = optionsByName;
	}

	SectionHeader getHeader() {
		return header;
	}
	
	boolean containsKey(String key) {
		return optionsByName.containsKey(key);
	}
	
	public String get(String key) {
		return optionsByName.get(key);
	}

	@Override
	public String toString() {
		return "SectionWithHeader" + header;
	}
}

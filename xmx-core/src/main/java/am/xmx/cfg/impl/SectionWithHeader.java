package am.xmx.cfg.impl;

import java.util.Map;

class SectionWithHeader {
	
	private SectionHeader header;
	private Map<String, String> optionsByName;
	
	public SectionWithHeader(SectionHeader header, Map<String, String> optionsByName) {
		this.header = header;
		this.optionsByName = optionsByName;
	}

	public SectionHeader getHeader() {
		return header;
	}
	
	public Map<String, String> getOptionsByName() {
		return optionsByName;
	}

	public boolean containsKey(String key) {
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

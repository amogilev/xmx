package am.xmx.cfg.impl;

import org.ini4j.Profile.Section;

class SectionWithHeader {
	
	private SectionHeader header;
	private Section section;
	
	public SectionWithHeader(SectionHeader header, Section section) {
		this.header = header;
		this.section = section;
	}

	public SectionHeader getHeader() {
		return header;
	}

	public Section getSection() {
		return section;
	}

	@Override
	public String toString() {
		return "SectionWithHeader" + header;
	}

}

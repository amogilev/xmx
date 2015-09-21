package org.ini4j;

import org.ini4j.Profile.Section;
import org.ini4j.spi.IniFormatter;

public class EnhancedIniFormatter extends IniFormatter {
	
	private Ini ini;
	private String curSectionName;
	
	public EnhancedIniFormatter() {
	}
	
	@Override
	protected void setConfig(Config config) {
		super.setConfig(config);
		this.ini = ((EnhancedIniConfig)config).getIni();
	}

	@Override
	public void startSection(String sectionName) {
		super.startSection(sectionName);
		curSectionName = sectionName;
	}


	@Override
	public void endSection() {
		Section curSection = ini.get(curSectionName);
		String comment = curSection.getComment(EnhancedIniCommentsSupport.ENDSECT_ANCHOR);
		if (comment != null) {
			handleComment(comment);
		}
		super.endSection();
		
		curSectionName = null;
	}


}

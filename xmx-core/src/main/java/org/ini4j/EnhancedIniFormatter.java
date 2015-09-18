package org.ini4j;

import java.io.PrintWriter;
import java.io.Writer;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.ini4j.spi.IniFormatter;

public class EnhancedIniFormatter extends IniFormatter {
	
	private final Ini ini;
	private String curSectionName;
	
	public static IniFormatter newInstance(Ini ini, Writer out) {
		PrintWriter pw = (out instanceof PrintWriter) ? (PrintWriter) out : new PrintWriter(out);
		IniFormatter instance = new EnhancedIniFormatter(ini, pw, ini.getConfig());
		
		return instance;
	}
	
	private EnhancedIniFormatter(Ini ini, PrintWriter pw, Config config) {
		this.ini = ini;
        setOutput(pw);
        setConfig(config);
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

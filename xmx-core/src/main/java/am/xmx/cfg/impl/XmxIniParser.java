package am.xmx.cfg.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class XmxIniParser implements SectionsNamespace {
	
	// shared map for parsed parts of section names
	Map<String, String> sectionNameParts = new HashMap<>(4);
	
	public static XmxIniConfig parse(Ini ini) {
		return new XmxIniParser().parseSections(ini);
	}

	private XmxIniConfig parseSections(Ini ini) {
		Section systemSection = null;
		
		ArrayList<SectionWithHeader> sections = new ArrayList<>();
		
		for (Entry<String, Section> e : ini.entrySet()) {
			String curSectionName = e.getKey().trim();
			Section section = e.getValue();
			if (curSectionName.equals(SECTION_SYSTEM)) {
				if (systemSection != null) {
					throw new XmxIniParseException("Duplicate [System] section");
				}
				systemSection = section;
				continue;
			}
			
			SectionHeader header = parseSectionHeader(curSectionName); 
			sections.add(new SectionWithHeader(header, section));
		}
		
		sections.trimToSize();
		Collections.reverse(sections);
		
		return new XmxIniConfig(ini, systemSection, sections);
	}
	
	private SectionHeader parseSectionHeader(String curSectionName) {
		parseSectionNameParts(curSectionName);
		if (!sectionNameParts.containsKey(PART_APP)) {
			throw new XmxIniParseException("Wrong section name - no App part: " + curSectionName);
		}
		
		// parse patterns in header parts
		SectionHeader header = new SectionHeader();
		for (Entry<String, String> e : sectionNameParts.entrySet()) {
			String spec = e.getValue();
			Pattern pattern = PatternsSupport.parse(spec);
			switch (e.getKey()) {
			case PART_APP:
				header.appSpec = spec;
				header.appPattern = pattern;
				break;
			case PART_CLASS:
				header.classSpec = spec;
				header.classPattern = pattern;
				break;
			case PART_MEMBER:
				header.memberSpec = spec;
				header.memberPattern = pattern;
				break;
			default:
				throw new XmxIniParseException("Unrecognized property: '" + e.getKey() + 
						" in section name [" + curSectionName + "]"); 
			}
		}
		return header;
	}

	// TODO unit tests

	void parseSectionNameParts(String str) {
		sectionNameParts.clear();
		
		int partStart = 0;
		for (;;) {
			int partEnd = findUnquotedChar(str, ',', partStart);
			if (partEnd > 0) {
				parseSectionNamePart(str.substring(partStart, partEnd));
				partStart = partEnd + 1;
			} else {
				parseSectionNamePart(str.substring(partStart));
				return;
			}
		}
	}

	void parseSectionNamePart(String part) {
		int n = findUnquotedChar(part, '=', 0);
		if (n < 0) {
			// whole part is a key, with empty value 
			sectionNameParts.put(part.trim(), "");
		} else {
			String key = part.substring(0, n).trim();
			String value = part.substring(n + 1).trim();
			sectionNameParts.put(key, value);
		}
	}

	static int findUnquotedChar(String str, char c, int from) {
		int len = str.length();
		boolean quoted = false, escaped = false;
		for (int i = 0; i < len; i++) {
			char ch = str.charAt(i);
			if (ch == c && !quoted) {
				return i;
			} else if (ch == '\\') {
				escaped = !escaped;
			} else if (escaped) {
				escaped = false;
			} else if (ch == '"') {
				quoted = !quoted;
			}
		}
		return -1;
	}
}

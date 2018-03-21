// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.xmx.cfg.pattern.PatternsSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class XmxCfgSectionNameParser implements SectionsNamespace {
	
	// shared map for parsed parts of section names
	Map<String, String> sectionNameParts = new HashMap<>(4);
	
	public SectionHeader parseSectionHeader(String curSectionName) {
		parseSectionNameParts(curSectionName);
		if (sectionNameParts.containsKey(SECTION_SYSTEM)) {
			if (sectionNameParts.size() > 1) {
				throw new XmxIniParseException("Wrong section name - System part is not compatible with anything else: '" 
						+ curSectionName + "'");
			}
			return new SectionHeader();
		}
		if (!sectionNameParts.containsKey(PART_APP)) {
			throw new XmxIniParseException("Wrong section name - no App part: '" + curSectionName + "'");
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
		header.initLevel();
		return header;
	}

	void parseSectionNameParts(String str) {
		sectionNameParts.clear();
		
		int partStart = 0;
		for (;;) {
			int partEnd = findUnquotedChar(str, ';', partStart);
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
			String key = unquote(part.trim());
			if (!key.isEmpty()) {
				sectionNameParts.put(key, "");
			}
		} else {
			String key = unquote(part.substring(0, n).trim());
			String value = part.substring(n + 1).trim();
			sectionNameParts.put(key, value);
		}
	}

	private String unquote(String str) {
		return PatternsSupport.unquote(str);
	}

	static int findUnquotedChar(String str, char c, int from) {
		int len = str.length();
		boolean quoted = false, escaped = false;
		for (int i = Math.max(0, from); i < len; i++) {
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

// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package am.xmx.cfg.impl;

import am.ucfg.OptionDescription;
import am.ucfg.SectionDescription;
import am.xmx.cfg.Properties;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;

import static am.ucfg.impl.IniSettings.*;
import static am.xmx.cfg.impl.ConfigDefaults.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestUpdateXmxIniConfig {
	
	// tests that auto-created config is not updated at next run
	@Test
	public void testUpdateNotRequiredForAutoCreatedConfig() throws IOException {
		Path tempIniFile = Files.createTempFile("testxmx", ".ini");
		
		try {
			// write default config
			XmxIniConfig.load(tempIniFile.toFile(), true);
			
			FileTime modifiedTime = Files.getLastModifiedTime(tempIniFile);
			
			// re-open it
			XmxIniConfig.load(tempIniFile.toFile(), true);
			
			// make sure not updated
			assertEquals(modifiedTime, Files.getLastModifiedTime(tempIniFile));
		} finally {
			Files.delete(tempIniFile);
		}
	}
	
	// tests that default config (created by test) is not updated at next run
	@Test
	public void testUpdateNotRequiredForDefaultConfig() throws Exception {
		List<String> lines = makeDefaultIniLines();
		checkUpdateNotRequiredForLines(lines);
	}
	
	@Test
	public void testUpdateNotRequiredWithManualCommentsAndExtraSection() throws Exception {
		List<String> lines = makeDefaultIniLines();
		lines.add(0, "# Manual comment");
		lines.add(4, "# Manual comment 2");
		lines.add(20, "# Manual comment 3");
		lines.add(25, "# Manual comment 4");
		lines.add("# Manual comment");
		lines.add("");
		lines.add("# Manually added section");
		lines.add("[App=My*]");
		lines.add("AppManagementEnabled=false");
		
		checkUpdateNotRequiredForLines(lines);
	}
	
	@Test
	public void testUpdateNotRequiredWithDuplicateManualSections() throws Exception {
		List<String> initialLines = makeDefaultIniLines();
		initialLines.add("");
		initialLines.add("[App=My]");
		initialLines.add("AppManagementEnabled = 1");
		initialLines.add("");
		initialLines.add("[App=My]");
		initialLines.add("ManagedClasses = *DataSource");
		initialLines.add("");
		initialLines.add("[App=My]");
		initialLines.add("MaxInstances = 100");
		
		checkUpdateNotRequiredForLines(initialLines);
	}
	
	@Test
	public void testUpdateNotRequiredWithOverrides() throws Exception {
		Map<String, List<String>> optionsOverrides = new HashMap<>();
		optionsOverrides.put(Properties.APP_ENABLED, 
				makeOptionLinesWithOverrideValue(SECTION_ALLAPPS_DESC, Properties.APP_ENABLED, "1", true));
		
		String mngOptName = Properties.specialClassesForm(Properties.SP_MANAGED);
		optionsOverrides.put(mngOptName, 
				makeOptionLinesWithOverrideValue(SECTION_ALLAPPS_DESC, mngOptName, "*Connection", true));
		
		List<String> overrideLines = makeSectionLinesWithOptionsOverride(SECTION_ALLAPPS_DESC, 
				optionsOverrides);
		List<String> lines = makeIniLinesWithSectionOverride(SECTION_ALLAPPS_DESC.getName(), 
				overrideLines);
		
		checkUpdateNotRequiredForLines(lines);
	}
	
	@Test
	public void testUpdateNotRequiredWithChangedOrder() throws Exception {
		SectionDescription section = SECTION_ALLAPPS_DESC;
		List<String> sectionLines = new ArrayList<>();
		List<OptionDescription> reversedOptions = Arrays.asList(SECTION_ALLAPPS_DESC.getOptions());
		Collections.reverse(reversedOptions);
		
		for (String line : section.getSectionComments()) {
			sectionLines.add("#" + (line.isEmpty() ? AUTO_EMPTY_LINE : AUTO_COMMENT_PREFIX + line));
		}
		sectionLines.add("[" + section.getName() + "]");
		for (OptionDescription option : reversedOptions) {
			sectionLines.addAll(makeDefaultOptionLines(option));
		}
		sectionLines.add("");
		
		List<String> lines = makeIniLinesWithSectionOverride(section.getName(), sectionLines);
		checkUpdateNotRequiredForLines(lines);
	}
	
	
	@Test
	public void testUpdateWithOptionOverrideByUncommenting() throws Exception {
		Map<String, List<String>> optionsOverrides = new HashMap<>();
		
		String mngOptName = Properties.specialClassesForm(Properties.SP_MANAGED);
		optionsOverrides.put(mngOptName, 
				makeOptionLinesWithOverrideValue(SECTION_ALLAPPS_DESC, mngOptName, "*Connection", true));
		optionsOverrides.put(Properties.CLASS_MAX_INSTANCES, 
				makeOptionLinesWithOverrideValue(SECTION_ALLAPPS_DESC, Properties.CLASS_MAX_INSTANCES, "123", false));
		
		List<String> overrideLines = makeSectionLinesWithOptionsOverride(SECTION_ALLAPPS_DESC, 
				optionsOverrides);
		List<String> initialLines = makeIniLinesWithSectionOverride(SECTION_ALLAPPS_DESC.getName(), 
				overrideLines);
		
		// expected contents after the update
		optionsOverrides.put(Properties.CLASS_MAX_INSTANCES, 
				makeOptionLinesWithOverrideValue(SECTION_ALLAPPS_DESC, Properties.CLASS_MAX_INSTANCES, "123", true));
		overrideLines = makeSectionLinesWithOptionsOverride(SECTION_ALLAPPS_DESC, 
				optionsOverrides);
		List<String> expectedLines = makeIniLinesWithSectionOverride(SECTION_ALLAPPS_DESC.getName(), 
				overrideLines);
		
		checkUpdateRequiredForLines(initialLines, expectedLines);
	}
	
	@Test
	public void testUpdateWithDuplicateManualSection() throws Exception {
		List<String> initialLines = makeDefaultIniLines();
		initialLines.set(0, "#!# Changed!"); // for triggering update
		
		initialLines.add("[App=My]");
		initialLines.add("AppManagementEnabled = 1");
		initialLines.add("");
		initialLines.add("[App=My]");
		initialLines.add("ManagedClasses = *DataSource");
		initialLines.add("");
		initialLines.add("[App=My]");
		initialLines.add("MaxInstances = 100");
		
		List<String> expectedLines = makeDefaultIniLines();
		expectedLines.add("[App=My]");
		expectedLines.add("AppManagementEnabled = 1");
		expectedLines.add("ManagedClasses = *DataSource");
		expectedLines.add("MaxInstances = 100");
		expectedLines.add("");
		
		checkUpdateRequiredForLines(initialLines, expectedLines);
	}
	
	private void checkUpdateNotRequiredForLines(List<String> lines) throws Exception {
		Path tempIniFile = Files.createTempFile("testxmx", ".ini");
		Files.write(tempIniFile,
				lines,
				Charset.defaultCharset());
		FileTime modifiedTime = Files.getLastModifiedTime(tempIniFile);
		Thread.sleep(10); // prevents tests completion at the same time quant
		
		try {
			XmxIniConfig.load(tempIniFile.toFile(), true);
			
			if (!modifiedTime.equals(Files.getLastModifiedTime(tempIniFile))) {
				List<String> lines2 = Files.readAllLines(tempIniFile, Charset.forName("UTF-8"));
				for (String line : lines2) {
					System.out.println(line);
				}
				fail("Expected no update for the test configuration file, but the update was detected");
			}
			
		} finally {
			Files.delete(tempIniFile);
		}
	}
	
	private void checkUpdateRequiredForLines(List<String> initialLines, List<String> expectedLines) throws Exception {
		Path tempIniFile = Files.createTempFile("testxmx", ".ini");
		Files.write(tempIniFile,
				initialLines,
				Charset.defaultCharset());
		FileTime modifiedTime = Files.getLastModifiedTime(tempIniFile);
		Thread.sleep(10); // prevents tests completion at the same time quant
		
		try {
			XmxIniConfig.load(tempIniFile.toFile(), true);
			
			if (modifiedTime.equals(Files.getLastModifiedTime(tempIniFile))) {
				fail("Expected an update for the test configuration file, but no update was detected");
			}

			List<String> lines2 = Files.readAllLines(tempIniFile, Charset.forName("UTF-8"));
			if (!lines2.equals(expectedLines)) {
				System.out.println("---- FOUND CONTENTS ----");
				int num = 0;
				for (String line : lines2) {
					System.out.println("" + ++num + ": " + line);
				}
				System.out.println("\n\n---- EXPECTED CONTENTS ----");
				num = 0;
				for (String line : expectedLines) {
					System.out.println("" + ++num + ": " + line);
				}
				
				num = 0;
				for (String line : lines2) {
					String expected;
					boolean matches;
					if (num >= expectedLines.size()) {
						expected = "<__MISSING__>"; 
						matches = false;
					} else {
						expected = expectedLines.get(num);
						matches = expected.equals(line);
					}
					
					if (!matches) {
						fail("Found and expected content lines are different. First discrepancy is at line " + 
								(num + 1) + ":\n" +
								"Expected: " + expected + "\n" +
								"Found: " + line + "\n");
					}
					num++;
				}
				
			}
			assertEquals(lines2, expectedLines);
			
			
		} finally {
			Files.delete(tempIniFile);
		}
	}
	
	private List<String> makeDefaultIniLines() {
		return makeIniLinesWithSectionOverride(null, null);
	}
	
	private List<String> makeIniLinesWithSectionOverride(String sectionName, List<String> sectionLines) {
		List<String> result = new ArrayList<>();
		for (String line : FILE_COMMENTS) {
			result.add("#" + (line.isEmpty() ? AUTO_EMPTY_LINE : AUTO_COMMENT_PREFIX + line));
		}
		result.add("");
		for (SectionDescription section : ALL_SECTIONS) {
			if (sectionName != null && section.getName().equals(sectionName)) {
				result.addAll(sectionLines);
			} else {
				result.addAll(makeDefaultSectionLines(section));
			}
		}
		return result;
	}

	private List<String> makeDefaultSectionLines(SectionDescription section) {
		return makeSectionLinesWithOptionsOverride(section, null);
	}
	
	private List<String> makeSectionLinesWithOptionsOverride(SectionDescription section,
			Map<String, List<String>> optionsOverrides) {
		List<String> result = new ArrayList<>();
		for (String line : section.getSectionComments()) {
			result.add("#" + (line.isEmpty() ? AUTO_EMPTY_LINE : AUTO_COMMENT_PREFIX + line));
		}
		result.add("[" + section.getName() + "]");
		for (OptionDescription option : section.getOptions()) {
			if (optionsOverrides != null && optionsOverrides.containsKey(option.getName())) {
				result.addAll(optionsOverrides.get(option.getName()));
			} else {
				result.addAll(makeDefaultOptionLines(option));
			}
		}
		result.add("");
		return result;
	}

	private List<String> makeDefaultOptionLines(OptionDescription option) {
		return makeOptionLinesWithOverrideValue(option, null, true);
	}
	
	private List<String> makeOptionLinesWithOverrideValue(SectionDescription sectionDesc,
			String optionName, String newValue, boolean includeDefaultCommentedValue) {
		return makeOptionLinesWithOverrideValue(sectionDesc.getOptionsByName().get(optionName), 
				newValue, includeDefaultCommentedValue);
	}
	
	private List<String> makeOptionLinesWithOverrideValue(OptionDescription option,
			String newValue, boolean includeDefaultCommentedValue) {
		List<String> result = new ArrayList<>();
		
		result.add("#" + AUTO_EMPTY_LINE);
		for (String line : option.getComments()) {
			result.add("#" + AUTO_COMMENT_PREFIX + line);
		}
		if (includeDefaultCommentedValue) {
			result.add("#" + AUTO_PREFIX + " " + option.getName() +
					(option.getDefautValue().isEmpty() ? " =" : " = ") +
					option.getDefautValue());

		}
		if (newValue != null) {
			result.add(option.getName() + " = " + newValue);
		}
		
		return result;
	}
}

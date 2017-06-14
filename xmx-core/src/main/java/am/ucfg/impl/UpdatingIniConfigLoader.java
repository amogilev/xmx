package am.ucfg.impl;

import am.ucfg.*;
import am.xmx.util.Pair;
import org.ini4j.*;
import org.ini4j.Profile.Section;
import org.ini4j.spi.IniBuilder;
import org.ini4j.spi.IniFormatter;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static am.ucfg.impl.IniSettings.*;
import static java.util.Arrays.asList;
import static org.ini4j.EnhancedIniCommentsSupport.ENDSECT_ANCHOR;

/**
 * Implementation of the updating config reader based on .ini files
 * and auto-comments.
 * 
 * @author Andrey Mogilev
 */
public class UpdatingIniConfigLoader implements IUpdatingConfigLoader<Ini> {
	
	private final IConfigInfoProvider cfgInfoProvider;
	private final Map<String, SectionDescription> defaultSectionsByName;
	private final Map<String, SectionDescription> defaultHiddenSectionsByName;
	private final String lineSeparator;
	private final Logger logger;

	public UpdatingIniConfigLoader(IConfigInfoProvider cfgInfoProvider) {
		this.cfgInfoProvider = cfgInfoProvider;
		this.lineSeparator = cfgInfoProvider.getLineSeparator();
		
		this.defaultSectionsByName = mapSectionsByName(cfgInfoProvider.getAllDefaultSectionsDescriptions());
		this.defaultHiddenSectionsByName = mapSectionsByName(cfgInfoProvider.getAllDefaultHiddenSectionsDescriptions());
		this.logger = cfgInfoProvider.getLogger(UpdatingIniConfigLoader.class);
	}

	private static Map<String, SectionDescription> mapSectionsByName(List<SectionDescription> sections) {
		if (sections == null) {
			return Collections.emptyMap();
		}
		Map<String, SectionDescription> sectionsByName = new LinkedHashMap<>(sections.size());
		for (SectionDescription sd : sections) {
			sectionsByName.put(sd.getName(), sd);
		}
		return sectionsByName;
	}

	@Override
	public ConfigUpdateResult<Ini> loadAndUpdate(File iniFile, boolean rewriteAllowed) {
		Ini ini = makeIni();
		ini.setFile(iniFile);
		
		ConfigLoadStatus status;
		
		// make "EnhancedIni*" classes available by context class loader, as they are loaded using Reflection
		ClassLoader prevContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(UpdatingIniConfigLoader.class.getClassLoader());
		
		try {
			// create if missing
			boolean created = iniFile.createNewFile();
			boolean updated;

			try (FileInputStream in = new FileInputStream(iniFile)) {
				// avoid using Ini4J's load(File), to have more reliable streams closing
				ini.load(in);
				updated = updateConfig(ini);
			}
			
			if (created || (rewriteAllowed && updated)) {
				ini.store();
			}

			status = created ? ConfigLoadStatus.NEW : (updated ? ConfigLoadStatus.UPDATED : ConfigLoadStatus.SUCCESS);
			
		} catch (IOException e) {
			// report error but continue with default in-memory config
			logger.error("Failed to read configuration file", e);
			updateConfig(ini);
			status = ConfigLoadStatus.FAIL;
		} finally {
			Thread.currentThread().setContextClassLoader(prevContextClassLoader);
		}

		List<Pair<String, Map<String, String>>> sectionsWithOptionsByName = new ArrayList<>(ini.size());
		for (Section section : ini.values()) {
			String sectionName = section.getName();
			Map<String, String> optionsByName;

			// convert "default" options (which are just comments in Ini) to actual options, but do not write
			//   them back to file
			SectionDescription sectionDesc = defaultSectionsByName.get(sectionName);
			if (sectionDesc != null) {
				// this section may contain default options
				optionsByName = new LinkedHashMap<>(section);
				addMissingOptions(optionsByName, sectionDesc);
			} else {
				// no changes, use section itself
				optionsByName = section;
			}
			
			sectionsWithOptionsByName.add(Pair.of(sectionName, optionsByName));
		}
		// add hidden sections at the end to override everything else
		for (Entry<String, SectionDescription> sectionEntry : defaultHiddenSectionsByName.entrySet()) {
			Map<String, String> optionsByName = new HashMap<>();
			addMissingOptions(optionsByName, sectionEntry.getValue());
			sectionsWithOptionsByName.add(Pair.of(sectionEntry.getKey(), optionsByName));
		}
		
		return new ConfigUpdateResult<>(status, sectionsWithOptionsByName, ini);
	}
	
	/**
	 * Adds default option values from the section description to the current map of options, if that
	 * option is missing.
	 */
	private void addMissingOptions(Map<String, String> options, SectionDescription sectionDesc) {
		// keep all current options, add only missing 
		for (OptionDescription option : sectionDesc.getOptions()) {
			String name = option.getName();
			if (!options.containsKey(name)) {
				options.put(name, option.getDefautValue());
			}
		}
	}
	
	static {
		System.setProperty(IniBuilder.class.getName(), EnhancedIniBuilder.class.getName());
		System.setProperty(IniFormatter.class.getName(), EnhancedIniFormatter.class.getName());
	}
	
	private Ini makeIni() {
		Ini ini = new Ini();
		ini.setConfig(new EnhancedIniConfig(ini));
		ini.getConfig().setLineSeparator(lineSeparator);
		ini.getConfig().setEmptySection(true);
		
		return ini;
	}
	
	
	/**
	 * Updates and cleans the config file: removes non-existent or bad properties, fills or updates
	 * auto-comments (including ones with default values). 
	 */
	private boolean updateConfig(Ini ini) {
		boolean updated = false;
		
		// default file comment
		updated |= updateComment(ini, cfgInfoProvider.getFileComments());
		
		// move incorrectly placed section comments to endsect anchor of the previous sections 
		restoreEndsectAnchorComments(ini);
		
		Map<String, SectionDescription> defaultSectionsToProcessByName = new LinkedHashMap<>(defaultSectionsByName);
		
		// check/update known sections and clean other sections (clean = remove non-existent options)
		for (Section section : ini.values()) {
			String name = section.getName();
			
			if (defaultSectionsToProcessByName.containsKey(name)) {
				SectionDescription sectionDesc = defaultSectionsToProcessByName.remove(name);
				updated |= updateSection(ini, section, sectionDesc);
			} else {
				updated |= cleanSection(ini, section);
			}
		}
		
		// add missing sections
		for (SectionDescription sectionDesc : defaultSectionsToProcessByName.values()) {
			String sectionName = sectionDesc.getName();
			Section s = ini.add(sectionName);
			ini.putComment(sectionName, generateComment(sectionDesc.getSectionComments()));
			String autoComment = generateAutoComments(sectionDesc.getOptions());
			s.putComment(EnhancedIniCommentsSupport.ENDSECT_ANCHOR, autoComment);
			updated = true;
		}
		
		return updated;
	}

	private boolean updateComment(Profile p, String[] defaultCommentLines) {
		String newComment = mergeComments(p.getComment(), defaultCommentLines);
		if (newComment != p.getComment()) {
			p.setComment(newComment);
			return true;
		}
		return false;
	}

	private String mergeComments(String curComment, String[] defaultCommentLines) {
		String[] curCommentLines = curComment == null ? new String[0] : curComment.split("[\\r\\n]+");
		List<String> manualCommentLines = null; // lazy init
		int nextDefaultLine = 0;
		boolean requireUpdate = false;
		for (String curLine : curCommentLines) {
			if (curLine.startsWith(AUTO_COMMENT_PREFIX)) {
				// well-formatted auto-comment line
				curLine = curLine.substring(AUTO_COMMENT_PREFIX.length()).trim();
			} else if (curLine.equals(AUTO_EMPTY_LINE)) {
				// well-formatted auto-comment empty line
				curLine = "";
			} else if (curLine.startsWith(AUTO_PREFIX)) {
				// auto-line with bad format, rewrite
				requireUpdate = true;
			} else {
				// manual comment, skip (at current line if no update needed, or after defaults)
				if (manualCommentLines == null) {
					manualCommentLines = new ArrayList<>();
				}
				manualCommentLines.add(curLine);
				continue;
			}
			if (requireUpdate || 
					nextDefaultLine >= defaultCommentLines.length || 
					!defaultCommentLines[nextDefaultLine++].equals(curLine)) {
				requireUpdate = true;
			}
		}
		if (nextDefaultLine < defaultCommentLines.length) {
			requireUpdate = true;
		}
		
		if (requireUpdate) {
			return generateComment(asList(defaultCommentLines), manualCommentLines);
		} else {
			return curComment;
		}
	}

	private String generateComment(String[] autoComments) {
		return generateComment(asList(autoComments), null);
	}
	
	private String generateAutoComments(OptionDescription...optionDescriptions) {
		StringBuilder sb = new StringBuilder(128);
		for (OptionDescription optDesc : optionDescriptions) {
			sb.append(AUTO_EMPTY_LINE).append(lineSeparator); // separate options with empty comment lines
			for (String commentLine : optDesc.getComments()) {
				sb.append(AUTO_COMMENT_PREFIX).append(commentLine).append(lineSeparator);
			}
			sb.append(AUTO_PREFIX).append(' ').append(optDesc.getName())
					.append(optDesc.getDefautValue().isEmpty() ? " =" : " = ")
					.append(optDesc.getDefautValue()).append(lineSeparator);
		}
		return sb.toString();
	}

	private String generateComment(List<String> autoComments, List<String> manualComments) {
		StringBuilder sb = new StringBuilder();
		if (autoComments != null) {
			for (String line : autoComments) {
				if (line.isEmpty()) {
					sb.append(AUTO_EMPTY_LINE).append(lineSeparator);
				} else {
					sb.append(AUTO_COMMENT_PREFIX).append(line);
					if (!line.endsWith(lineSeparator)) {
						sb.append(lineSeparator);
					}
				}
			}
		}
		if (manualComments != null) {
			if (sb.length() > 0) {
				sb.append(lineSeparator);
			}
			for (String line : manualComments) {
				sb.append(line);
				if (!line.endsWith(lineSeparator)) {
					sb.append(lineSeparator);
				}
			}
		}
		
		return sb.toString();
	}
	
	// a comment line or option which may belong to a section
	private static class SectionLineInfo {
		enum Kind {
			/** Actual (non-commented) option */
			OPTION,
			/** Default option found in auto-comment */
			DEFOPTION,
			/** Auto-comment line , which does not contain option */
			AUTOCOMMENT,
			/** Manual (non-auto) comment */
			USERCOMMENT
		};
		
		Kind kind;
		
		SectionLineInfo(Kind kind) {
			this.kind = kind;
		}

		@Override
		public String toString() {
			return "" + kind + ": ";
		}
	}
	
	private static interface HasOption {
		String getOptionName();
		String getOptionValue();
	}
	
	// base class for all comment lines - DEFOPTION, AUTOCOMMENT and USERCOMMENT
	private static class CommentLine extends SectionLineInfo {
		String commentLine;
		
		CommentLine(Kind kind, String commentLine) {
			super(kind);
			this.commentLine = commentLine;
		}

		@Override
		public String toString() {
			return super.toString() + commentLine;
		}
	}
	
	private static class DefOptionLine extends CommentLine implements HasOption {
		
		String optionName;
		String optionValue;
		
		DefOptionLine(String commentLine, String optionName, String optionValue) {
			super(Kind.DEFOPTION, commentLine);
			this.optionName = optionName;
			this.optionValue = optionValue;
		}
		
		public String getOptionName() {
			return optionName;
		}

		public String getOptionValue() {
			return optionValue;
		}
	}
	
	private static class OptionLine extends SectionLineInfo implements HasOption {
		String optionName;
		String optionValue;

		private OptionLine(String optionName, String optionValue) {
			super(Kind.OPTION);
			this.optionName = optionName;
			this.optionValue = optionValue;
		}
		
		public String getOptionName() {
			return optionName;
		}
		
		public String getOptionValue() {
			return optionValue;
		}
		
		@Override
		public String toString() {
			return super.toString() + optionName;
		}
	}
	
	/**
	 * Checks all section comments and moves incorrectly placed auto-comments back to
	 * ENDSECT_ANCHOR of the previous sections.
	 */
	private void restoreEndsectAnchorComments(Ini ini) {
		// iterate all sections and check if a section comments shall be moved to ENDSECT_ANCHOR of the 
		//  previous section. The checked conditions are: the previous section shall have no comments at 
		//  ENDSECT_ANCHOR yet, and shall have default options for which auto-comment lines (DEFOPTIONs) 
		//  present in the current section's comment
		Section prevSection = null;
		for (Section section : ini.values()) {
			if (prevSection != null && prevSection.getComment(ENDSECT_ANCHOR) == null) {
				String sectionComment = ini.getComment(section.getName());
				if (sectionComment != null &&
						defaultSectionsByName.containsKey(prevSection.getName()) &&
						sectionComment.contains(AUTO_PREFIX + " ")) {
					
					List<SectionLineInfo> commentLines = new ArrayList<>();
					Set<String> defOptionNames = defaultSectionsByName.get(prevSection.getName()).getOptionsByName().keySet();
					parseCommentLines(commentLines, sectionComment, defOptionNames);
					
					if (containsLineKind(commentLines, SectionLineInfo.Kind.DEFOPTION)) {
						// found DEFOPTION, so move the section comment
						prevSection.putComment(ENDSECT_ANCHOR, sectionComment);
						ini.removeComment(section.getName());
					}
				}
			}
			prevSection = section;
		}
	}
	
	/**
	 * Checks and, if necessary, updates the section according to its description.
	 * Updates the section comment and auto-comments for the default options,
	 * removes unsupported options.
	 * 
	 * @param ini the ini file
	 * @param s the section to check
	 * @param sectionDesc the section description
	 * 
	 * @return whether the update was required
	 */
	private boolean updateSection(Ini ini, Section s, SectionDescription sectionDesc) {
		boolean updated = false;
		
		// ensure section comment
		String prevComment = ini.getComment(sectionDesc.getName());
		String newComment = mergeComments(prevComment, sectionDesc.getSectionComments());
		if (newComment != prevComment) {
			ini.putComment(sectionDesc.getName(), newComment);
			updated = true;
		}
		
		// prepare current section's lines (options and different kinds of comments) for matching
		// with default comments/values
		List<SectionLineInfo> sectionLines = parseSectionLines(s, sectionDesc);
		
		// match auto-comments in the actual section contents with the expected defaults, 
		//  update if necessary.
		// The ALGORITHM:
		// - iterate OPTION and DEFOPTION section lines. Remove unknown options.
		// - for DEFOPTION, match auto-comments in previous lines. For OPTION, there shall be already matched DEFOPTION above
		// - if not matched, regenerate auto-comments, keep manual comments, add to list of pending comments
		// - if matched DEFOPTION, add it pending comments
		// - on known OPTION processing, add previously pending comments to it 
		
		int lastProcessedOptionLine = -1;
		Set<String> foundOptions = new HashSet<>(); // found options and defoptions
		Set<String> foundDefOptions = new HashSet<>(); // found defoptions only (subset of previous)
		List<String> pendingApprovedComments = new ArrayList<>();
		
		for (int i = 0; i < sectionLines.size(); i++) {
			SectionLineInfo line = sectionLines.get(i);
			if (line instanceof HasOption) {
				String optionName = ((HasOption)line).getOptionName();
				OptionDescription optionDesc = sectionDesc.getOptionsByName().get(optionName);
				if (optionDesc == null) {
					if (line instanceof OptionLine && !cfgInfoProvider.isSupportedOption(sectionDesc.getName(), optionName)) {
						logger.warn("Configuration: removing unsupported option {}", optionName);
						s.remove(optionName);
					}
					
					// keep only user comments, clean all auto-comments found so far
					generateAndAddCommentFromLinesTo(pendingApprovedComments, sectionLines, 
							lastProcessedOptionLine + 1, i - 1, SectionLineInfo.Kind.USERCOMMENT);
					updated = true;
					lastProcessedOptionLine = i;
					continue;
				}
				foundOptions.add(optionName);
				
				// whether automatic comments for this option or defoption matches current defaults,
				// and so no update required
				boolean autoCommentsMatch;
				
				boolean skipDefault = false;
				
				if (line.kind == SectionLineInfo.Kind.OPTION) {
					// matches only if defoption already met above, and there is no extra auto-comments
					autoCommentsMatch = foundDefOptions.contains(optionName) &&
						!containsLineKind(sectionLines.subList(lastProcessedOptionLine + 1, i), SectionLineInfo.Kind.AUTOCOMMENT);
					if (!autoCommentsMatch) {
						// we will add defoption on update, so consider it "found"
						foundDefOptions.add(optionName);
					}
				} else {
					assert line.kind == SectionLineInfo.Kind.DEFOPTION;
					
					if (foundDefOptions.contains(optionName)) {
						// duplicate defOption found, or we already have generated it
						autoCommentsMatch = false;
						skipDefault = true;
					} else {
						foundDefOptions.add(optionName);
						DefOptionLine optLine = (DefOptionLine) line;
						// match itself and above auto-comments
						autoCommentsMatch = optionDesc.getDefautValue().equals(optLine.optionValue)
								&& matchAutoComments(optionDesc.getComments(), sectionLines, lastProcessedOptionLine + 1, i - 1);
					}
				}

				if (!autoCommentsMatch) {
					// keep only user comments, re-generate auto-comments
					generateAndAddCommentFromLinesTo(pendingApprovedComments, sectionLines, lastProcessedOptionLine + 1, i - 1, 
							SectionLineInfo.Kind.USERCOMMENT);
					if (!skipDefault) {
						pendingApprovedComments.add(generateAutoComments(optionDesc));
					}
					updated = true;
				} else {
					// ok, move all comments to pending (required in case of future updates)
					generateAndAddCommentFromLinesTo(pendingApprovedComments, sectionLines, lastProcessedOptionLine + 1, i,
							SectionLineInfo.Kind.AUTOCOMMENT, SectionLineInfo.Kind.DEFOPTION, SectionLineInfo.Kind.USERCOMMENT);
				}
				
				// if in update mode, apply pending comments for the option
				if (line.kind == SectionLineInfo.Kind.OPTION && !pendingApprovedComments.isEmpty()) {
					if (updated) {
						s.putComment(optionName, generateComment(null, pendingApprovedComments));
					}
					pendingApprovedComments.clear();
				}
				lastProcessedOptionLine = i;
			}
		}
		
		// generate missing defoptions
		for (OptionDescription optionDesc : sectionDesc.getOptions()) {
			if (!foundOptions.contains(optionDesc.getName())) {
				pendingApprovedComments.add(generateAutoComments(optionDesc));
				updated = true;
			}
		}
		
		boolean hasExtraAutoComments = containsLineKind(sectionLines.subList(lastProcessedOptionLine + 1, sectionLines.size()), 
				SectionLineInfo.Kind.AUTOCOMMENT);
		if (hasExtraAutoComments) {
			// found unmatched auto-comments
			updated = true;
		}
		
		// merge pending approved comments (if any) with current manual ENDSECT comments
		if (updated) {
			generateAndAddCommentFromLinesTo(pendingApprovedComments, sectionLines, lastProcessedOptionLine + 1, sectionLines.size() - 1,
					SectionLineInfo.Kind.USERCOMMENT);
			s.putComment(ENDSECT_ANCHOR, generateComment(null, pendingApprovedComments));
		}
		
		return updated;
	}

	/**
	 * Removes all "bad" options (i.e. unknown or inappropriate for section level) from
	 * the section. Also validates the section name, and removes the section if it is unrecognizable.
	 * 
	 * @return whether the config was updated, e.g. the section or some property was removed
	 */
	private boolean cleanSection(Ini ini, Section section) {
		String sectionName = section.getName();
		
		// process each option, remove unknown and non-corresponding to the level
		boolean updated = false;
		
		Iterator<Entry<String, String>> entriesIt = section.entrySet().iterator();
		while (entriesIt.hasNext()) {
			Entry<String, String> entry = entriesIt.next();
			String optionName = entry.getKey();
			if (!cfgInfoProvider.isSupportedOption(sectionName, optionName)) {
				logger.warn("Configuration: removing non-existing option {}", optionName);
				section.remove(optionName);
				updated = true;
			}
		}
		
		if (section.isEmpty()) {
			// remove empty user sections (as this is not one of default sections!)
			logger.warn("Configuration: removing empty section '{}'", sectionName);
			ini.remove(section);
			updated = true;
		}
		
		return updated;
	}

	
	/**
	 * Prepares the list of SectionLineInfo from all options and comments found inside the section.
	 */
	private static List<SectionLineInfo> parseSectionLines(Section section, SectionDescription sectionDesc) {
		Set<String> defOptionNames = sectionDesc.getOptionsByName().keySet();
		
		List<SectionLineInfo> result = new ArrayList<>();
		for (Entry<String, String> option : section.entrySet()) {
			String name = option.getKey();
			parseCommentLines(result, section.getComment(name), defOptionNames);
			result.add(new OptionLine(name, option.getValue()));
		}
		parseCommentLines(result, section.getComment(ENDSECT_ANCHOR), defOptionNames);
		return result;
	}

	/**
	 * Splits the specified comments into lines, parses and determines kind of each line, and
	 * add them to the result list.
	 */
	private static void parseCommentLines(List<SectionLineInfo> result,
			String comments, Set<String> defOptionNames) {
		
		if (comments == null) {
			return;
		}
		
		BufferedReader br = new BufferedReader(new StringReader(comments), Math.max(1024,  comments.length()));
		String line;
		try {
			while ((line = br.readLine()) != null) {
				if (line.startsWith(AUTO_PREFIX)) {
					if (line.startsWith(AUTO_COMMENT_PREFIX)) {
						line = line.substring(AUTO_COMMENT_PREFIX.length());
						result.add(new CommentLine(SectionLineInfo.Kind.AUTOCOMMENT, line));
						continue;
					}
					// may be DEFOPTION or empty AUTOCOMMENT or incorrectly formatted AUTOCOMMENT
					line = line.substring(AUTO_PREFIX.length());
					// commented options usually have space after '!' 
					if (line.startsWith(" ")) {
						line = line.substring(1);
					}
					int n = line.indexOf('=');
					if (n > 0) {
						String opt = line.substring(0, n).trim();
						if (defOptionNames.contains(opt)) {
							String optValue = line.substring(n + 1).trim();
							result.add(new DefOptionLine(line, opt, optValue));
							continue;
						}
					}
					if (line.length() > 0 && line.charAt(0) == '#') {
						line = line.substring(1).trim();
					}
					result.add(new CommentLine(SectionLineInfo.Kind.AUTOCOMMENT, line));
				} else {
					result.add(new CommentLine(SectionLineInfo.Kind.USERCOMMENT, line));
				}
			}
		} catch (IOException e) {
			// unexpected
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks and returns whether the specified list contains a line of the specified kind.
	 */
	private static boolean containsLineKind(List<SectionLineInfo> sectionLines, SectionLineInfo.Kind kind) {
		
		for (int i = 0; i < sectionLines.size(); i++) {
			SectionLineInfo line = sectionLines.get(i);
			if (kind == line.kind) {
				return true; 
			}
		}
		return false;
	}

	private void generateAndAddCommentFromLinesTo(List<String> comments,
			List<SectionLineInfo> sectionLines, int from, int to,
			SectionLineInfo.Kind firstKind, SectionLineInfo.Kind...otherKinds) {
		
		String generatedComment = generateCommentFromLines(sectionLines, from, to, firstKind, otherKinds);
		if (!generatedComment.isEmpty()) {
			comments.add(generatedComment);
		}
	}
	
	private String generateCommentFromLines(
			List<SectionLineInfo> sectionLines, int from, int to,
			SectionLineInfo.Kind firstKind, SectionLineInfo.Kind...otherKinds) {
		
		StringBuilder sb = new StringBuilder();
		EnumSet<SectionLineInfo.Kind> kindsSet = EnumSet.of(firstKind, otherKinds);
		for (int i = from; i <= to; i++) {
			SectionLineInfo line = sectionLines.get(i);
			if (kindsSet.contains(line.kind) && line instanceof CommentLine) {
				CommentLine cl = (CommentLine) line;
				if (cl.commentLine.isEmpty()) {
					sb.append(AUTO_EMPTY_LINE);
				} else if (cl.kind == SectionLineInfo.Kind.AUTOCOMMENT) {
					sb.append(AUTO_COMMENT_PREFIX);
				} else if (cl.kind == SectionLineInfo.Kind.DEFOPTION) {
					sb.append(AUTO_PREFIX).append(' ');
				}
				sb.append(cl.commentLine).append(lineSeparator);
			}
		}
		return sb.toString();
	}
	
	private static boolean matchAutoComments(String[] expectedComments,
			List<SectionLineInfo> sectionLines, int from, int to) {
		
		int nMatchedExpectedComments = 0; 
		for (int i = from; i <= to; i++) {
			SectionLineInfo line = sectionLines.get(i);
			if (line.kind != SectionLineInfo.Kind.AUTOCOMMENT) {
				// only match non-empty auto-comments
				continue;
			}
			CommentLine commentLine = (CommentLine) line;
			if (commentLine.commentLine.isEmpty()) {
				// only match non-empty auto-comments
				continue;
			}
			if (nMatchedExpectedComments >= expectedComments.length) {
				// extra auto-comment
				// logDebug("Negative match - extra comment line: i=" + i + "; line=" + commentLine.commentLine);
				return false;
			}
			String expected = expectedComments[nMatchedExpectedComments++];
			if (!expected.isEmpty() && !expected.equals(commentLine.commentLine)) {
				// contents do not match
				// logDebug("Negative match - different contents; line: i=" + i + "; actual=" + 
				//		commentLine.commentLine + "; expected=" + expected);
				return false;
			}
		}
		
		for (int i = nMatchedExpectedComments; i < expectedComments.length; i++) {
			String expected = expectedComments[i];
			if (!expected.isEmpty()) {
				// some non-empty expected comments missing
				// logDebug("Negative match - missing expected lines starting from " + nMatchedExpectedComments + 
				//		"; expected=" + expectedComments[nMatchedExpectedComments]);
				return false;
				
			}
		}
		
		return true;
	}
//	private static void logDebug(String message) {
//		System.out.println(message);
//	}

}

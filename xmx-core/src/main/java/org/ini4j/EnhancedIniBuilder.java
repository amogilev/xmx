package org.ini4j;

import java.util.ArrayList;
import java.util.List;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.ini4j.spi.IniHandler;

/**
 * Replacement of the standard Ini4J's IniBuilder for better
 * handling of sparse comments (e.g. several same-level comments split 
 * by empty lines)
 *  
 * @author Andrey Mogilev
 */
public class EnhancedIniBuilder implements IniHandler {
	
    private Ini ini;
	private boolean isHeader;
	private Section curSection, prevSection;
	private List<String> pendingComments = new ArrayList<>();
	
	public EnhancedIniBuilder(Ini ini) {
		this.ini = ini;
		
	}
	@Override
	public void startIni() {
		boolean isHeaderCommentAllowed = ini.getConfig().isHeaderComment();
		// if header comment not allowed, all starting comments will go to the first section
        isHeader = isHeaderCommentAllowed;
	}
	
	@Override
	public void handleComment(String comment) {
		if (ini.getConfig().isComment()) {
			pendingComments.add(comment);
		}
	}

	@Override
	public void startSection(String sectionName) {
        if (ini.getConfig().isMultiSection()) {
            curSection = ini.add(sectionName);
        } else {
            Ini.Section s = ini.get(sectionName);
            curSection = (s == null) ? ini.add(sectionName) : s;
        }

        if (!pendingComments.isEmpty()) {
        	String sectionComment = null;
            if (isHeader) {
            	// first comment goes to header, others to section
            	String headerComment = pendingComments.get(0);
            	ini.setComment(headerComment);
            	if (pendingComments.size() > 0) {
            		sectionComment = joinComments(pendingComments.subList(1, pendingComments.size()));
            	}
            } else if (prevSection != null) {
            	// all-but-last comments (if any) goes to END of the previous section, the 
            	//  last comment goes to the section being started
            	sectionComment = pendingComments.get(pendingComments.size() - 1);
            	if (pendingComments.size() > 1) {
            		String endPrevSectionComment = joinComments(pendingComments.subList(0, pendingComments.size() - 1));
            		prevSection.putComment(EnhancedIniCommentsSupport.ENDSECT_ANCHOR, endPrevSectionComment);
            	}
            } else {
            	sectionComment = joinComments(pendingComments);
            }
            if (sectionComment != null) {
            	ini.putComment(sectionName, sectionComment);
            }

            pendingComments.clear();
        }

        isHeader = false;
	}
	
	@Override
	public void endIni() {
		if (!pendingComments.isEmpty()) {
			if (isHeader) {
	        	ini.setComment(joinComments(pendingComments));
			} else if (prevSection != null) {
        		String endPrevSectionComment = joinComments(pendingComments);
        		prevSection.putComment(EnhancedIniCommentsSupport.ENDSECT_ANCHOR, endPrevSectionComment);
			}
			pendingComments.clear();
		}
	}

	private String joinComments(List<String> comments) {
		if (comments.size() == 1) {
			return comments.get(0);
		}
		StringBuilder sb = new StringBuilder(256);
		boolean first = true;
		for (String comment : comments) {
			if (first) {
				first = false;
			} else {
				// use single empty line to separate comments, as there is
				// no way to get the original separators
				sb.append(ini.getConfig().getLineSeparator());
			}
			sb.append(comment);
		}
		return sb.toString();
	}
	
	@Override
	public void endSection() {
		prevSection = curSection;
		curSection = null;
	}

	@Override
	public void handleOption(String optionName, String optionValue) {
        if (ini.getConfig().isMultiOption()) {
            curSection.add(optionName, optionValue);
        } else {
            curSection.put(optionName, optionValue);
        }

        if (!pendingComments.isEmpty()) {
        	curSection.putComment(optionName, joinComments(pendingComments));
        	pendingComments.clear();
        }
	}
}
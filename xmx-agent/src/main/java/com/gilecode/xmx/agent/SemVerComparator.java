// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.agent;

import java.util.Comparator;

/**
 * A simplified comparator which follows the precedence rules of "Semantic Versioning 2.0" but not
 * enforces the validity of version strings. As an additional rule the "empty" version is the minimal one.
 * <br/>
 * NOTE: currently does not support build metadata (should be "1.0.0+20130313144700" EQ "1.0.0-beta+exp.sha.5114f85")
 */
public class SemVerComparator implements Comparator<String> {

	private static class SemVerBaseParts {

		// base parts - should be 3 integers (major.minor.patch), but support any number of alphanums
		String[] baseParts; // expect 3 numbers, but support

		// the trailing part after the first hyphen, e.g. '-alpha.1'
		String preReleaseIds;

		public SemVerBaseParts(String[] baseParts, String preReleaseIds) {
			this.baseParts = baseParts;
			this.preReleaseIds = preReleaseIds;
		}
	}

	@Override
	public int compare(String v1, String v2) {
		SemVerBaseParts p1 = parseBaseParts(v1), p2 = parseBaseParts(v2);
		int cmp = comparePartsArrays(p1.baseParts, p2.baseParts);
		return cmp != 0 ? cmp : comparePreReleaseIds(p1.preReleaseIds, p2.preReleaseIds);
	}

	private int comparePartsArrays(String[] parts1, String[] parts2) {
		int minLen = Math.min(parts1.length, parts2.length);
		for (int i = 0; i < minLen; i++) {
			int cmp = compareParts(parts1[i], parts2[i]);
			if (cmp != 0) {
				return cmp;
			}
		}
		if (parts1.length == parts2.length) {
			return 0;
		} else {
			// "1.0" < "1.0.anything"
			return parts1.length < parts2.length ? -1 : 1;
		}
	}

	private int compareParts(String p1, String p2) {
		if (p1.equals(p2)) {
			return 0;
		} else if (p1.isEmpty() || p2.isEmpty()) {
			// "" < "anything"
			return p1.isEmpty() ? -1 : 1;
		}
		Integer num1 = safeParseInt(p1);
		Integer num2 = safeParseInt(p2);
		boolean isNum1 = num1 != null;
		boolean isNum2 = num2 != null;

		if (isNum1 != isNum2) {
			// numeric ids have lower precedence, i.e. "1" < "ABC"
			return isNum1 ? -1 : 1;
		} else if (isNum1) {
			// compare as numbers
			return num1.compareTo(num2);
		} else {
			// compare lexicographically
			return p1.compareTo(p2);
		}
	}

	private Integer safeParseInt(String str) {
		// fast path: do not parse if first char is not digit
		if (str.isEmpty() || !Character.isDigit(str.charAt(0))) {
			return null;
		}

		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private int comparePreReleaseIds(String trail1, String trail2) {
		if (trail1.equals(trail2)) {
			return 0;
		} else if (trail1.isEmpty() || trail2.isEmpty()) {
			// pre-release versions have lowere precedence, e.g. "1.0.0.alpha" < "1.0.0"
			return trail1.isEmpty() ? 1 : -1;
		}
		// split to parts and compare
		String[] parts1 = trail1.split("\\.");
		String[] parts2 = trail2.split("\\.");

		// same comparison rules work
		return comparePartsArrays(parts1, parts2);
	}


	private SemVerBaseParts parseBaseParts(String ver) {
		String base, preReleaseIds;
		int n = ver.indexOf('-');
		if (n < 0) {
			base = ver;
			preReleaseIds = "";
		} else {
			base = ver.substring(0, n);
			preReleaseIds = ver.substring(n + 1);
		}
		String[] baseParts = base.split("\\.");
		return new SemVerBaseParts(baseParts, preReleaseIds);
	}
}

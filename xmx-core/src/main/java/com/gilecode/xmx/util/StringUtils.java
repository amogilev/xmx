// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.util;

public class StringUtils {

    /**
     * Quotes the string with single quotes, and escapes all inner quotes with the same quote character,
     * e.g. transforms " 'a'" to "' ''a'''".
     */
    public static String quote(String str) {
        return "'" + str.replace("'", "''") + "'";
    }

    /**
     * Extracts quoted substring started at the specified position, returns the unqouted substring and
     * the tail of the original string. The head of the original string (before position) is skipped.
     *
     * @param str the string to process
     * @param n the start of the quoted substring, the character at that position must be quote
     *
     * @return the array of two strings: unquoted substring and the tail of the string
     *
     * @throws IllegalArgumentException if the specified position is not of the quote character, or
     *   the substring is not closed with the quote
     */
    public static String[] extractQuoted(String str, int n) throws IllegalArgumentException {
        if (str.length() <= n || str.charAt(n) != '\'') {
            throw new IllegalArgumentException("Wrong position for quoted substring: pos=" + n + ", str=" + str);
        }

        String[] parts = new String[2];

        StringBuilder sb = new StringBuilder();

        int cur = n; // position of the current quote
        while (true) {
            int next = str.indexOf('\'', cur + 1); // position of the next quote
            if (next >= 0) {
                // found a quote - it may be either escaping of next quote, or closing quote
                boolean isEscape = next + 1 < str.length() && str.charAt(next + 1) == '\'';
                if (isEscape) {
                    sb.append(str, cur + 1, next + 1);
                    cur = next + 1;
                } else {
                    // found last quote
                    sb.append(str, cur + 1, next);
                    parts[0] = sb.toString();
                    parts[1] = str.substring(next + 1);
                    return parts;
                }
            } else {
                throw new IllegalArgumentException("Missing closing quote character: start=" + n + ", str=" + str);
            }
        }
    }
}

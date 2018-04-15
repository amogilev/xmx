// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern;

import com.gilecode.xmx.cfg.impl.XmxIniParseException;
import com.gilecode.xmx.cfg.pattern.impl.MethodPatternParser;

import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parser and utility methods for entity masks (patterns) supported for XMX configuration.
 * Such patterns may be used in the section names (like [App=<i>AppMask</i>,Class=<i>ClassMask</i>)
 * and values of some properties.
 * <p/>  
 * The following cases are recognized and supported:
 * <ul>
 * <li>Simple string values, like in [App=MyApp]. Only letters or digits are allowed for simple values.
 * <li>Quoted string values, used when special characters or whitespaces are used, like in [App="My App"].
 * <li>Java patterns, which must start with ^ and end with $, like in [App=^(my|our)app\d*$]
 * <li>Simple patterns (masks), which are |-separated lists of simple names with optional * symbol 
 * designating any sequence of characters, e.g. [App=*, Class=NewService|NewServiceImpl]
 * </ul>
 * 
 * @author Andrey Mogilev
 */
public class PatternsSupport {
	
//	private static final Pattern SIMPLE_PATTERN_OR_LITERAL = Pattern.compile("^[\\w\\*\\|]*$");
	private static final Pattern SIMPLE_PATTERN_OR_LITERAL = Pattern.compile("^[*|. \\p{javaJavaIdentifierPart}]*$");
	
	public static Pattern parse(String patternValue) throws XmxIniParseException {
		patternValue = patternValue.trim();
		
		try {
			if (patternValue.startsWith("^") && patternValue.endsWith("$")) {
				// Java pattern
				return Pattern.compile(patternValue);
			} else if (patternValue.startsWith("\"") && patternValue.endsWith("\"") && patternValue.length() > 1) {
				// quoted literal
				patternValue = unquote(patternValue);
				return Pattern.compile("^\\Q" + patternValue + "\\E$");
			} else if (SIMPLE_PATTERN_OR_LITERAL.matcher(patternValue).matches()) {
				// simple pattern or simple literal
				patternValue = patternValue.replace(".", "\\.").replace("$", "\\$").replace("*", ".*");
				return Pattern.compile("^(?:" + patternValue + ")$");
			}
		} catch (PatternSyntaxException e) {
			throw new XmxIniParseException("Unrecognized pattern type: " + patternValue, e);
		}
		
		throw new XmxIniParseException("Unrecognized pattern type: " + patternValue);
	}
	
	public static String unquote(String patternValue) {
		if (patternValue.startsWith("\"") && patternValue.endsWith("\"") && patternValue.length() > 1) {
			String str = patternValue.substring(1, patternValue.length() - 1);
			return str.replace("\"\"", "\"");
		} else {
			return patternValue;
		}
	}

	public static boolean matches(String patternValue, String name) {
		return parse(patternValue).matcher(name).matches();
	}

	public static IMethodMatcher parseMethodPattern(String patternValue) throws XmxIniParseException {
		String[] patterns = patternValue.split("\\|");
		if (patterns.length == 1) {
			return new MethodPatternParser(patterns[0]).getMatcher();
		}
		final IMethodMatcher[] orMatchers = new IMethodMatcher[patterns.length];
		for (int i = 0; i < patterns.length; i++) {
			orMatchers[i] = new MethodPatternParser(patterns[i]).getMatcher();
		}
		return new IMethodMatcher() {
			@Override
			public boolean matches(Method m) {
				for (IMethodMatcher orMatcher : orMatchers) {
					if (orMatcher.matches(m)) {
						return true;
					}
				}
				return false;
			}
		};
	}
}

// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.impl.XmxIniParseException;
import com.gilecode.xmx.cfg.pattern.IMethodMatcher;
import com.gilecode.xmx.cfg.pattern.ITypeMatcher;
import com.gilecode.xmx.cfg.pattern.PatternsSupport;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A parser for method patterns. After a successful parsing, returns {@link IMethodMatcher}.
 * <p/>
 * The parser implementation is lenient, so it shall accept not only "strict" patterns described in doc, but also
 * other convenient forms, like a raw copy of Java method declaration (i.e. skips generic type parameters, parameter
 * names etc.
 */
public class MethodPatternParser {

    private static Map<String, ModifierKeyword> modifiersByNameUC = new HashMap<>();
    private static Map<String, VisibilityKeyword> visibiltyByNameUC = new HashMap<>();

    private final IMethodMatcher matcher;
    private final ParseContext ctx;
    private final List<ParseToken> tokens;

    public MethodPatternParser(String pattern) {
        pattern = pattern.trim();

        if (pattern.equals("*")) {
            // fast path
            ctx = null;
            tokens = null;
            matcher = IMethodMatcher.ANY_METHOD;
        } else {
            ctx = new ParseContext(pattern);
            tokens = new MethodPatternTokenizer(ctx).tokenize();
            if (tokens.isEmpty()) {
                throw new XmxIniParseException("Empty method pattern. Use '*' for ALL!");
            }

            matcher = parse();
        }
    }

    public IMethodMatcher getMatcher() {
        return matcher;
    }

    static class MethodPatternTokenizer {

        private final ParseContext ctx;

        public MethodPatternTokenizer(ParseContext ctx) {
            this.ctx = ctx;
        }

        /**
         * Parses and returns the next token of a 'method pattern' grammar after the specified 'from' position.
         * <p/>
         * A token is a valid Java identifier or type (primitive or simple class name or fully-qualified class name,
         * but templates not allowed) with zero or more optional '*' characters allowed, or single other non-whitespace
         * character (like comma, parenthesis etc.).
         *
         * If there are no more tokens in the source char array, returns {@code null}
         */
        private ParseToken nextSimpleToken(int from) {
            char[] ca = ctx.ca;
            int idxTokenStart = -1;
            int nDots = 0;
            for (int i = from; i < ca.length; i++) {
                char c = ca[i];

                if (idxTokenStart < 0) {
                    if (!Character.isWhitespace(c)) {
                        idxTokenStart = i;
                        if (c != '*' && c != '.' && !Character.isJavaIdentifierStart(c)) {
                            // single-char token
                            return new ParseToken(ctx.original.substring(i, i+1), i);
                        }
                    } // if ws, skip it
                } else {
                    boolean end = false;
                    if (c == '.') {
                        if (++nDots == 3) {
                            i++; // consume dot
                            end = true;
                        }
                    } else {
                        if (nDots == 2) {
                            // one "extra" dot is not consumed, left for next token
                            end = true;
                        }
                        nDots = 0;
                        if (!Character.isJavaIdentifierPart(c) && c != '*') {
                            end = true;
                        }
                    }
                    if (end) {
                        return new ParseToken(ctx.original.substring(idxTokenStart, i), idxTokenStart);
                    }
                }
            }
            return idxTokenStart < 0 ? null : new ParseToken(ctx.original.substring(idxTokenStart), idxTokenStart);
        }

        public List<ParseToken> tokenize() {
            List<ParseToken> result = new ArrayList<>();
            ParseToken pt;
            int pos = 0;
            while ((pt = nextSimpleToken(pos)) != null) {
                result.add(pt);
                pos = pt.pos + pt.token.length();
            }
            return result;
        }
    }

    private IMethodMatcher parse() throws XmxIniParseException {
        MethodMatchSpec mms = new MethodMatchSpec();

        // 1: find parenthesis, extract type list
        int idxParenethesis = indexOfToken("(", 0);
        if (idxParenethesis == 0) {
            throwUnexpectedTokenParseException(tokens.get(0), "Expected method name but got");
        } else if (idxParenethesis > 0) {
            ParseToken lastToken = tokens.get(tokens.size() - 1);
            if (!lastToken.token.equals(")")) {
                throwUnexpectedTokenParseException(lastToken, "Expected ')' as last token but got");
            }
            List<TypePatternSpec> types = new ArrayList<>();
            ParamsParseState ps = ParamsParseState.START;
            boolean hasEllipsis = false;
            for (int i = idxParenethesis + 1; i < tokens.size() - 1; i++) {
                ParseToken pt = tokens.get(i);
                String token = pt.token;
                switch (ps) {
                case START:
                    if (token.equals("...")) {
                        hasEllipsis = true;
                        ps = ParamsParseState.AFTER_ELLIPSIS;
                    } else {
                        // expect type
                        TypePatternSpec typeSpec = parseTypeToken(pt);
                        int nextIdx = parseTypeSuffixes(typeSpec, i + 1, tokens.size() - 1);
                        i = nextIdx - 1; // as it will be incremented at next loop iteration
                        types.add(typeSpec);
                        ps = ParamsParseState.AFTER_TYPE;
                    }
                    break;
                case AFTER_NAME:
                case AFTER_TYPE:
                    if (token.equals(",")) {
                        ps = ParamsParseState.START;
                    } else if (ps == ParamsParseState.AFTER_TYPE && isValidIdentifier(pt)) {
                        // allow skipping one identifier
                        ps = ParamsParseState.AFTER_NAME;
                    } else {
                        throwUnexpectedTokenParseException(pt, "Unexpected token after parameter type");
                    }
                    break;
                case AFTER_ELLIPSIS:
                    throwUnexpectedTokenParseException(pt, "Expected ')' but got");
                }
            }
            mms.parameters = new MethodParamsSpec(types, hasEllipsis);
        }

        // the last token before args should be the method name pattern
        int idxName = idxParenethesis > 0 ? idxParenethesis - 1 : tokens.size() - 1;
        mms.namePattern = parseMethodNamePattern(tokens.get(idxName));

        // other tokens may be (part of) modifiers or, for the last one, the type
        int nextIdx = parseModifiers(mms, idxName);

        boolean expectType = nextIdx < idxName;

        if (expectType) {
            mms.returnTypeSpec = parseTypeToken(tokens.get(nextIdx));
            nextIdx = parseTypeSuffixes(mms.returnTypeSpec, nextIdx + 1, idxName);
            if (nextIdx < idxName) {
                throwUnexpectedTokenParseException(tokens.get(nextIdx), "Unexpected token after return type");
            }
        }

        ModifierFlags flags = new ModifierFlags();
        if (mms.visibilitySpec != null) {
            mms.visibilitySpec.writeModifierFlags(flags);
        }
        for (ModifierSpec ms : mms.modifierSpecs) {
            ms.writeModifierFlags(flags);
        }

        return new MethodPatternMatcher(flags, mms.namePattern,
                mms.getReturnTypeMatcher(), mms.getParametersMatcher());
    }

    /**
     * Parses optional type suffixes (like array specifiers or type parameters) and add them to the type specifier.
     * For simplicity, supports both [] and ... as array specifiers, so can be used both for return types and parameter
     * types.
     * @param typeSpec the type specifier generated from the parsed type token
     * @param from start index of optional type suffix tokens
     * @param endIdx the end index (excluding)
     *
     * @return the index to continue parsing, greater or equal than 'from'
     */
    private int parseTypeSuffixes(TypePatternSpec typeSpec, int from, int endIdx) {
        TypeParseState ps = TypeParseState.AFTER_TYPE;
        int typeParamsLevel = 0;
        int i = from;
        for (; i < endIdx; i++) {
            ParseToken pt = tokens.get(i);
            String token = pt.token;
            switch (ps) {
            case AFTER_TYPE:
                switch (token) {
                case "...":
                    typeSpec.arrayLevel++;
                    return i + 1; // final token
                case "[":
                    ps = TypeParseState.ARRAY_OPENED;
                    break;
                case "<":
                    ps = TypeParseState.IN_TYPE_PARAMS;
                    typeParamsLevel++;
                    break;
                default:
                    // not a type suffix, return
                    return i;
                }
                break;
            case ARRAY_OPENED:
                if (token.equals("]")) {
                    typeSpec.arrayLevel++;
                    ps = TypeParseState.AFTER_TYPE;
                } else {
                    throwUnexpectedTokenParseException(pt, "Expected ']' but got");
                }
                break;
            case IN_TYPE_PARAMS:
                switch (token) {
                case "<":
                    typeParamsLevel++;
                    break;
                case ">":
                    if (--typeParamsLevel == 0) {
                        ps = TypeParseState.AFTER_TYPE;
                    }
                    break;
                default:
                    // skip other tokens
                }
                break;
            default:
                assert false : "Unknown state";
            }
        }

        if (ps != TypeParseState.AFTER_TYPE) {
            throwUnexpectedTokenParseException(tokens.get(from - 1), "Incorrect type specifier: no ending " +
                    (ps == TypeParseState.ARRAY_OPENED ? "]" : ">") + " found");
        }
        return i;
    }

    private int parseModifiers(MethodMatchSpec mms, int endTokenIdx) {
        boolean negated = false;
        for (int i = 0; i < endTokenIdx; i++) {
            ParseToken pt = tokens.get(i);
            String token = pt.token;
            assert token.length() > 0;
            if (token.equals("!")) {
                negated = !negated;
            } else if (token.equals("{")) {
                int nextIdx = parseVisibilitiesList(i, endTokenIdx, negated, mms);
                i = nextIdx - 1; // will be incremented
                negated = false; // consumed
            } else if (Character.isJavaIdentifierStart(token.charAt(0))) {
                // either modifier or visibility or type name
                ModifierKeyword mod;
                VisibilityKeyword vis;

                if ((mod = tryParseModifier(token)) != null) {
                    mms.modifierSpecs.add(new ModifierSpec(negated, mod));
                    negated = false;
                } else if ((vis = tryParseVisibility(token)) != null) {
                    if (mms.visibilitySpec != null) {
                        throwUnexpectedTokenParseException(pt, "Duplicate visibility specifier");
                    } else {
                        mms.visibilitySpec = new VisibilitySpec(negated, vis);
                    }
                    negated = false;
                } else if (negated) {
                    // type is not expected here
                    throwUnexpectedTokenParseException(pt, "Unknown modifier");
                } else {
                    // found type, which means that all modifiers are parsed already
                    return i;
                }
            } else if (token.equals("<")) {
                // skip generics
                do {
                    if (++i >= endTokenIdx) {
                        throwUnexpectedTokenParseException(pt, "No closing '>' found for");
                    }
                } while (!tokens.get(i).token.equals(">"));

            } else {
                throwUnexpectedTokenParseException(pt, "Unknown modifier");
            }
        }
        return endTokenIdx;
    }

    /**
     * Parses visibility modifiers list opened with "{" and adds the corresponding {@link VisibilitySpec} to the method
     * matcher specifiers.
     * @param mms the method match specifiers to which the parser shall add the visibility specifier
     * @param idxOpenBrace the index of '{' token which opened the visibility list
     * @param endIdx the end index (excluding)
     *
     * @return the index to continue parsing, greater than 'idxOpenBrace'
     */
    private int parseVisibilitiesList(int idxOpenBrace, int endIdx, boolean negated, MethodMatchSpec mms) {
        EnumSet<VisibilityKeyword> curVisibilities = EnumSet.noneOf(VisibilityKeyword.class);
        for (int i = idxOpenBrace + 1; i < endIdx; i++) {
            ParseToken pt = tokens.get(i);
            String token = pt.token;

            VisibilityKeyword vis = tryParseVisibility(token);
            if (vis == null) {
                throwUnexpectedTokenParseException(pt, "Unknown visibility token");
            }
            curVisibilities.add(vis);
            if (++i >= endIdx) {
                throwUnexpectedTokenParseException(pt, "Expected '}' after token");
            }
            ParseToken nextPt = tokens.get(i);
            String nextToken = nextPt.token;
            if (nextToken.equals("}")) {
                // end of list
                if (mms.visibilitySpec != null) {
                    throwUnexpectedTokenParseException(pt, "Duplicate visibility specifier");
                }
                mms.visibilitySpec = new VisibilitySpec(negated, curVisibilities);
                if (mms.visibilitySpec.visibilities.size() == 0) {
                    throwUnexpectedTokenParseException(nextPt, "Invalid visibilities list: all visibilities are prohibited!");
                }
                return i + 1;
            } else if (!nextToken.equals(",")) {
                throwUnexpectedTokenParseException(nextPt, "Unexpected token in visibility list");
            }
        }

        return endIdx;
    }

    private int indexOfToken(String str, int from) {
        for (int i = from; i < tokens.size(); i++) {
            if (tokens.get(i).token.equals(str)) {
                return i;
            }
        }
        return -1;
    }

    private static ModifierKeyword tryParseModifier(String token) {
        return modifiersByNameUC.get(token.toUpperCase(Locale.ENGLISH));
    }

    private static VisibilityKeyword tryParseVisibility(String token) {
        return visibiltyByNameUC.get(token.toUpperCase(Locale.ENGLISH));
    }

    private Pattern parseMethodNamePattern(ParseToken pt) throws XmxIniParseException {
        // check that all characters are allowed in the method name pattern
        int fromIncl = pt.pos;
        char ch = ctx.ca[fromIncl];
        if (ch != '*' && !Character.isJavaIdentifierStart(ch)) {
            throwInvalidCharacterParseException(fromIncl, "Wrong method name pattern");
        }
        int length = pt.token.length();
        for (int i = 1; i < length; i++) {
            ch = ctx.ca[fromIncl + i];
            if (ch != '*' && !Character.isJavaIdentifierPart(ch)) {
                throwInvalidCharacterParseException(i, "Wrong method name pattern");
            }
        }
        // matches "simple" pattern with additional restrictions, so can use general "parse"
        return PatternsSupport.parse(pt.token);
    }

    private TypePatternSpec parseTypeToken(ParseToken pt) throws XmxIniParseException {
        if (pt.token.equals("*")) {
            return TypePatternSpec.anyType();
        }
        // check that all characters are allowed in the type pattern
        boolean hasDot = false;
        int arrayLevel = 0;
        int fromIncl = pt.pos;
        char ch = ctx.ca[fromIncl];
        if (!Character.isJavaIdentifierStart(ch)) {
            throwInvalidCharacterParseException(fromIncl, "Wrong type pattern");
        }
        if (pt.token.endsWith("...")) {
            arrayLevel += 1;
            pt.token = pt.token.substring(0, pt.token.length() - 3);
        }
        if (pt.token.endsWith(".")) {
            throwInvalidCharacterParseException(fromIncl + pt.token.length() - 1, "Type pattern shall not end by dot");
        }
        int length = pt.token.length();
        for (int i = 1; i < length; i++) {
            ch = ctx.ca[fromIncl + i];
            if (ch == '.') {
                hasDot = true;
            } else if (!Character.isJavaIdentifierPart(ch)) {
                throwInvalidCharacterParseException(i, "Wrong type pattern");
            }
        }
        return new TypePatternSpec(hasDot, pt.token, arrayLevel);
    }

    private boolean isValidIdentifier(ParseToken pt) {
        int fromIncl = pt.pos;
        char ch = ctx.ca[fromIncl];
        if (!Character.isJavaIdentifierStart(ch)) {
            return false;
        }
        int len = pt.token.length();
        for (int i = 1; i < len; i++) {
            ch = ctx.ca[fromIncl + i];
            if (!Character.isJavaIdentifierPart(ch)) {
                return false;
            }
        }
        return true;
    }

    private void throwInvalidCharacterParseException(int pos, String prefixMessage) {
        char ch = ctx.ca[pos];
        throw new XmxIniParseException(prefixMessage + ": invalid character '" + ch + "' at pos=" + pos +
                "; pattern='" + ctx.original + "'");
    }

    private void throwUnexpectedTokenParseException(ParseToken pt, String prefixMessage) {
        String token = pt.token;
        throw new XmxIniParseException(prefixMessage + " '" + token + "' at pos=" + pt.pos +
                "; pattern='" + ctx.original + "'");
    }

    enum ParamsParseState {
        START, AFTER_TYPE, AFTER_NAME, AFTER_ELLIPSIS;
    }

    enum TypeParseState {
        AFTER_TYPE, ARRAY_OPENED, IN_TYPE_PARAMS
    }

    enum VisibilityKeyword {
        PUBLIC (Modifier.PUBLIC),
        PROTECTED (Modifier.PROTECTED),
        PRIVATE (Modifier.PRIVATE),
        PACKAGE (0);

        final int modifierFlag;

        VisibilityKeyword(int modifierFlag) {
            this.modifierFlag = modifierFlag;
        }

    }

    enum ModifierKeyword {
        STATIC (Modifier.STATIC),
        ABSTRACT (Modifier.ABSTRACT),
        FINAL (Modifier.FINAL),
        SYNCHRONIZED (Modifier.SYNCHRONIZED),
        STRICTFP (Modifier.STRICT),
//        DEFAULT (Modifier.DEFAULT),
        NATIVE (Modifier.NATIVE);

        final int modifierFlag;

        ModifierKeyword(int modifierFlag) {
            this.modifierFlag = modifierFlag;
        }
    }

    static class VisibilitySpec {
        EnumSet<VisibilityKeyword> visibilities;

        VisibilitySpec(boolean negated, EnumSet<VisibilityKeyword> visibilities) {
            this.visibilities = negated ? EnumSet.complementOf(visibilities) : visibilities;
        }

        VisibilitySpec(boolean negated, VisibilityKeyword vis) {
            this (negated, EnumSet.of(vis));
        }

        void writeModifierFlags(ModifierFlags flags) {
            if (visibilities.contains(VisibilityKeyword.PACKAGE)) {
                // as package-private visibility has no modifier, we shall convert 'allowed' set to 'prohibited'
                for (VisibilityKeyword vis : EnumSet.complementOf(visibilities)) {
                    flags.prohibitedModifiers |= vis.modifierFlag;
                }
            } else if (visibilities.size() == 1) {
                // use 'required' flags if exactly one non-default visibility is allowed
                // (alternate would work too, but it is suboptimal)
                flags.requiredModifiers |= visibilities.iterator().next().modifierFlag;
            } else {
                // any of these modifier flags fits
                for (VisibilityKeyword vis : visibilities) {
                    flags.alternateModifiers |= vis.modifierFlag;
                }
            }
        }
    }

    static class ModifierSpec {
        boolean negated;
        ModifierKeyword keyword;

        ModifierSpec(boolean negated, ModifierKeyword keyword) {
            this.negated = negated;
            this.keyword = keyword;
        }

        void writeModifierFlags(ModifierFlags flags) {
            if (negated) {
                flags.prohibitedModifiers |= keyword.modifierFlag;
            } else {
                flags.requiredModifiers |= keyword.modifierFlag;
            }
        }
    }

    static class MethodMatchSpec {
        VisibilitySpec visibilitySpec;
        List<ModifierSpec> modifierSpecs = new ArrayList<>();

        TypePatternSpec returnTypeSpec;

        Pattern namePattern;
        MethodParamsSpec parameters;

        ITypeMatcher getReturnTypeMatcher() {
            return returnTypeSpec == null ? null : returnTypeSpec.toMatcher();
        }

        public IMethodMatcher getParametersMatcher() {
            return parameters == null ? IMethodMatcher.ANY_METHOD : parameters.toMatcher();
        }
    }

    static class ParseContext {
        final String original;
        final char[] ca;

        ParseContext(String pattern) {
            original = pattern;
            ca = original.toCharArray();
        }
    }

    static class ParseToken {
        String token;
        int pos;

        ParseToken(String token, int pos) {
            this.token = token;
            this.pos = pos;
        }
    }

    static class TypePatternSpec {
        boolean anyType;
        boolean fullyQualified;
        String name;
        int arrayLevel;

        TypePatternSpec(boolean anyType, boolean fullyQualified, String name, int arrayLevel) {
            this.anyType = anyType;
            this.fullyQualified = fullyQualified;
            this.name = name;
            this.arrayLevel = arrayLevel;
        }

        public TypePatternSpec(boolean fullyQuialified, String name, int arrayLevel) {
            this.fullyQualified = fullyQuialified;
            this.name = name;
            this.arrayLevel = arrayLevel;
        }

        public static TypePatternSpec anyType() {
            return new TypePatternSpec(true, false, null, 0);
        }

        ITypeMatcher toMatcher() {
            return anyType ? ITypeMatcher.ANY_TYPE : new TypeMatcher(fullyQualified, name, arrayLevel);
        }
    }

    static class MethodParamsSpec {
        List<TypePatternSpec> requiredParamTypes;
        boolean allowExtraParams;

        MethodParamsSpec(List<TypePatternSpec> requiredParamTypes, boolean allowExtraParams) {
            this.requiredParamTypes = requiredParamTypes;
            this.allowExtraParams = allowExtraParams;
        }

        boolean isAny() {
            return allowExtraParams && requiredParamTypes.size() == 0;
        }

        IMethodMatcher toMatcher() {
            if (isAny()) {
                return IMethodMatcher.ANY_METHOD;
            }
            List<ITypeMatcher> typeMatchers = new ArrayList<>(requiredParamTypes.size());
            for (TypePatternSpec typeSpec : requiredParamTypes) {
                typeMatchers.add(typeSpec.toMatcher());
            }
            return new ParametersMatcher(typeMatchers, allowExtraParams);
        }
    }

    static {
        for (MethodPatternParser.ModifierKeyword m : MethodPatternParser.ModifierKeyword.values()) {
            modifiersByNameUC.put(m.name(), m);
        }
        for (MethodPatternParser.VisibilityKeyword v : MethodPatternParser.VisibilityKeyword.values()) {
            visibiltyByNameUC.put(v.name(), v);
        }
    }
}

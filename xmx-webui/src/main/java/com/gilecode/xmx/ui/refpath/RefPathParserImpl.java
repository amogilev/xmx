// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

import com.gilecode.xmx.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.gilecode.xmx.ui.UIConstants.*;

public class RefPathParserImpl implements RefPathParser {

    /*
    RefPath grammar is like this:
        RefPath ::= (ProxyPrefix)?(RootSingleton|RootId)('.'(FieldSuffix|BeanSuffix))*
        RootSingleton ::= $:'AppName':ClassName:
    or, in more details
        RefPath ::= ($P:)?$(:'<quote_escaped_literal>':<literal>:|<num>)(.(<literal>|#'<quote_escaped_literal>'))*
     */

    @Override
    public RefPath parse(String refpath) throws RefPathSyntaxException {

        String tail;

        boolean requireProxy = false;
        RefPathRoot root;

        if (refpath.startsWith(PROXY_PATH_PREFIX)) {
            refpath = refpath.substring(PROXY_PATH_PREFIX.length());
            requireProxy = true;
        }
        if (refpath.startsWith(PERMA_PATH_PREFIX)) {
            // permanent path like "$:'APP':CLASS:" or "$:'APP':CLASS:.f1.f2"
            int n = PERMA_PATH_PREFIX.length();
            String appName, className;
            if (refpath.length() > n && refpath.charAt(n) == '\'') {
                try {
                    String[] parts = StringUtils.extractQuoted(refpath, n);
                    appName = parts[0];
                    tail = parts[1];
                } catch (IllegalArgumentException e) {
                    throw new RefPathSyntaxException("Missing closing quote character after APP in PermaRefPath", refpath);
                }
            } else {
                int endApp = refpath.indexOf(':', n);
                appName = endApp >= 0 ? refpath.substring(n, endApp) : refpath;
                tail = endApp >= 0 ? refpath.substring(endApp) : "";
            }
            if (!tail.startsWith(":")) {
                throw new RefPathSyntaxException("Missing ':' after the app name", refpath);
            }
            int endClass = tail.indexOf(':', 1);
            if (endClass <= 1) {
                throw new RefPathSyntaxException("Missing ':' after the class name", refpath);
            }
            className = tail.substring(1, endClass);
            root = new RefPathSingletonRoot(appName, className);

            if (endClass == tail.length() - 1) {
                tail = "";
            } else if (tail.charAt(endClass + 1) != '.') {
                throw new RefPathSyntaxException("Missing \":.\" between the class name and subcomponents", refpath);
            } else {
                tail = tail.substring(endClass + 2);
            }
        } else {
            // general refpath like "$123" or "$123.f1.f2"
            int endId = refpath.indexOf('.');
            String refId = endId < 0 ? refpath : refpath.substring(0, endId);
            tail = endId < 0 ? "" : refpath.substring(endId + 1);
            int objectId = parseRefId(refId);
            root = new RefPathIdRoot(objectId);
        }

        List<RefPathSuffix> suffixes;
        if (tail.isEmpty()) {
            suffixes = Collections.emptyList();
        } else {
            suffixes = extractRefPathSuffixes(tail, refpath);
        }

        return new RefPath(requireProxy, root, suffixes);
    }

    List<RefPathSuffix> extractRefPathSuffixes(String subPath, String fullRefPath) throws RefPathSyntaxException {
        List<RefPathSuffix> suffixes;
        List<String> parts = extractPathElements(subPath, fullRefPath);
        suffixes = new ArrayList<>(parts.size());
        for (String part : parts) {
            suffixes.add(parseRefPathSuffix(part, fullRefPath));
        }
        return suffixes;
    }

    RefPathSuffix parseRefPathSuffix(String part, String fullRefPath) throws RefPathSyntaxException {
        RefPathSuffix suffix;
        if (part.isEmpty()) {
            throw new RefPathSyntaxException("Empty element part", fullRefPath);
        } else if (part.startsWith("##")) {
            suffix = new RefPathBeanSuffix(part.substring(2), true);
        } else if (part.startsWith("#")) {
            suffix = new RefPathBeanSuffix(part.substring(1), false);
        } else if (Character.isDigit(part.charAt(0))) {
            try {
                int idx = Integer.parseInt(part);
                suffix = new RefPathArrayElementSuffix(idx);
            } catch (NumberFormatException e) {
                throw new RefPathSyntaxException("Illegal refpath: expect integer index: '" +  part + "'", fullRefPath, e);
            }
        } else {
            suffix = new RefPathFieldSuffix(part);
        }
        return suffix;
    }

    // splits to path elements by '.' with respect to quoted names, unquotes names and unescapes quotes
    List<String> extractPathElements(String subPath, String fullRefPath) throws RefPathSyntaxException {
        if (subPath.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> parts = new ArrayList<>(5);
        while (true) {
            int nDashes = countStartDashes(subPath);
            if (subPath.length() > nDashes + 1 && subPath.charAt(nDashes) == '\'') {
                try {
                    String[] split = StringUtils.extractQuoted(subPath, nDashes);
                    parts.add(nDashes > 0 ? subPath.substring(0, nDashes) + split[0] : split[0]);
                    if (split[1].isEmpty()) {
                        break;
                    } else if (split[1].startsWith(".")) {
                        subPath = split[1].substring(1);
                    } else {
                        throw new RefPathSyntaxException("Missing '.' after the quoted substring (" + subPath + ")", fullRefPath);
                    }
                } catch (IllegalArgumentException e) {
                    throw new RefPathSyntaxException(e.getMessage(), fullRefPath);
                }
            } else {
                int n = subPath.indexOf('.');
                if (n < 0) {
                    parts.add(subPath);
                    break;
                } else {
                    parts.add(subPath.substring(0, n));
                    subPath = subPath.substring(n + 1);
                }
            }
        }
        return parts;
    }

    private int countStartDashes(String s) {
        int n = 0;
        while (s.length() > n && s.charAt(n) == '#')
            n++;
        return n;
    }

    private int parseRefId(String path) throws RefPathSyntaxException {
        if (path.startsWith(REFPATH_PREFIX)) {
            String idStr = path.substring(REFPATH_PREFIX.length());
            try {
                return Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                throw new RefPathSyntaxException("Illegal refpath: integer ID expected after starting '" +
                        REFPATH_PREFIX + "'", path, e);
            }
        } else {
            throw new RefPathSyntaxException("Illegal refpath: shall start with '" + REFPATH_PREFIX +
                    "' followed by integer ID", path);
        }
    }
}

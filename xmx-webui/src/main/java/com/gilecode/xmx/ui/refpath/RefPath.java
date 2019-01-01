// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

import java.util.List;

import static com.gilecode.xmx.ui.UIConstants.PROXY_PATH_PREFIX;

/**
 * Representation of a parsed refpath
 */
public class RefPath {
    private final boolean requireProxy;
    private final RefPathRoot root;
    private final List<RefPathSuffix> suffixes;

    public RefPath(boolean requireProxy, RefPathRoot root, List<RefPathSuffix> suffixes) {
        this.requireProxy = requireProxy;
        this.root = root;
        this.suffixes = suffixes;
    }

    public boolean isRequireProxy() {
        return requireProxy;
    }

    public RefPathRoot getRoot() {
        return root;
    }

    public List<RefPathSuffix> getSuffixes() {
        return suffixes;
    }

    @Override
    public String toString() {
        return buildPath(Integer.MAX_VALUE);
    }

    public String buildPath(int nSuffixes) {
        StringBuilder sb = new StringBuilder();
        if (requireProxy) {
            sb.append(PROXY_PATH_PREFIX);
        }
        sb.append(root);
        for (int i = 0; i < nSuffixes && i < suffixes.size(); i++) {
            sb.append('.').append(suffixes.get(i));
        }
        return sb.toString();
    }
}

// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.refpath;

/**
 * An abstract parser for a refpath
 */
public interface RefPathParser {

    /**
     * Parses refpath and returns its representation
     */
    RefPath parse(String refpath) throws RefPathSyntaxException;

}

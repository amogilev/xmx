// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.spring;

import com.gilecode.xmx.util.Pair;

import java.util.Set;

public interface IXmxSpringService {

    /**
     * Returns all resolved placeholders or properties for a Spring context with the
     * specified XMX id.
     */
    Set<Pair<String, String>> getContextResolvedValues(ResolvedValueKind kind, int ctxObjectId);

}

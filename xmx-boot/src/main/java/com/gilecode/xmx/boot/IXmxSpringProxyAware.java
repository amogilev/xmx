// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

public interface IXmxSpringProxyAware {

    void registerProxy(Object target, Object proxy);
}

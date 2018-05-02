// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop;

import java.lang.annotation.*;

/**
 * Specifies that a method is an AOP 'advice' which shall be invoked when a call to a managed method is intercepted.
 * The correspondence of target method(s) and advices are specified in XMX configuration.
 * <p/>
 * The annotated advices method may be either static or instance methods. If non-static, then a new instance of the
 * advice class will be created any time an intercepted method is invoked. This is only needed for implementing stateful
 * 'AROUND' aspect logic, like checking the execution times.
 * <p/>
 * Consequently, any longer term state (like counters) shall be saved elsewhere. Moreover, if the advice class is used
 * for intercepting methods from several classes, loaded with different class loaders, XMX will load the advice class
 * several times, using the corresponding class loaders (derived to contain the advices JAR).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Advice {

	AdviceKind value();
}

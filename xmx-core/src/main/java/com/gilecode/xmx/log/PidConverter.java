// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.lang.management.ManagementFactory;

/**
 * Converter which allows using process IDs in Logback patterns.
 */
public class PidConverter extends ClassicConverter {

	private static final String PROCESS_ID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

	@Override
	public String convert(ILoggingEvent event) {
		return PROCESS_ID;
	}
}

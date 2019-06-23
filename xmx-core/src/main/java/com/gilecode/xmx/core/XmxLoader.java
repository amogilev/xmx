// Copyright © 2017-2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import com.gilecode.xmx.boot.IXmxBootService;
import com.gilecode.xmx.cfg.IXmxConfig;
import com.gilecode.xmx.cfg.impl.XmxIniConfig;
import com.gilecode.xmx.log.LogbackConfigurator;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

/**
 * Used to configures XMX environment (including logging) and create XmxManager instance.
 */
public class XmxLoader {

	private static XmxManager instance;

	public synchronized static IXmxBootService createXmxService(Map<String, String> overrideSystemProps, File homeDir) {
		if (instance != null) {
			throw new IllegalStateException("XMX is already initialized");
		}

		XmxIniConfig config = XmxIniConfig.getDefault(overrideSystemProps);
		configureLogging(config);

		instance = new XmxManager(config, homeDir);

		if (instance.isEnabled()) {
			XmxWelcomeProvider.printWelcomeHeader(config, homeDir);
		}

		return instance;
	}

	// invoked from Web UI
	public static XmxManager getServiceInstance() {
		if (instance == null) {
			throw new IllegalStateException("XMX is not initialized yet");
		}

		return instance;
	}

	private static void configureLogging(IXmxConfig config) {
		LogbackConfigurator.setConfig(config);

		// the following line actually initializes SLF4J/Logback logging
		LoggerFactory.getLogger(XmxLoader.class);
		config.onLoggingInitialized();
	}
}


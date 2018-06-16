// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import com.gilecode.ucfg.ConfigLoadStatus;
import com.gilecode.xmx.boot.IXmxBootService;
import com.gilecode.xmx.cfg.IXmxConfig;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.cfg.impl.XmxIniConfig;
import com.gilecode.xmx.log.LogbackConfigurator;
import com.gilecode.xmx.service.IXmxService;
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

		XmxIniConfig config = XmxIniConfig.getDefault();
		config.overrideSystemProperties(overrideSystemProps);
		configureLogging(config);

		instance = new XmxManager(config, homeDir);

		if (instance.isEnabled()) {
			printWelcomeMessage(config, homeDir);
		}

		return instance;
	}

	// invoked from Web UI
	public static IXmxService getServiceInstance() {
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

	private static void printWelcomeMessage(IXmxConfig config, File homeDir) {
		StringBuilder sb = new StringBuilder(1024);
		String prefix = "=[XMX]= ";
		String newline = System.lineSeparator();
		String implVersion = XmxLoader.class.getPackage().getImplementationVersion();

		ConfigLoadStatus cfgStatus = config.getLoadStatus();
		sb.append(prefix).append("XMX Agent ").append(implVersion).append(" is started using configuration in ")
				.append(config.getConfigurationFile());
		if (cfgStatus != ConfigLoadStatus.SUCCESS) {
			sb.append(" (").append(cfgStatus).append(")");
		}
		sb.append(newline);

		sb.append(prefix).append("XMX Home Dir=").append(homeDir).append(newline);

		sb.append(prefix).append(LogbackConfigurator.getLastStatus()).append(newline);
		if (config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_ENABLED).asBool()) {
			String webPort = config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asString();
			sb.append(prefix).append("Web console will be started at http://localhost:").append(webPort).append(newline);
		}
		System.out.print(sb.toString());
	}
}


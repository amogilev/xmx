// Copyright © 2019 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import com.gilecode.ucfg.ConfigLoadStatus;
import com.gilecode.xmx.cfg.IXmxConfig;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.cfg.PropertyValue;
import com.gilecode.xmx.log.LogbackConfigurator;

import java.io.File;

public class XmxWelcomeProvider {

    static void printWelcomeMessage(IXmxConfig config, File homeDir) {
        StringBuilder sb = new StringBuilder(1024);
        String prefix = "=[" + getWelcomeXmxName(config) + "]= ";
        String newline = System.lineSeparator();
        String implVersion = XmxLoader.class.getPackage().getImplementationVersion();

        ConfigLoadStatus cfgStatus = config.getLoadStatus();
        sb.append(prefix).append("XMX Agent ").append(implVersion).append(" is started using configuration in ")
                .append(config.getConfigurationFile());
        if (cfgStatus != ConfigLoadStatus.SUCCESS) {
            sb.append(" (").append(cfgStatus.message()).append(")");
        }
        sb.append(newline);

        if (!skipWelcomeHomeLocation(config)) {
            sb.append(prefix).append("Home Dir = ").append(homeDir).append(newline);
        }

        sb.append(prefix).append(LogbackConfigurator.getLastStatus()).append(newline);
        if (config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_ENABLED).asBool()) {
            String webPort = config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asString();
            sb.append(prefix).append("Web console will be started at http://localhost:").append(webPort)
                    .append("/smx/") // may need to configure main UI URL for different XMX editions
                    .append(newline);
        }
        System.out.print(sb.toString());
    }

    private static boolean skipWelcomeHomeLocation(IXmxConfig config) {
        PropertyValue prop = config.getSystemProperty(Properties.GLOBAL_WELCOME_SKIP_HOME);
        return prop != null && prop.asBool();
    }

    private static String getWelcomeXmxName(IXmxConfig config) {
        PropertyValue prop = config.getSystemProperty(Properties.GLOBAL_WELCOME_XMX_NAME);
        return prop == null ? "XMX" : prop.asString();
    }
}

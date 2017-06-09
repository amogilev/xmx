package am.xmx.log;

import am.xmx.cfg.IXmxConfig;
import am.xmx.cfg.Properties;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configurator for Logback which uses XMX configuration as the source of properties,
 */
public class LogbackConfigurator extends ContextAwareBase implements Configurator {

	private static IXmxConfig config;

	public static void setConfig(IXmxConfig newConfig) {
		config = newConfig;
	}

	private static final String OFF_STATUS = "Logging is OFF";
	private static String lastConfigureStatus;

	public static String getLastStatus() {
		return lastConfigureStatus;
	}

	@Override
	public void configure(LoggerContext lc) {
		if (config == null) {
			// pre-mature configuration, ignore
			Logger rootLogger = lc.getLogger("ROOT");
			rootLogger.setLevel(Level.OFF);
			lastConfigureStatus = OFF_STATUS;
			return;
		}
		addCustomConverters(lc);

		String logbackCfgPath = config.getSystemProperty(Properties.GLOBAL_LOG_CFG_FILE).asString();
		if (!logbackCfgPath.isEmpty() && configureUsingFile(lc, logbackCfgPath)) {
			return;
		}

		OutputStreamAppender<ILoggingEvent> appender;

		String logOut = config.getSystemProperty(Properties.GLOBAL_LOG_DIR).asString();
		appender = createAppender(lc, logOut);

		String levelStr = config.getSystemProperty(Properties.GLOBAL_LOG_LEVEL).asString();
		Level xmxLevel = Level.toLevel(levelStr);
		// non-xmx libs (e.g. Jetty) shall have WARN level at minimum
		Level rootLevel = xmxLevel.isGreaterOrEqual(Level.WARN) ? xmxLevel : Level.WARN;

		Logger rootLogger = lc.getLogger("ROOT");
		rootLogger.setLevel(rootLevel);
		rootLogger.addAppender(appender);

		Logger xmxLogger = lc.getLogger("am");
		xmxLogger.setLevel(xmxLevel);

		StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
		lastConfigureStatus = "Logging " + xmxLevel + " events to " + logOut;
	}

	private boolean configureUsingFile(LoggerContext lc, String logbackCfgPath) {
		// use custom configuration
		if (logbackCfgPath.contains("${")) {
			logbackCfgPath = OptionHelper.substVars(logbackCfgPath, lc);
		}
		File cfgFile = new File(logbackCfgPath);
		if (!cfgFile.isFile()) {
			lc.getStatusManager().add(new ErrorStatus(
					"Configuration file not found: " + logbackCfgPath, this));
			return false;
		} else if (!getExtension(logbackCfgPath).equalsIgnoreCase("xml")) {
			lc.getStatusManager().add(new ErrorStatus(
					"Only XML Logback configuration files are supported: " + logbackCfgPath, this));
			return false;
		}
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(lc);
		try {
			configurator.doConfigure(cfgFile.toURI().toURL());
		} catch (JoranException e) {
			lc.getStatusManager().add(new ErrorStatus(
					"Failed to parse Logback configuration: " + logbackCfgPath, this, e));
			return false;
		} catch (MalformedURLException e) {
			lc.getStatusManager().add(new ErrorStatus(
					"Failed to read Logback configuration: " + logbackCfgPath, this, e));
			return false;
		}

		// success
		StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
		lastConfigureStatus = "Logging is configured by " + cfgFile;
		return true;
	}

	private String getExtension(String filename) {
		int i = filename.lastIndexOf('.');
		return i >= 0 ? filename.substring(i + 1) : "";
	}

	private OutputStreamAppender<ILoggingEvent> createAppender(LoggerContext lc, String logOut) {
		OutputStreamAppender<ILoggingEvent> appender;
		Layout<ILoggingEvent> layout;
		if (logOut.equalsIgnoreCase("STDOUT")) {
			appender = new ConsoleAppender<>();
			appender.setName("STDOUT");
			layout = getConsoleLayout(lc);
		} else if (logOut.equalsIgnoreCase("STDERR") || logOut.equalsIgnoreCase("CONSOLE")) {
			ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
			ca.setTarget(ConsoleTarget.SystemErr.getName());
			appender = ca;
			appender.setName("STDERR");
			layout = getConsoleLayout(lc);
		} else {
			// expected to be a directory name, maybe with ${user.home} placeholder
			if (logOut.contains("${")) {
				logOut = OptionHelper.substVars(logOut, lc);
			}
			if (!logOut.endsWith("/") && !logOut.endsWith("\\")) {
				logOut += "/";
			}

			RollingFileAppender<ILoggingEvent> rfa = new RollingFileAppender<>();
			rfa.setName("ROLLING");
			rfa.setPrudent(true);
			rfa.setAppend(true);

			SizeAndTimeBasedRollingPolicy rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
			rollingPolicy.setMaxFileSize(FileSize.valueOf("2mb"));
			rollingPolicy.setContext(lc);
			rollingPolicy.setMaxHistory(7);
			rollingPolicy.setTotalSizeCap(FileSize.valueOf("20mb"));
			rollingPolicy.setFileNamePattern(logOut + "xmx-%d{yyyy-MM-dd}.%i.log");
			rollingPolicy.setParent(rfa);
			rollingPolicy.start();

			rfa.setRollingPolicy(rollingPolicy);
			appender = rfa;

			layout = getFileLayout(lc);
		}

		LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
		encoder.setContext(lc);
		encoder.setLayout(layout);
		encoder.start();

		appender.setContext(lc);
		appender.setEncoder(encoder);
		appender.start();
		return appender;
	}

	private Layout<ILoggingEvent> getFileLayout(LoggerContext lc) {
		return getPatternLayout(lc, "%d{HH:mm:ss.SSS} [%pid][%thread] %-5level %logger{36} - %msg%n");
	}

	private Layout<ILoggingEvent> getConsoleLayout(LoggerContext lc) {
		return getPatternLayout(lc, "[XMX] %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
	}

	private PatternLayout getPatternLayout(LoggerContext lc, String pattern) {
		PatternLayout pl = new PatternLayout();
		pl.setPattern(pattern);

		pl.setContext(lc);
		pl.start();

		return pl;
	}

	@SuppressWarnings("unchecked")
	private void addCustomConverters(LoggerContext lc) {
		Map<String, String> ruleRegistry = (Map) lc.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
		if (ruleRegistry == null) {
			ruleRegistry = new HashMap<>();
			context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);
		}
		if (!ruleRegistry.containsKey("pid")) {
			ruleRegistry.put("pid", PidConverter.class.getName());
		}
	}
}

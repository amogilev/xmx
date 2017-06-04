package am.xmx.log;

import am.xmx.cfg.IXmxConfig;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Configurator for Logback which uses XMX configuration as the source of properties,
 */
public class LogbackConfigurator extends ContextAwareBase implements Configurator {

	private static IXmxConfig config;

	public static void setConfig(IXmxConfig newConfig) {
		config = newConfig;
	}

	@Override
	public void configure(LoggerContext lc) {
		if (config == null) {
			// pre-mature configuartion, ignore
			Logger rootLogger = lc.getLogger("ROOT");
			rootLogger.setLevel(Level.OFF);
		} else {
			// TODO configure root level
			// TODO configure appender - FILE or CONSOLE
			ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender();
			ca.setContext(lc);
			ca.setName("console");
			LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder();
			encoder.setContext(lc);

			// TODO: think about the format to use
			TTLLLayout layout = new TTLLLayout();
			layout.setContext(lc);
			layout.start();
			encoder.setLayout(layout);
			ca.setEncoder(encoder);
			ca.start();

			Logger rootLogger = lc.getLogger("ROOT");
			rootLogger.setLevel(Level.WARN);
			rootLogger.addAppender(ca);

			Logger xmxLogger = lc.getLogger("am");
			xmxLogger.setLevel(Level.DEBUG);
		}
	}
}

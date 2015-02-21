package am.xmx.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import am.xmx.loader.XmxLoader;
import am.xmx.service.IXmxService;

@Configuration
public class Config {

	@Bean
	public IXmxService xmxService() {
		return XmxLoader.getService();
	}
}

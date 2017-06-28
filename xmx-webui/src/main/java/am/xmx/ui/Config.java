package am.xmx.ui;

import am.xmx.core.XmxLoader;
import am.xmx.service.IXmxService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

	@Bean
	public IXmxService xmxService() {
		return XmxLoader.getServiceInstance();
	}
}

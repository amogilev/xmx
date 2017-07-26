package am.xmx.ui;

import am.xmx.core.type.IMethodInfoService;
import am.xmx.service.IMapperService;
import am.xmx.service.IXmxService;
import am.xmx.service.XmxServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

	@Bean
	public IXmxService xmxService() {
		return XmxServiceRegistry.getXmxService();
	}

	@Bean
	public IMapperService mapperService() {
		return XmxServiceRegistry.getMapperService();
	}

	@Bean
	public IMethodInfoService methodInfoService() {
		return XmxServiceRegistry.getMethodInfoService();
	}
}

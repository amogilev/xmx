package am.xmx.ui;

import am.xmx.dto.XmxService;
import am.xmx.loader.XmxLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

	@Bean
	public XmxService xmxService(){
		return XmxLoader.getService();
	}
}

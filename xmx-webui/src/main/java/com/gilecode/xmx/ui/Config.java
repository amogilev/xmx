// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui;

import com.gilecode.xmx.core.type.IMethodInfoService;
import com.gilecode.xmx.service.IMapperService;
import com.gilecode.xmx.service.IXmxService;
import com.gilecode.xmx.service.XmxServiceRegistry;
import com.gilecode.xmx.spring.IXmxSpringService;
import com.gilecode.xmx.ui.refpath.RefPathParser;
import com.gilecode.xmx.ui.refpath.RefPathParserImpl;
import com.gilecode.xmx.ui.service.IXmxUiService;
import com.gilecode.xmx.ui.service.XmxUiService;
import com.gilecode.xmx.ui.smx.service.ISmxUiService;
import com.gilecode.xmx.ui.smx.service.SmxUiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

	@Bean
	public IXmxService xmxService() {
		return XmxServiceRegistry.getXmxService();
	}

	@Bean
	public IXmxSpringService xmxSpringService() {
		return XmxServiceRegistry.getXmxSpringService();
	}

	@Bean
	public IMapperService mapperService() {
		return XmxServiceRegistry.getMapperService();
	}

	@Bean
	public IMethodInfoService methodInfoService() {
		return XmxServiceRegistry.getMethodInfoService();
	}

	@Bean
	public IXmxUiService xmxUiService() {
		return new XmxUiService(methodInfoService(), xmxService(), mapperService(), refPathParser());
	}

	@Bean
	public RefPathParser refPathParser() {
		return new RefPathParserImpl();
	}

	@Bean
	public ISmxUiService smxUiService() {
		return new SmxUiService(xmxUiService(), xmxService(), xmxSpringService());
	}

}

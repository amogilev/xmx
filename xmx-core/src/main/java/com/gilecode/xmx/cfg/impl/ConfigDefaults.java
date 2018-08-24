// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import com.gilecode.ucfg.OptionDescription;
import com.gilecode.ucfg.SectionDescription;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.server.IXmxServerLauncher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface ConfigDefaults {
	
	String LINE_SEPARATOR = "\n";
	
	String[] FILE_COMMENTS = {
			"",
			"The XMX configuration: specifies which apps/classes are managed and",
			"which management interfaces and possibilities are provided.",
			"",
			"Lines starting with \"#!\" specify the default options of the current XMX",
			"version and are updated automatically. In order to change a default option,",
			"please uncomment it and modify the value, or just override on a separate",
			"line. Either way, the modified option will persist even in a case of XMX",
			"updates. However, if there are several sections with the same name, they",
			"may be split together.",
			"In case of major updates, it is recommended to compare the changed",
			"values against the defaults and update manually if necessary.",
	};
	
	String[] SECTION_SYSTEM_COMMENTS = {
			"",
			"[System] section contains global settings for XMX system"
	};
	
	String[] SECTION_ALLAPPS_COMMENTS = {
			"",
			"Per-application an per-class settings sections follow, marked as either",
			"[App=app_name_pattern] or [App=app_name_pattern;Class=class_name_pattern],",
			"respectively. These patterns may be just names with optional '*' for simple",
            "cases, or Java RegEx full patterns enclosed in ^$, e.g. \"^(a|bc)[def].*$\".",
            "",
			"Currently the app names are recognized only for the web applications",
			"deployed to Tomcat or Jetty application servers.",
			"",
			"As the application/class names may match several patterns, the settings",
			"override each other, and the latest matching setting wins.",
			"",
			"[App=*] section contains the default settings for all applications."
	};
	
	String[] SECTION_ALLCLASSES_COMMENTS = {
			"",
			"This section contains the default settings for all managed classes in all",
			"managed applications."
	};

	String[] SECTION_SAMPLECLASS_COMMENTS = {
			"",
			"This sample section shows how to override 'Managed'/'ManagedClasses'",
			"options in the previous sections and disable or enable the management of",
			"the matching classes.",
	};

	String[] SECTION_SAMPLEADVICE_COMMENTS = {
			"",
			"This sample section shows how to specify custom AOP-like advices for the",
			"specified target method(s) in the managed class. For more details, see the",
			"section \"AOP Advices\" in README.txt",
	};

	SectionDescription SECTION_SYSTEM_DESC = new SectionDescription(SectionsNamespace.SECTION_SYSTEM,
			SECTION_SYSTEM_COMMENTS,
			new OptionDescription(Properties.GLOBAL_ENABLED, true, "Whether to enable XMX at all"),
			new OptionDescription(Properties.GLOBAL_EMB_SERVER_ENABLED, true, "Whether to enable the embedded web server"),
			new OptionDescription(Properties.GLOBAL_EMB_SERVER_IMPL, "Jetty", "The embedded web server implementation. Only Jetty is supported now"),
			new OptionDescription(Properties.GLOBAL_EMB_SERVER_PORT, 8081, "The port for the embedded web server"),
			new OptionDescription(Properties.GLOBAL_JMX_ENABLED, true, "Whether to publish managed objects to JMX"),
			new OptionDescription(Properties.GLOBAL_LOG_LEVEL, "INFO", "The log level: OFF, ERROR, WARN, INFO or DEBUG"),
			new OptionDescription(Properties.GLOBAL_LOG_DIR, "${user.home}/.xmx/logs/",
				"The directory for log files or STDOUT or STDERR"),
			new OptionDescription(Properties.GLOBAL_LOG_CFG_FILE, "",
				"(optional) A custom Logback configuration XML file")
			);
	
	SectionDescription SECTION_ALLAPPS_DESC = new SectionDescription(SectionsNamespace.SECTION_ALLAPPS, 
			SECTION_ALLAPPS_COMMENTS,
			new OptionDescription(Properties.APP_ENABLED, true, "Whether the management is enabled for the application"),
			new OptionDescription(Properties.specialClassesForm(Properties.SP_MANAGED), 
					"^.*(Service|(?<![rR]eference)Manager|Engine|DataSource)\\d*(Impl\\d*)?$", 
					"Determines instances of which application classes are managed by XMX")
			);

	SectionDescription SECTION_ALLCLASSES_DESC = new SectionDescription(SectionsNamespace.SECTION_ALLCLASSES,
			SECTION_ALLCLASSES_COMMENTS,
			new OptionDescription(Properties.CLASS_MAX_INSTANCES, 10, "Max number of managed instances per class")
	);

	SectionDescription SECTION_SAMPLECLASS_DESC = new SectionDescription("App=sampleapp; Class=com.example.SampleService",
			SECTION_SAMPLECLASS_COMMENTS,
			new OptionDescription(Properties.SP_MANAGED, false, "Specifies whether the matching classes shall be managed or not")
	);

	SectionDescription SECTION_SAMPLEADVICE_DESC = new SectionDescription("App=sampleapp; Class=org.example.MyClassToManage; Method=\"foo(int,String)\"",
            SECTION_SAMPLEADVICE_COMMENTS,
			new OptionDescription(Properties.M_ADVICES, "yourAdvices.jar:org.example.YourAdvicesClass", "Specifies the JAR file and class with the advices to use for the target method(s)")
	);

	List<SectionDescription> ALL_SECTIONS = Collections.unmodifiableList(Arrays.asList(
			SECTION_SYSTEM_DESC, SECTION_ALLAPPS_DESC, SECTION_ALLCLASSES_DESC,
            SECTION_SAMPLECLASS_DESC, SECTION_SAMPLEADVICE_DESC));

	//
	// Hidden (internal) options
	//

	SectionDescription INTERNAL_SECTION_XMX_WEBAPP = new SectionDescription("App=\"" + IXmxServerLauncher.APPNAME + "\"",
			null, new OptionDescription(Properties.APP_ENABLED, false));

	// TODO: maybe make full-qualified classes with advices auto-managed, or "hiddenly" managed
	SectionDescription INTERNAL_SPRING_ADVICES_SECTION1 = new SectionDescription(
			"App=*;" +
			"Class=org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory",
			null,
			new OptionDescription(Properties.SP_MANAGED, true));

	SectionDescription INTERNAL_SPRING_ADVICES_SECTION2 = new SectionDescription(
			"App=*;" +
			"Class=org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;" +
			"Method=\"protected Object initializeBean(String beanName, Object bean, RootBeanDefinition mbd)\"",
			null,
			new OptionDescription(Properties.M_ADVICES,
					"xmx-advices.jar:com.gilecode.xmx.advices.SpringBeanProxyInterceptor"));

	List<SectionDescription> HIDDEN_INTERNAL_SECTIONS = Collections.unmodifiableList(Arrays.asList(
			INTERNAL_SECTION_XMX_WEBAPP, INTERNAL_SPRING_ADVICES_SECTION1, INTERNAL_SPRING_ADVICES_SECTION2));
}

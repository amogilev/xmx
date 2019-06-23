// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.server.impl;

import com.gilecode.xmx.cfg.IXmxConfig;
import com.gilecode.xmx.cfg.Properties;
import com.gilecode.xmx.core.XmxWelcomeProvider;
import com.gilecode.xmx.server.IXmxServerLauncher;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Starter for Embedded Jetty running xmx-webui.war 
 * 
 * @author Andrey Mogilev
 */
public class XmxEmbeddedJettyLauncher implements IXmxServerLauncher {

	private final static Logger logger = LoggerFactory.getLogger(XmxEmbeddedJettyLauncher.class);

	@Override
	public void launchServer(File warFile, IXmxConfig config) {
		try {
			Thread.currentThread().setContextClassLoader(XmxEmbeddedJettyLauncher.class.getClassLoader());

			int port = config.getSystemProperty(Properties.GLOBAL_EMB_SERVER_PORT).asInt();
			// TODO: check port for JVM_Bind, allow auto-select ranges
			
			// make all Jetty threads daemon so that they do not prevent apps shutdown
			QueuedThreadPool threadPool = new QueuedThreadPool(8, 2);
			threadPool.setName("XMX-Jetty-qtp");
			
			threadPool.setDaemon(true);
			
			Server server = new Server(threadPool);
			Scheduler scheduler = new ScheduledExecutorScheduler("XMX-Jetty-Scheduler", true);			
	        ServerConnector connector = new ServerConnector(server, null, scheduler, null, -1, -1, new HttpConnectionFactory()); 
	        
	        connector.setPort(port);
	        server.setConnectors(new Connector[]{connector});
			
	        WebAppContext webapp = new WebAppContext();
	        webapp.setWar(warFile.getAbsolutePath());
	        
	        File jspTempDir = new File(new File(System.getProperty("java.io.tmpdir")), "embedded-jetty-jsp-" + port);
	        if (!jspTempDir.exists()) {
	        	jspTempDir.mkdir();
	        }
	        webapp.setAttribute("javax.servlet.context.tempdir", jspTempDir);
	        webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                    ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");			        

	        
	        // either of two configs below work, but 2nd is bit faster 
/* V1 */			        
//	        org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
//	        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");
/**/			        
	         
/* V2 */ 			        
	        webapp.setAttribute("org.eclipse.jetty.containerInitializers", 
	        		Arrays.asList(new ContainerInitializer(new JettyJasperInitializer(), null)));
	        webapp.addBean(new ServletContainerInitializersStarter(webapp), true);
/**/			        
	 
	        server.setHandler(webapp);
	        server.start();

	        logger.info("Started XMX Web Server at http://localhost:{}", port);

			XmxWelcomeProvider.printWelcomeToStartedUI(config, port);
		} catch (Exception e) {
			logger.error("Failed to launch XMX Web Server", e);
		}
	}
}

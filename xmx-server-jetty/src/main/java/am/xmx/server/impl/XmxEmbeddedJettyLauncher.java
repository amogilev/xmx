package am.xmx.server.impl;

import java.io.File;
import java.util.Arrays;

import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import am.xmx.dto.XmxRuntimeException;
import am.xmx.server.IXmxServerLauncher;

/**
 * Starter for Embedded Jetty running xmx-webui.war 
 * 
 * @author Andrey Mogilev
 */
public class XmxEmbeddedJettyLauncher implements IXmxServerLauncher {
	
	@Override
	public void launchServer(File warFile, int port) {
		// TODO redirect Jetty logging somewhere
//		 LoggingUtil.config();
//		 Log.setLog(new JavaUtilLog());
		
		
		try {
			Thread.currentThread().setContextClassLoader(XmxEmbeddedJettyLauncher.class.getClassLoader());
			
			Server server = new Server(port);
			
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
		} catch (Exception e) {
			throw new XmxRuntimeException(e);
		}
	}
}

package am.xmx.server;

import java.io.File;

/**
 * Abstract interface for application (web) servers which may be started
 * to serve XMX UI.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxServerLauncher {
	
	/**
	 * Launch the server and deploy the specified WAR application there.
	 * 
	 * @param warFile the web application archive file
	 */
	void launchServer(File warFile);
	
}

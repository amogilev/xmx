// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.server;

import com.gilecode.xmx.cfg.IXmxConfig;

import java.io.File;

/**
 * Abstract interface for application (web) servers which may be started
 * to serve XMX UI.
 * 
 * @author Andrey Mogilev
 */
public interface IXmxServerLauncher {

	/**
	 * The webapp name which XMX UI shall have.
	 * <p/>
	 * It is used to suppress management of classes within this app. Make sure that
	 * "display-name" in web.xml has the same value
	 */
	String APPNAME = "XMX Console";

	/**
	 * Launch the server and deploy the specified WAR application there.
	 * @param warFile the web application archive file
	 * @param config the XMX configuration which specifies which port to use and possibly other parameters
	 */
	void launchServer(File warFile, IXmxConfig config);
	
}

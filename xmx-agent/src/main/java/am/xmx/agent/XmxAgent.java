package am.xmx.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.JarFile;

public class XmxAgent {
	
	private static final String XMX_HOME_PROP = "xmx.home.dir";

	public static void premain(String agentArgs, Instrumentation instr) {
		System.err.println("XmxAgent premain");
		
		// TODO: support work from xmx-agent-all.jar - unzip to temp pseudo-distr dir
		// TODO: support XMX_HOME dir
		
		try {
			URL jarLocation = XmxAgent.class.getProtectionDomain().getCodeSource().getLocation();
			File agentBaseDir = new File(jarLocation.toURI()).getParentFile();
			File agentHomeDir = agentBaseDir.getParentFile();

			File xmxApiJarFile = new File(agentHomeDir, "lib" + File.separator + "xmx-api.jar");
			if (!xmxApiJarFile.isFile()) {
				throw new RuntimeException("Failed to determine proper XMX home directory. Please make sure that xmx-agent.jar resides in XMX_HOME/bin");
			}
			
			System.setProperty(XMX_HOME_PROP, agentHomeDir.getAbsolutePath());
			instr.appendToBootstrapClassLoaderSearch(new JarFile(xmxApiJarFile));
			instr.addTransformer(new XmxClassTransformer());
			
		} catch (Exception e) {
			System.err.println("Failed to start XmxAgent");
			e.printStackTrace();
		}
	}
	
	// TODO support starting agent after VM startup
	public static void agentmain(String agentArgs, Instrumentation instr) {
		System.err.println("XmxAgent agentmain");
	}	

}

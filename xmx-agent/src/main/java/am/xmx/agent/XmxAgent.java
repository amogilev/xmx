package am.xmx.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.jar.JarFile;

public class XmxAgent {
	
	public static void premain(String agentArgs, Instrumentation instr) {
		System.err.println("XmxAgent premain");
		
		// TODO: support work from xmx-agent-all.jar - unzip to temp pseudo-distr dir
		
		try {
			URL jarLocation = XmxAgent.class.getProtectionDomain().getCodeSource().getLocation();
			File agentBaseDir = new File(jarLocation.toURI()).getParentFile();
			File xmxApiJarFile = new File(agentBaseDir, ".." + File.separator + "lib" + File.separator + "xmx-api.jar");
			
			instr.appendToSystemClassLoaderSearch(new JarFile(xmxApiJarFile));
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

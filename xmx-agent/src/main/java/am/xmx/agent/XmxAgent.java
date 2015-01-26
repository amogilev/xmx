package am.xmx.agent;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Locale;
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

			File agentLibDir = new File(agentHomeDir, "lib");
			
			// find xmx-api.jar, support optional version 
			File[] xmxApiFiles = agentLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase(Locale.ENGLISH);
					return name.equals("xmx-api.jar") || (name.startsWith("xmx-api-") && name.endsWith(".jar"));
				}
			});
			
			if (xmxApiFiles.length == 0 || !xmxApiFiles[0].isFile()) {
				throw new RuntimeException("Failed to determine proper XMX home directory. Please make sure that xmx-agent.jar resides in XMX_HOME/bin");
			}
			
			System.setProperty(XMX_HOME_PROP, agentHomeDir.getAbsolutePath());
			instr.appendToBootstrapClassLoaderSearch(new JarFile(xmxApiFiles[0]));
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

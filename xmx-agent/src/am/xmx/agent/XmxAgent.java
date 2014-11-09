package am.xmx.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class XmxAgent {
	
	public static void premain(String agentArgs, Instrumentation instr) {
		System.err.println("XmxAgent premain");
		
		// TODO: extract xmx-api.jar from resources to temp dir
		try {
			instr.appendToSystemClassLoaderSearch(new JarFile("W:\\Projects\\xmx\\distr\\xmx-api.jar"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		instr.addTransformer(new XmxClassTransformer());
		
	}
	
	// TODO support starting agent after VM startup
	public static void agentmain(String agentArgs, Instrumentation instr) {
		System.err.println("XmxAgent agentmain");
	}	

}

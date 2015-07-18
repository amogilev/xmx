package am.xmx.loader;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;

import am.xmx.service.IXmxService;
import am.xmx.service.IXmxServiceEx;

public class XmxLoader {
	
	private static ClassLoader xmxClassLoader;
	private static Class<?> xmxManagerClass;
	private static IXmxServiceEx xmxService;

	static {
		String homeDir = System.getProperty(IXmxService.XMX_HOME_PROP);
		if (homeDir == null) {
			// not proper "agent" start... but still try to determine by this jar location
			URL jarLocation = XmxLoader.class.getProtectionDomain().getCodeSource().getLocation();
			if (jarLocation != null) {
				try {
					homeDir = new File(jarLocation.toURI()).getParentFile().getParentFile().getAbsolutePath();
					System.setProperty(IXmxService.XMX_HOME_PROP, homeDir);
				} catch (Exception e) {
					logError("", e);
				}
			} else {
				// seems there is now way to determine JAR location for for bootstrap class loader in
				// this implementation of Java; 
				// TODO check getResource for non-class resources
			}
		}
		
		// create xmxClassLoader from xmx-core.jar; dependencies loaded automatically by manifest's classpath
		File xmxLibDir = homeDir == null ? null : new File(homeDir, "lib");
		if (xmxLibDir == null || !xmxLibDir.isDirectory() || !new File(xmxLibDir, "xmx-core.jar").isFile()) {
			logError("Could not find loadable XMX lib directory, XMX functionality is disabled");
			xmxClassLoader = null;
		} else {
			// find xmx-core.jar, support optional version (like xmx-core-1.0.0.jar) 
			File[] coreImpls = xmxLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase(Locale.ENGLISH);
					return name.equals("xmx-core.jar") || (name.startsWith("xmx-core-") && name.endsWith(".jar")); 
				}
			});
			if (coreImpls.length == 0) {
				logError("Could not find xmx-core.jar, XMX functionality is disabled");
				xmxClassLoader = null;
			} else {
				try {
					URL[] urls = new URL[]{coreImpls[0].toURI().toURL()};
					xmxClassLoader = new URLClassLoader(urls, XmxLoader.class.getClassLoader());
					xmxManagerClass = Class.forName("am.xmx.core.XmxManager", true, xmxClassLoader);
					
					Method getServiceMethod = xmxManagerClass.getDeclaredMethod("getService");
					xmxService = (IXmxServiceEx) getServiceMethod.invoke(null);
					
					// start UI if needed (xmx-webui.war in Embedded Jetty or another server)
					Method xmxManagerStartUIMethod = xmxManagerClass.getDeclaredMethod("startUI");
					xmxManagerStartUIMethod.invoke(null);
					
					
				} catch (Exception e) {
					logError("Failed to find or instantiate XmxManager, XMX functionality is disabled");
					xmxClassLoader = null;
					xmxService = null;
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	public static byte[] transformClass(ClassLoader classLoader, String className, 
			byte[] classBuffer) {
		if (xmxService != null) {
			return xmxService.transformClassIfInterested(classLoader, className, classBuffer);
		} else {
			return classBuffer;
		}
	}
	
	public static void registerObject(Object obj, int classId) {
		if (xmxService != null) {
			xmxService.registerObject(obj, classId);
		}
	}
	
	
	/**
	 * Return singleton implementation of {@link IXmxService}
	 */
	public static IXmxService getService() {
		return xmxService;
	}

	// TODO maybe check presence of common Logs via Class.forName() and use them? 
	private static void logError(String message) {
		System.err.println(message);
	}
	private static void logError(String message, Throwable e) {
		System.err.print(message + " :: ");
		e.printStackTrace(System.err);
	}
}

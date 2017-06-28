package am.xmx.boot;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;

public class XmxProxy {

	private static IXmxBootService xmxService;
	private static boolean initialized = false;

	/**
	 * Initializes XMX with optional override of system (XMX global) properties.
	 * 
	 * @param overrideProperties properties to override, case-insensitive
	 * 
	 * @return whether XMX is initialized successfully and is enabled 
	 */
	synchronized
	public static boolean initialize(Map<String, String> overrideProperties) {
		if (initialized) {
			logError("XMX is already initialized!");
			return xmxService != null;
		}
		initialized = true;
		
		String homeDir = System.getProperty(IXmxBootService.XMX_HOME_PROP);
		if (homeDir == null) {
			// not proper "agent" start... but still try to determine by this jar location
			URL jarLocation = XmxProxy.class.getProtectionDomain().getCodeSource().getLocation();
			if (jarLocation != null) {
				try {
					homeDir = new File(jarLocation.toURI()).getParentFile().getParentFile().getAbsolutePath();
					System.setProperty(IXmxBootService.XMX_HOME_PROP, homeDir);
				} catch (Exception e) {
					logError("", e);
				}
			} else {
				// seems there is no way to determine JAR location for for bootstrap class loader in
				// this implementation of Java; 
				// TODO check getResource for non-class resources
			}
		}
		
		// create xmxClassLoader from xmx-core.jar; dependencies loaded automatically by manifest's classpath
		File xmxLibDir = homeDir == null ? null : new File(homeDir, "lib");
		if (xmxLibDir == null || !xmxLibDir.isDirectory() || !new File(xmxLibDir, "xmx-core.jar").isFile()) {
			logError("Could not find loadable XMX lib directory, XMX functionality is disabled");
		} else {
			// find xmx-core.jar, support optional version (like xmx-core-1.0.0.jar) 
			File[] coreImpls = xmxLibDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					name = name.toLowerCase(Locale.ENGLISH);
					return name.equals("xmx-core.jar") || (name.startsWith("xmx-core-") && name.endsWith(".jar")); 
				}
			});
			if (coreImpls == null || coreImpls.length == 0) {
				logError("Could not find xmx-core.jar, XMX functionality is disabled");
			} else {
				try {
					URL[] urls = new URL[]{coreImpls[0].toURI().toURL()};
					ClassLoader xmxClassLoader = new URLClassLoader(urls, XmxProxy.class.getClassLoader());

					Class<?> xmxLoaderClass =
							Class.forName("am.xmx.core.XmxLoader", true, xmxClassLoader);
					Method xmxCreateSvcMethod =
							xmxLoaderClass.getDeclaredMethod("createXmxService", Map.class);
					xmxService = (IXmxBootService) xmxCreateSvcMethod.invoke(xmxLoaderClass, overrideProperties);

					if (!xmxService.isEnabled()) {
						// disabled
						xmxService = null;
					}
				} catch (Exception e) {
					logError("Failed to find or instantiate XmxManager, XMX functionality is disabled");
					xmxService = null;
					throw new RuntimeException(e);
				}
			}
		}
		
		return xmxService != null;
	}
	
	public static byte[] transformClass(ClassLoader classLoader, String className, 
			byte[] classBuffer, Class<?> classBeingRedefined) {
		if (xmxService != null) {
			// FIXME: move try/catch to XmxManager?
			try {
				return xmxService.transformClassIfInterested(classLoader, className, classBuffer, classBeingRedefined);
			} catch (RuntimeException e) {
				// not really expected. Try-catch used to make sure that XMX bugs do not break users' apps
				logError("XMX: Failed to register class", e);
			}
		}
		
		return classBuffer;
	}

	@SuppressWarnings("unused")
	public static void registerObject(Object obj, int classId) {
		if (xmxService != null) {
			// FIXME: move try/catch to XmxManager?
			try {
				xmxService.registerObject(obj, classId);
			} catch (RuntimeException e) {
				// not really expected. Try-catch used to make sure that XMX bugs do not break users' apps
				logError("XMX: Failed to register object", e);
			}
		}
	}
	
	private static String ERR_PREFIX = "[XMX ERROR!] ";
	private static void logError(String message) {
		System.err.println(ERR_PREFIX + message);
	}
	private static void logError(String message, Throwable e) {
		System.err.print(ERR_PREFIX + message + " :: ");
		e.printStackTrace(System.err);
	}
}

package am.xmx.loader;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import am.xmx.dto.XmxRuntimeException;
import am.xmx.dto.XmxService;

public class XmxLoader {
	
	private static ClassLoader xmxClassLoader;
	private static Class<?> xmxManagerClass;

	private static Method xmxManagerInstrumentMethod;
	private static Method xmxManagerRegisterMethod;
	private static Method xmxManagerGetServiceMethod;
	
	static {
		String homeDir = System.getProperty(XmxService.XMX_HOME_PROP);
		if (homeDir == null) {
			// not proper "agent" start... but still try to determine by this jar location
			URL jarLocation = XmxLoader.class.getProtectionDomain().getCodeSource().getLocation();
			if (jarLocation != null) {
				try {
					homeDir = new File(jarLocation.toURI()).getParentFile().getParentFile().getAbsolutePath();
					System.setProperty(XmxService.XMX_HOME_PROP, homeDir);
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
					
					xmxManagerGetServiceMethod = xmxManagerClass.getDeclaredMethod("getService");
					
					// TODO: maybe add to XmxService? (or XmxServiceEx?)
					xmxManagerInstrumentMethod = xmxManagerClass.getDeclaredMethod("transformClass", 
							ClassLoader.class, String.class, byte[].class); 
					xmxManagerRegisterMethod = xmxManagerClass.getDeclaredMethod("registerObject", 
							Object.class); 
					
					// start UI (wmx-webui.war in Embedded Jetty)
					Method xmxManagerStartUIMethod = xmxManagerClass.getDeclaredMethod("startUI");
					xmxManagerStartUIMethod.invoke(null);
					
				} catch (Exception e) {
					logError("Failed to find or instantiate XmxManager, XMX functionality is disabled");
					xmxClassLoader = null;
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	public static byte[] transformClass(ClassLoader classLoader, String className, 
			byte[] classBuffer) {
		if (xmxClassLoader != null && maybeInterested(classLoader, className)) {
			try {
				Object result = xmxManagerInstrumentMethod.invoke(null, classLoader, className, classBuffer);
				return (byte[]) result;
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("Failed to invoke xmxManagerInstrumentMethod", e);
				return classBuffer;
			}
		}
		return classBuffer;
	}
	
	public static void registerObject(Object obj) {
		if (xmxClassLoader != null) {
			try {
				xmxManagerRegisterMethod.invoke(null, obj);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("Failed to invoke xmxManagerRegisterMethod", e);
			}
		}
	}
	
	
	/**
	 * Return singleton implementation of {@link XmxService}
	 */
	public static XmxService getService() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object result = xmxManagerGetServiceMethod.invoke(null);
		return (XmxService) result;
	}

	private static boolean maybeInterested(ClassLoader classLoader,
			String className) {
		// TODO: read pattern from config, maybe check ClassLoader too
		return className.endsWith("Service") || className.endsWith("ServiceImpl") 
				|| className.endsWith("DataSource");
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

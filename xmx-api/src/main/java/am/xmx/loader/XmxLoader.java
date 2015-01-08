package am.xmx.loader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import am.xmx.dto.XmxService;

public class XmxLoader {
	
	private static ClassLoader xmxClassLoader;
	private static Class<?> xmxManagerClass;

	private static Method xmxManagerInstrumentMethod;
	private static Method xmxManagerRegisterMethod;
	private static Method xmxGetServiceMethod;
	
	static {
		File xmxLibDir = null;
		try {
			URL jarLocation = XmxLoader.class.getProtectionDomain().getCodeSource().getLocation();
			xmxLibDir = new File(jarLocation.toURI()).getParentFile();
		} catch (Exception e) {
			logError("", e);
		}
		
		if (xmxLibDir == null || !xmxLibDir.isDirectory() || !new File(xmxLibDir, "xmx-core.jar").isFile()) {
			logError("Could not find loadable XMX lib directory, XMX functionality is disabled");
			xmxClassLoader = null;
		} else {
			List<URL> jarUrls = new ArrayList<>();
			
			File[] files = xmxLibDir.listFiles();
			for (File f : files) {
				String fname = f.getName().toLowerCase(); 
				if (fname.endsWith(".jar") && !fname.startsWith("xmx-api.")) {
					try {
						jarUrls.add(f.toURI().toURL());
					} catch (MalformedURLException e) {
						logError("Corrupted XMX library: " + f, e);
					}
				}
			}
			
			if (jarUrls.isEmpty()) {
				logError("Could not find XMX jars, XMX functionality is disabled");
				xmxClassLoader = null;
			} else {
				xmxClassLoader = new URLClassLoader(jarUrls.toArray(new URL[jarUrls.size()]), XmxLoader.class.getClassLoader());
				try {
					xmxManagerClass = Class.forName("am.xmx.core.XmxManager", true, xmxClassLoader);
					
					xmxGetServiceMethod = xmxManagerClass.getDeclaredMethod("getService");
					
					// TODO: maybe add to XmxService? (or XmxServiceEx?)
					xmxManagerInstrumentMethod = xmxManagerClass.getDeclaredMethod("transformClass", 
							ClassLoader.class, String.class, byte[].class); 
					xmxManagerRegisterMethod = xmxManagerClass.getDeclaredMethod("registerObject", 
							Object.class); 
					
				} catch (Exception e) {
					logError("Failed to find or instantiate XmxManager, XMX functionality is disabled");
					xmxClassLoader = null;
					throw new IllegalArgumentException(e);
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
		Object result = xmxGetServiceMethod.invoke(null);
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

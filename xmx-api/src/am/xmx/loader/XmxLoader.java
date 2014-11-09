package am.xmx.loader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import am.xmx.dto.XmxClassInfo;
import am.xmx.dto.XmxObjectDetails;
import am.xmx.dto.XmxObjectInfo;
import am.xmx.dto.XmxRuntimeException;
import am.xmx.dto.XmxService;

public class XmxLoader {
	
	private static ClassLoader xmxClassLoader;
	private static Class<?> xmxManagerClass;

	private static Method xmxManagerInstrumentMethod;
	private static Method xmxManagerRegisterMethod;
	private static Method xmxGetServiceMethod;
	
	static {
		ClassLoader parentClassLoader = XmxLoader.class.getClassLoader();
		// TODO better use JarInTheJar concept for xmx libs, but for now separate folder is fine
		URL xmxLibUrl = parentClassLoader.getResource("xmx");
		if (xmxLibUrl == null) {
			// TODO temporal solution, replace with jar-in-jar
			try {
				xmxLibUrl = new File("W:\\Projects\\xmx\\distr\\xmxlib").toURI().toURL();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if (xmxLibUrl == null || xmxLibUrl.getFile() == null || xmxLibUrl.getFile().equals("") || !new File(xmxLibUrl.getFile()).isDirectory()) {
			logError("Could not find loadable XMX lib directory, XMX functionality is disabled");
			xmxClassLoader = null;
		} else {
			List<URL> jarUrls = new ArrayList<>();
			
			File xmxLibDir = new File(xmxLibUrl.getFile());
			File[] files = xmxLibDir.listFiles();
			for (File f : files) {
				if (f.getName().toLowerCase().endsWith(".jar")) {
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

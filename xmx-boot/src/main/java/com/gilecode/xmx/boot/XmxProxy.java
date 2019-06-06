// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.boot;

import com.gilecode.xmx.aop.log.IAdviceLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class XmxProxy {

	private static IXmxBootService xmxService;
	private static boolean initialized = false;

	/**
	 * Initializes XMX with optional override of system (XMX global) properties.
	 * 
	 * @param overrideProperties properties to override, case-insensitive
	 * @param homeDir XMX home dir, detected by the location of agent JAR
	 * @param xmxVersion the version of XMX libraries to load
	 *
	 * @return whether XMX is initialized successfully and is enabled
	 */
	synchronized
	public static boolean initialize(Map<String, String> overrideProperties, File homeDir, String xmxVersion) {
		if (initialized) {
			logError("XMX is already initialized!");
			return xmxService != null;
		}
		initialized = true;
		
		if (homeDir == null) {
			// not proper "agent" start... but still try to determine by this jar location
			URL jarLocation = XmxProxy.class.getProtectionDomain().getCodeSource().getLocation();
			if (jarLocation != null) {
				try {
					homeDir = new File(jarLocation.toURI()).getParentFile().getParentFile().getAbsoluteFile();
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
		if (xmxLibDir == null || !xmxLibDir.isDirectory()) {
			logError("Could not find loadable XMX lib directory, XMX functionality is disabled");
		} else {
			// find xmx-core.jar, support optional version (like xmx-core-1.0.0.jar)
			String[] libFileNames = xmxLibDir.list();
			String versionSuffix = xmxVersion.isEmpty() ? "" : "-" + xmxVersion;
			String coreImplName = findLibraryJar(libFileNames, "xmx-core" + versionSuffix + ".jar");
			if (coreImplName == null) {
				logError("Could not find xmx-core.jar, XMX functionality is disabled");
			} else {
				try {
					List<String> optCoreJars = findLibraryJars(libFileNames, "xmx-core-opt", versionSuffix);
					URL[] urls = makeJarsUrls(xmxLibDir, Collections.singleton(coreImplName), optCoreJars);

					// NOTE: cannot use AppClassLoader as parent, as need to use our version os some jars (e.g. logback)
					//       Bootstrap CL as parent seems fine though. Alternatively, implement "local-first"
					//       policy in XmxUrlClassLoader
					ClassLoader xmxClassLoader = new XmxURLClassLoader(urls, getPlatformClassLoader());

					Class<?> xmxLoaderClass =
							Class.forName("com.gilecode.xmx.core.XmxLoader", true, xmxClassLoader);
					Method xmxCreateSvcMethod =
							xmxLoaderClass.getDeclaredMethod("createXmxService", Map.class, File.class);
					xmxService = (IXmxBootService) xmxCreateSvcMethod.invoke(xmxLoaderClass, overrideProperties, homeDir);

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

		// initialize AOP proxy
		if (xmxService != null) {
			XmxAopProxy.setAopService(xmxService.getAopService());
		}
		
		return xmxService != null;
	}

	private static ClassLoader getPlatformClassLoader() {
		int javaVersion = getMajorJavaVersion();
		if (javaVersion >= 9) {
			try {
				return (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader").invoke(null);
			} catch (Exception e) {
				logError("Failed to obtain the platform ClassLoader in Java " + javaVersion, e);
			}
		}
		return null;
	}

	private static int getMajorJavaVersion() {
		String[] parts = System.getProperty("java.version").split("[._]");
		int firstVer = Integer.parseInt(parts[0]);
		if (firstVer == 1 && parts.length > 1) {
			return Integer.parseInt(parts[1]);
		} else {
			return firstVer;
		}
	}

	@SafeVarargs
	private static URL[] makeJarsUrls(File parentDir, Collection<String>...jarNames) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		for (Collection<String> jarNameColl : jarNames) {
			for (String jarName : jarNameColl) {
				urls.add(new File(parentDir, jarName).toURI().toURL());
			}
		}
		return urls.toArray(new URL[0]);
	}

	private static String findLibraryJar(String[] filenames, String jarName) {
		if (filenames != null) {
			for (String fname : filenames) {
				if (fname.equalsIgnoreCase(jarName)) {
					return fname;
				}
			}
		}

		return null;
	}

	private static List<String> findLibraryJars(String[] libFileNames, String libNamePrefixLC, String versionSuffixLC) {
		if (libFileNames == null || libFileNames.length == 0) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		String suffix = (versionSuffixLC == null ? "" : versionSuffixLC) + ".jar";
		for (String name : libFileNames) {
			String nameLC = name.toLowerCase(Locale.ENGLISH);
			if (nameLC.startsWith(libNamePrefixLC) && nameLC.endsWith(suffix)) {
				result.add(name);
			}
		}
		return result;
	}

	public static byte[] transformClass(ClassLoader classLoader, String className,
			byte[] classBuffer, Class<?> classBeingRedefined) {
		if (xmxService != null) {
			return xmxService.transformClassIfInterested(classLoader, className, classBuffer, classBeingRedefined);
		} else {
			return classBuffer;
		}
	}

	@SuppressWarnings("unused")
	public static void registerObject(Object obj, int classId) {
		if (xmxService != null) {
			xmxService.registerObject(obj, classId);
		}
	}

	// TODO: do we need that IXmxSpringProxyAware iface? Or better just always call XmxProxy, e.g. using event?
	public static IXmxSpringProxyAware getSpringProxyRegistrator() {
		return new IXmxSpringProxyAware() {
			@Override
			public void registerProxy(Object target, Object proxy) {
				if (xmxService != null) {
					xmxService.registerProxyObject(target, proxy);
				}
			}
		};
	}
	
	private static String ERR_PREFIX = "[XMX ERROR!] ";
	private static void logError(String message) {
		System.err.println(ERR_PREFIX + message);
	}
	private static void logError(String message, Throwable e) {
		System.err.print(ERR_PREFIX + message + " :: ");
		e.printStackTrace(System.err);
	}

	public static IAdviceLogger getAdviceLogger(String name) {
		if (xmxService == null) {
			throw new IllegalStateException("Not expected with non-initialized XMX service");
		}
		return xmxService.getAdviceLogger(name);
	}

	public static void fireAdviceEvent(String pluginId, String eventName, Object arg) {
		if (xmxService == null) {
			throw new IllegalStateException("Not expected with non-initialized XMX service");
		}
		xmxService.fireAdviceEvent(pluginId, eventName, arg);
	}

	public static void fireAdviceEvent(String pluginId, String eventName, Object arg1, Object arg2) {
		if (xmxService == null) {
			throw new IllegalStateException("Not expected with non-initialized XMX service");
		}
		xmxService.fireAdviceEvent(pluginId, eventName, arg1, arg2);
	}

}

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
	private static Method xmxManagerGetAppNamesMethod;
	private static Method xmxManagerGetClassInfosMethod;
	private static Method xmxManagerGetObjectsMethod;
	private static Method xmxManagerGetDetailsMethod;
	private static Method xmxManagerSetFieldMethod;
	private static Method xmxManagerInvokeMethodMethod;
	
	static {
		ClassLoader parentClassLoader = XmxLoader.class.getClassLoader();
		// TODO better use JarInTheJar concept for xmx libs, but for now separate folder is fine
		URL xmxLibUrl = parentClassLoader.getResource("xmx");
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
					xmxManagerInstrumentMethod = xmxManagerClass.getDeclaredMethod("transformClass", 
							ClassLoader.class, String.class, byte[].class); 
					xmxManagerRegisterMethod = xmxManagerClass.getDeclaredMethod("registerObject", 
							Object.class); 
					xmxManagerGetAppNamesMethod = xmxManagerClass.getDeclaredMethod("getApplicationNames"); 
					xmxManagerGetClassInfosMethod = xmxManagerClass.getDeclaredMethod("findManagedClassInfos", 
							String.class, String.class);
					xmxManagerGetObjectsMethod = xmxManagerClass.getDeclaredMethod("getManagedObjects", 
							Integer.class);
					xmxManagerGetDetailsMethod = xmxManagerClass.getDeclaredMethod("getObjectDetails", 
							int.class);
					xmxManagerSetFieldMethod = xmxManagerClass.getDeclaredMethod("setObjectField", 
							int.class, int.class, String.class);
					xmxManagerInvokeMethodMethod = xmxManagerClass.getDeclaredMethod("invokeObjectMethod", 
							int.class, int.class, String[].class);
					
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
	
	private static final XmxService xmxServiceImpl = new XmxService() {
		
		@SuppressWarnings("unchecked")
		@Override
		public List<String> getApplicationNames() {
			try {
				return (List<String>) xmxManagerGetAppNamesMethod.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("getApplicationNames() failed", e);
				throw new XmxRuntimeException(e);
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public List<XmxClassInfo> findManagedClassInfos(String appNameOrNull,
				String classNamePatternOrNull) {
			try {
				return (List<XmxClassInfo>) xmxManagerGetClassInfosMethod.invoke(null, 
						appNameOrNull, classNamePatternOrNull);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("findManagedClassInfos() failed", e);
				throw new XmxRuntimeException(e);
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public List<XmxObjectInfo> getManagedObjects(Integer classId) {
			try {
				return (List<XmxObjectInfo>) xmxManagerGetObjectsMethod.invoke(null, 
						classId);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("getManagedObjects() failed", e);
				throw new XmxRuntimeException(e);
			}
		}

		@Override
		public XmxObjectDetails getObjectDetails(int objectId)
				throws XmxRuntimeException {
			try {
				return (XmxObjectDetails) xmxManagerGetDetailsMethod.invoke(null, 
						objectId);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("getObjectDetails() failed", e);
				throw new XmxRuntimeException(e);
			}
		}

		@Override
		public XmxObjectDetails setObjectField(int objectId, int fieldId,
				String newValue) throws XmxRuntimeException {
			try {
				return (XmxObjectDetails) xmxManagerSetFieldMethod.invoke(null, 
						objectId, fieldId, newValue);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("setObjectField() failed", e);
				throw new XmxRuntimeException(e);
			}
		}

		@Override
		public String invokeObjectMethod(int objectId, int methodId,
				String... args) throws XmxRuntimeException {
			try {
				return (String) xmxManagerInvokeMethodMethod.invoke(null, 
						objectId, methodId, args);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				logError("invokeObjectMethod() failed", e);
				throw new XmxRuntimeException(e);
			}
		}
		
	};
	
	/**
	 * Return singleton implementation of {@link XmxService}
	 */
	public static XmxService getService() {
		return xmxServiceImpl;
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

package am.xmx.agent;

import am.xmx.boot.XmxProxy;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class XmxClassTransformer implements ClassFileTransformer {
	
	private boolean disabled = false;

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		
		if (disabled) {
			return null;
		
		}
		
		if (loader == null) {
			// skip bootstrap classes (performance reasons only, may be changed in future)
			return null;
		}
		
		if (className == null) {
			// skip anonymous (lambda) classes
			return null;
		}
		
		if (className.startsWith("am/") && (className.startsWith("am/specr/") || className.startsWith("am/xmx/"))) {
			// skip XMX classes from management to prevent circular class loading
			return null;
		}

		String loaderClassName = loader.getClass().getName();
		if (loaderClassName.equals("sun.reflect.DelegatingClassLoader")) {
			// skip auxiliary classes loaded by sun/reflect/DelegatingClassLoader, helps reducing number of processed classloaders
			return null;
		}

		try {
			byte[] transformClass = XmxProxy.transformClass(loader, className, classfileBuffer, classBeingRedefined);
			return transformClass;
		} catch (Throwable e) {
			e.printStackTrace();
			disabled = true;
			return null;
		}
	}
}

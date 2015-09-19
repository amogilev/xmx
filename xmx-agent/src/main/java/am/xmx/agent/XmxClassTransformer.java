package am.xmx.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import am.xmx.loader.XmxLoader;

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
		
		try {
			byte[] transformClass = XmxLoader.transformClass(loader, className, classfileBuffer, classBeingRedefined);
			return transformClass;
		} catch (Throwable e) {
			e.printStackTrace();
			disabled = true;
			return null;
		}
	}
}

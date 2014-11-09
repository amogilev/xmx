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
		
		if (loader.getParent() == null) {
			// skip bootstrap classes, as XmxLoader is not defined there
			// TODO think if it need to be there 
			return null;
		}
		
		try {
			byte[] transformClass = XmxLoader.transformClass(loader, className, classfileBuffer);
			return transformClass;
		} catch (Throwable e) {
			e.printStackTrace();
			disabled = true;
			return null;
		}
	}
}

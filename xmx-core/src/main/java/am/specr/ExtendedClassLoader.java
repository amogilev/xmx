// Copyright © 2014-2017 Andrey Mogilev. All rights reserved.

package am.specr;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class loader which extends parent loader with all class <i>files</i> loadable
 * by an additional class loader, and used to forcibly (re-)load the given target class.
 * <p/>
 * It provides all classes of the parent loader (maybe except the target class), and, in addition,
 * all classes which files can be loaded as resource by additional class loader. Thus,
 * it allows to load and use classes with dependencies in both class loaders.
 * <p/>
 * VMs with Ahead-of-Time compilation which do not provide original .class files
 * are not supported.   
 * 
 * @author Andrey Mogilev
 */
public class ExtendedClassLoader extends ClassLoader {

	/**
	 * The 'additional' class loader used as the resource locator to jars for loading
	 * the target class and any additional classes not found in the parent loader.
	 */
	private final WeakReference<ClassLoader> additionalClassLoaderRef;

	/**
	 * The class to be (re-)loaded by this class loader, even if already loaded in parent
	 */
	private final String targetClassName;

	/**
	 * The known classes to ignore - always return these instances.
	 */
	private final Class<?>[] predefinedClasses;

	private Map<String, Class<?>> knownClasses = null; // lazily initiated


	public ExtendedClassLoader(ClassLoader parent, ClassLoader additionalClassLoader,
							   String targetClassName, Class<?>... predefinedClasses) {
		super(parent);
		this.additionalClassLoaderRef = new WeakReference<>(additionalClassLoader);
		this.targetClassName = targetClassName;
		this.predefinedClasses = predefinedClasses;
	}

	/**
	 * All pre-defined classes and their dependencies must be loaded by their
	 * initial class loader, but not by this one. I.e., if they are available only
	 * in additional loader, we have to delegate to it.
	 */
    private void ensureKnownClasses() {
    	if (knownClasses == null) {
    		knownClasses = new HashMap<String, Class<?>>(predefinedClasses.length);
    		for (Class<?> cl : predefinedClasses) {
    			// TODO check if need to traverse parents, declared fields and methods
    			//  current assumption is that the order of class loading covers it
    			knownClasses.put(cl.getName(), cl);
    		}
    		
    	}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    	if (targetClassName.equals(name)) {
    		// general super.loadClass() will return already loaded class if laoded by parent
			// in order to re-load it, force our findClass() if not loaded by this exact CL
			Class c = findLoadedClass(name);
    		if (c == null) {
    			c = findClass(name);
				if (resolve && c != null) {
					resolveClass(c);
				}
			}
			return c;
		} else {
			return super.loadClass(name, resolve);
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
    	if (!targetClassName.equals(name)) {
			// if class is found in parent class path, return it
			try {
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				// not available in parent class loader, try find in additional
			}
		}
		
		ensureKnownClasses();
		Class<?> cl = knownClasses.get(name); 
		if (cl != null) {
			return cl;
		}
		
		// even if class is loaded in additional class path, only get it as URL and load
		// by this class
		String classFileName = name.replace('.', '/') + ".class";
		ClassLoader additionalClassLoader = additionalClassLoaderRef.get();
		if (additionalClassLoader == null) {
			return null;
		}
		InputStream classStream = additionalClassLoader.getResourceAsStream(classFileName);
		if (classStream == null) {
			// not found
			return null;
		}
		
		// although there are many readFully implementations available, own is preferred because:
		// 1) better to avoid third=party dependencies
		// 2) in-line implementation is more efficient than anything that provide shrinked byte[] as result
		try {
			int initialEstimation = classStream.available();
			
			// we use "initialEstimation+1" buffer, as we MUST make at least two
			//  reads to make sure that input stream is fully read (the last call shall
			//  return EOF (-1) code). In the "fast" path, the last 1 byte will be empty,
			//  otherwise (backup path), we grow buffer twice and ignore further estimations
			int initialBufSize = Math.max(1024, initialEstimation + 1);
		    byte[] buf = new byte[initialBufSize];
		    
		    int pos = 0;
		    int total = 0;
		    while (true) {
		    	if (pos == buf.length) {
		    		// initial estimation exceeded, need to grow buffer, no more
		    		//  trust to stream estimations
		    		buf = grow(buf);
		    	}
		    	int read = classStream.read(buf, pos, buf.length - pos);
		    	if (read == -1) {
		    		// stream ended
		    		break;
		    	}
		    	total += read;
		    }
		    
		    // fully read
			return defineClass(name, buf, 0, total);
		} catch (IOException e) {
			// failed to read class file contents
			throw new IllegalStateException("Failed to read class file content", e);
		}
		
	}

	private byte[] grow(byte[] buf) {
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity < 0) {
            throw new OutOfMemoryError();
        }
        return Arrays.copyOf(buf, newCapacity);
    }
}

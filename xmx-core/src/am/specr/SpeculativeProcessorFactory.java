package am.specr;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Utility factory which manages speculative processors, i.e. dynamically loaded 
 * processor classes which depend on other optional classes, and are loaded and executed 
 * only when those dependencies present in the class loader of the processed object.
 * <p/>
 * This factory implementation is NOT THREAD-SAFE.  
 * 
 * @author Andrey Mogilev
 *
 * @param <P> base interface or abstract class of processors managed by this factory 
 */
public class SpeculativeProcessorFactory<P> {
	
	/**
	 * Base interface or abstract class of processors managed by this factory.
	 */
	private Class<P> abstractProcessorClass;
	
	/**
	 * All registered processors and their required classes.
	 */
	private Map<String, String[]> registeredProcesors = new HashMap<>();
	
	/**
	 * Cached processors for known class loaders. 
	 */
	private WeakHashMap<ClassLoader, List<P>> processorsByClassLoaderCache = new WeakHashMap<>();

	/**
	 * Creates the factory, which will manage the implementations of the
	 * given base processor class. 
	 *  
	 * @param abstractProcessorClass base interface or abstract class of processors 
	 * 		to be managed by this factory
	 */
	public SpeculativeProcessorFactory(Class<P> abstractProcessorClass) {
		this.abstractProcessorClass = abstractProcessorClass;
	}
	
	/**
	 * Registers the processor class name, which shall be used for processing objects
	 * only when all of the required classes are available in the class loader of the
	 * processed object. 
	 * <p/>
	 * <strong>The processor class MUST NOT be loaded by any public class loader anywhere
	 * except by the factory.</strong> In particular, the processor class name needs to be passed 
	 * as pure string literal, rather than as <code><strike>MyProcessor.class.getName()</strike></code>
	 * <p/>
	 * The processor class is supposed to be loaded by the class loader of processed 
	 * objects, with an addition class file loaded as resource by class loader of this factory,
	 * so only two cases are supported: 
	 * <ul>
	 *  <li> the processor class shall be available in the class loader of processed objects,
	 *  	e.g. added to the system class path; or
	 *  <li> the .class files shall be available as resources for the factory's class loader;
	 *  	in this case, no Ahead-of-Time VMs are supported.
	 * </ul>
	 * <p/>
	 * The processor class MUST be a child of the base processor class specified in the 
	 * factory's constructor. This is not checked during the registration, but may cause
	 * further {@link ClassCastException}. Also, it MUST have no-args public constructor.
	 *  
	 * @param processorClassName the name of the processor class to be registered, not yet
	 * 	loaded by any public class loader
	 * @param requiredClassesNames names of all dependency classes of the processor class
	 */
	public void registerProcessor(String processorClassName, String...requiredClassesNames) {
		String[] oldValue = registeredProcesors.get(processorClassName);
		if (oldValue != null && !Arrays.equals(oldValue, requiredClassesNames)) {
			throw new IllegalStateException("Speculative processor class \"" + processorClassName + 
					"\" is already registered with another required classes");
		}
		
		registeredProcesors.put(processorClassName, requiredClassesNames);
		processorsByClassLoaderCache.clear(); // invalidate cache
	}
	
	/**
	 * Returns instances of all processor classes which have all their dependencies
	 * available in the class loader of the specified object.
	 *  
	 * @param processedObject the object to be processed
	 * 
	 * @return all processors for the specified object
	 */
	public List<P> getProcessorsFor(Object processedObject) {
		return Collections.unmodifiableList(
				getProcessorWith(processedObject.getClass().getClassLoader()));
	}
	
	/**
	 * Same as {@link #getProcessorsFor()}, but returns only the first matched processor.
	 * 
	 * @param processedObject the object to be processed
	 * 
	 * @return the first processor for the specified object, or {@code null} if none registered
	 */
	public P getFirstProcessorFor(Object processedObject) {
		List<P> processors = getProcessorsFor(processedObject);
		if (processors.isEmpty()) {
			return null;
		} else {
			return processors.get(0);
		}
	}
	
	///// IMPLEMENTATION FOLLOWS ////
	
	protected List<P> getProcessorWith(ClassLoader classLoader) {
		List<P> cachedList = processorsByClassLoaderCache.get(classLoader);
		if (cachedList != null) {
			return cachedList;
		}
		
		// new class loader, or after invalidation
		List<P> processors = new ArrayList<>(2); // most common is 1 per loader
		
		for (Entry<String, String[]> entry : registeredProcesors.entrySet()) {
			String processorClassName = entry.getKey();
			String[] requiredClassNames = entry.getValue();
			
			if (availableIn(classLoader, requiredClassNames)) {
				
				// dependencies are available, try initialize the processor
				
				P processor = tryCreateProcessor(classLoader, processorClassName);
				if (processor != null) {
					processors.add(processor);
				}
			}
		}
		processorsByClassLoaderCache.put(classLoader, processors);
		
		return processors;
	}

	private P tryCreateProcessor(ClassLoader classLoader, String processorClassName) {
		Class<? extends P> processorClass;
		try {
			ClassLoader extendedClassLoader = new ExtendedClassLoader(classLoader, 
					getClass().getClassLoader(), abstractProcessorClass);
			processorClass = Class.forName(processorClassName, true, extendedClassLoader)
					.asSubclass(abstractProcessorClass);;
		} catch (ClassNotFoundException e) {
			System.err.println("Processor class is not available to either class loader of processed object"
					+ "  and class loader of the factory: class=" + processorClassName + 
					", beanLoader=" + classLoader + ", factoryLoader=" + getClass().getClassLoader());
			return null;
		} catch (ClassCastException e) {
			System.err.println("Processor class MUST be subclass of base processor class: "
					+ "class=" + processorClassName + ", baseClass=" + abstractProcessorClass);
			return null;
		} catch (Error e) {
			System.err.println("Failed to initialize processor class: "
					+ "class=" + processorClassName + ", loader=" + classLoader);
			e.printStackTrace();
			return null;
		}
		
		Constructor<? extends P> constructor;
		try {
			constructor = processorClass.getConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			System.err.println("Processor class MUST have a no-args public constructor: "
					+ "class=" + processorClassName + "\"");
			e.printStackTrace();
			return null;
		}
		
		try {
			P instance = constructor.newInstance();
			return instance;
		} catch (Exception e) {
			System.err.println("Failed to instantiated processor: "
					+ "class=" + processorClassName + "\"");
			e.printStackTrace();
			return null;
		}
	}

	private boolean availableIn(ClassLoader loader, String...classNames) {
		try {
			for (String className : classNames) {
				loader.loadClass(className);
			}
		} catch (ClassNotFoundException e) {
			return false;
		}
		
		return true;
	}

}

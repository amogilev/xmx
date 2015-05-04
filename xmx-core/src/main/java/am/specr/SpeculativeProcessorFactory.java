package am.specr;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	 * Description of the registered processors.
	 */
	private static class ProcessorInfo {
		
		/** The processor class name */
		String processorClassName;
		
		/** The names of all required classes to be searched in the target class loader */
		String[] requiredClassesNames;
		
		/** 
		 * Whether the target class loader may be one of the parents of the class loader
		 * of the processed object, or only that loader itself.
		 */
		boolean traverseParents;
		
		public ProcessorInfo(String processorClassName,
				String[] requiredClassesNames, boolean traverseParents) {
			this.processorClassName = processorClassName;
			this.requiredClassesNames = requiredClassesNames;
			this.traverseParents = traverseParents;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((processorClassName == null) ? 0 : processorClassName
							.hashCode());
			result = prime * result + Arrays.hashCode(requiredClassesNames);
			result = prime * result + (traverseParents ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ProcessorInfo other = (ProcessorInfo) obj;
			if (processorClassName == null) {
				if (other.processorClassName != null)
					return false;
			} else if (!processorClassName.equals(other.processorClassName))
				return false;
			if (!Arrays.equals(requiredClassesNames, other.requiredClassesNames))
				return false;
			if (traverseParents != other.traverseParents)
				return false;
			return true;
		}
	}
	
	/**
	 * Base interface or abstract class of processors managed by this factory.
	 */
	private Class<P> abstractProcessorClass;
	
	/**
	 * Information about all registered processors, mapped by the processor class name.
	 */
	private Map<String, ProcessorInfo> registeredProcesors = new HashMap<>();
	
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
		registerProcessor(false, processorClassName, requiredClassesNames);
	}
	
	/**
	 * Registers the processor class name, which shall be used for processing objects
	 * only when all of the required classes are available in the <i>target</i> class 
	 * loader, which may be the class loader of the processed object, or, if 
	 * <b>traverseParents</b> flag is set, its closest parent where the required
	 * classes are available.
	 * <p/>
	 * This flag is useful for class loaders which shadows some classes available in
	 * parents, like Jetty's WebAppClassLoader.
	 * <p/>
	 * <strong>The processor class MUST NOT be loaded by any public class loader anywhere
	 * except by the factory.</strong> In particular, the processor class name needs to be passed 
	 * as pure string literal, rather than as <code><strike>MyProcessor.class.getName()</strike></code>
	 * <p/>
	 * The processor class is supposed to be loaded by some class loader where all required classes,
	 * so only two cases are supported: 
	 * <ul>
	 *  <li> the processor class shall be available in the target class loader,
	 *  	e.g. added to the system class path; or
	 *  <li> the .class files shall be available as resources for the factory's class loader;
	 *  	then a new synthetic class loader is created with the target loader as a parent,
	 *      and the processor class file added as resource. <b>NOTE</b>In this case, no 
	 *      Ahead-of-Time VMs are supported.
	 * </ul>
	 * <p/>
	 * The processor class MUST be a child of the base processor class specified in the 
	 * factory's constructor. This is not checked during the registration, but may cause
	 * further {@link ClassCastException}. Also, it MUST have no-args public constructor.
	 *  
	 * @param processorClassName the name of the processor class to be registered, not yet
	 * 	loaded by any public class loader
	 * @param requiredClassesNames names of all dependency classes of the processor class
	 * @param traverseParents if {@code true}, not only the processed object's class loader,
	 *    but also its parents may be the target class loader
	 */
	public void registerProcessor(boolean traverseParents, String processorClassName, String...requiredClassesNames) {
		ProcessorInfo pi = new ProcessorInfo(processorClassName, requiredClassesNames, traverseParents);
		ProcessorInfo oldValue = registeredProcesors.get(processorClassName);
		if (oldValue == null) {
			registeredProcesors.put(processorClassName, pi);
			processorsByClassLoaderCache.clear(); // invalidate cache
		} else if (oldValue.equals(pi)) {
			throw new IllegalStateException("Speculative processor class \"" + processorClassName + 
					"\" is already registered with another properties");
		}
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
		ClassLoader loader = processedObject instanceof ClassLoader ? (ClassLoader)processedObject : 
			processedObject.getClass().getClassLoader();
		return Collections.unmodifiableList(getProcessorsWith(loader));
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
	
	private List<P> getProcessorsWith(ClassLoader classLoader) {
		List<P> cachedList = processorsByClassLoaderCache.get(classLoader);
		if (cachedList != null) {
			return cachedList;
		}
		
		// new class loader, or after invalidation
		List<P> processors = new ArrayList<>(2); // most common is 1 per loader
		
		for (ProcessorInfo pi : registeredProcesors.values()) {
			
			ClassLoader targetCL = findTargetClassLoader(classLoader, pi.traverseParents, pi.requiredClassesNames);
			if (targetCL != null) {
				
				// dependencies are available, try initialize the processor
				
				P processor = tryCreateProcessor(targetCL, pi.processorClassName);
				if (processor != null) {
					processors.add(processor);
				}
			}
		}
		
		// always use initial class loader (of processed object) as a key, as targets may
		// be different (if there are several processors)
		processorsByClassLoaderCache.put(classLoader, processors);
		
		return processors;
	}

	/**
	 * Finds the target class loader, i.e. the class loader where all the required classes
	 * are available.
	 * <p/>
	 * If traverseParents is {@code false}, the only the class loader of the processed objects
	 * is checked. Otherwise, all its parents are checked as well.
	 *  
	 * @param processedObjectClassLoader the class loader to start the check
	 * @param traverseParents whether to check the parents of the starting class loader
	 * @param requiredClassesNames the name of the classes which shall be available in the
	 * 		target class loader
	 * 
	 * @return the found target class loader, or {@code null}
	 */
	private ClassLoader findTargetClassLoader(ClassLoader processedObjectClassLoader,
			boolean traverseParents, String[] requiredClassesNames) {
		
		ClassLoader targetCL = processedObjectClassLoader;
		while (targetCL != null) {
			if (availableIn(targetCL, requiredClassesNames)) {
				// found
				return targetCL;
			}
			if (traverseParents) {
				targetCL = targetCL.getParent();
			} else {
				break;
			}
		}
		return null;
	}

	private P tryCreateProcessor(ClassLoader targetClassLoader, String processorClassName) {
		Class<? extends P> processorClass;
		try {
			ClassLoader extendedClassLoader = new ExtendedClassLoader(targetClassLoader, 
					getClass().getClassLoader(), abstractProcessorClass);
			processorClass = Class.forName(processorClassName, true, extendedClassLoader)
					.asSubclass(abstractProcessorClass);
		} catch (ClassNotFoundException e) {
			System.err.println("Processor class is not available to either class loader of processed object"
					+ "  and class loader of the factory: class=" + processorClassName + 
					", targetLoader=" + targetClassLoader + ", factoryLoader=" + getClass().getClassLoader());
			return null;
		} catch (ClassCastException e) {
			System.err.println("Processor class MUST be subclass of base processor class: "
					+ "class=" + processorClassName + ", baseClass=" + abstractProcessorClass);
			return null;
		} catch (Error e) {
			System.err.println("Failed to initialize processor class: "
					+ "class=" + processorClassName + ", loader=" + targetClassLoader);
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
		for (String className : classNames) {
			try {
				loader.loadClass(className);
			} catch (ClassNotFoundException e) {
				return false;
			}
		}
		
		return true;
	}

}

package am.xmx.core;


/**
 * Abstract extractor of application names by bean objects and their class loaders.
 * <p/>
 * The implementations are specific for application servers and are selected according 
 * to the available classes. 
 */
public interface IWebappNameExtractor {
	
	/**
	 * Tries to obtain the app name for the bean object. Shall 
	 * return {@code null} if app cannot be determined.
	 * 
	 * @param beanLoader the class loader of the bean object
	 */
	String extract(ClassLoader beanLoader);
}

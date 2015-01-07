package am.xmx.core;


/**
 * Abstract extractor of web application name of corresponding bean objects.
 * <p/>
 * The implementations are specific for application servers, 
 * are selected according to the available classes, and generally use the 
 * class loaders for the object class. 
 */
public interface IWebappNameExtractor {
	
	/**
	 * Tries to obtain the webapp name for the bean object. Shall 
	 * return {@code null} if app cannot be determined.
	 * 
	 * @param obj the bean object
	 */
	String extract(Object obj);
}

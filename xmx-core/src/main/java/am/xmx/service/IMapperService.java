package am.xmx.service;

/**
 * Services for mapping to and from JSON and other text representations.
 */
public interface IMapperService {

	/**
	 * Returns JSON representation of an object. The call is safe in that it shall never throw exceptions.
	 */
	String safeToJson(Object obj);

	/**
	 * Returns toString() representation of an object. The call is safe in that it shall never throw exceptions.
	 */
	String safeToString(Object obj);
}

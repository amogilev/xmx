// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

/**
 * Services for mapping to and from JSON and other text representations.
 */
public interface IMapperService {

	String LIMIT_EXCEEDED_SUFFIX = "...";
	String NOT_AVAILABLE = "N/A";

	/**
	 * Returns JSON representation of an object. The call is safe in that it shall never throw exceptions.
	 */
	String safeToJson(Object obj);

	/**
	 * Return JSON implementation, limited to the specified number of characters. If exceeded, a
	 * {@link #LIMIT_EXCEEDED_SUFFIX} is added to the end of the limited resulting string.
	 *
	 * @param obj the object to serialize to JSON
	 * @param cLimit if greater than 0, the limit in characters for the JSON representation
	 *
	 */
	String safeToJson(Object obj, long cLimit);

	/**
	 * Returns toString() representation of an object. The call is safe in that it shall never throw exceptions.
	 */
	String safeToString(Object obj);
}

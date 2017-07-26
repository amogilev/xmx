package am.xmx.dto;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Detailed information about all managed fields and methods of a managed object.
 *
 * @author Andrey Mogilev
 */
public class XmxObjectDetails extends XmxObjectInfo {

	/**
	 * Lists of fields for object and its parents, mapped by the class name.
	 */
	final private Map<Integer, Field> managedFields;
	
	/**
	 * Lists of methods for object and its parents.
	 */
	final private Map<Integer, Method> managedMethods;

	public XmxObjectDetails(int objectId, XmxClassInfo classInfo, Object value,
							Map<Integer, Field> managedFields,
							Map<Integer, Method> managedMethods) {
		super(objectId, classInfo, value);
		this.managedFields = managedFields;
		this.managedMethods = managedMethods;
	}

	public Map<Integer, Field> getManagedFields() {
		return managedFields;
	}

	public Map<Integer, Method> getManagedMethods() {
		return managedMethods;
	}
}

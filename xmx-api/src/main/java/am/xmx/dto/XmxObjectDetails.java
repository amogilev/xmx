package am.xmx.dto;

import java.util.List;
import java.util.Map;

/**
 * Detailed information about all superclasses, all fields
 * (with values) and all methods of the managed object, including 
 * inherited ones.
 */
public class XmxObjectDetails {
	
	/**
	 * Information about a single field.
	 */
	public static class FieldInfo {
		
		/**
		 * Field ID, unique within the managed object. 
		 */
		private int id;
		
		/**
		 * Field name.
		 */
		private String name;
		
		/**
		 * Field value in string representation.
		 */
		private String value;

		public FieldInfo(int id, String name, String value) {
			super();
			this.id = id;
			this.name = name;
			this.value = value;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}
	
	/**
	 * Information about a single method.
	 */
	public static class MethodInfo {
		
		/**
		 * Method ID, unique within the managed object. 
		 */
		private int id;
		
		/**
		 * Simple method name. Several methods with same name may exist.
		 */
		private String name;
		
		/**
		 * Method signature.
		 */
		private String signature;

		public MethodInfo(int id, String name, String signature) {
			super();
			this.id = id;
			this.name = name;
			this.signature = signature;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getSignature() {
			return signature;
		}
	}

	/**
	 * toString() value of the object
	 */
	final private String toStringValue;

	/**
	 * JSON representation of the object
	 */
	final private String jsonValue;
	
	
	/**
	 * Names of the object class and all its superclasses,
	 * in the reverse order of inheritance. The object's class
	 * is the first in the list, then it's superclass, then the
	 * next superclass etc.
	 * <p/>
	 * These names are used as keys to other Maps.
	 */
	final private List<String> classesNames;
	
	/**
	 * Lists of fields for object and its parents, mapped by the class name.
	 */
	final private Map<String, List<FieldInfo>> fieldsByClass;
	
	/**
	 * Lists of methods for object and its parents.
	 */
	final private Map<String, List<MethodInfo>> methodsByClass;

	public XmxObjectDetails(String toStringValue, String jsonValue, List<String> classesNames,
							Map<String, List<FieldInfo>> fieldsByClass,
							Map<String, List<MethodInfo>> methodsByClass) {
		super();
		this.toStringValue = toStringValue;
		this.jsonValue = jsonValue;
		this.classesNames = classesNames;
		this.fieldsByClass = fieldsByClass;
		this.methodsByClass = methodsByClass;
	}

	public String getToStringValue() {
		return toStringValue;
	}

	public String getJsonValue() {
		return jsonValue;
	}

	public List<String> getClassesNames() {
		return classesNames;
	}

	public Map<String, List<FieldInfo>> getFieldsByClass() {
		return fieldsByClass;
	}

	public Map<String, List<MethodInfo>> getMethodsByClass() {
		return methodsByClass;
	}
}

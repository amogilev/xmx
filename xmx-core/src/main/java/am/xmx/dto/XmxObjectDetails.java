package am.xmx.dto;

import java.util.List;
import java.util.Map;

/**
 * Detailed information about all superclasses, all fields
 * (with values) and all methods of the managed object, including 
 * inherited ones.
 */
public class XmxObjectDetails extends XmxObjectInfo {
	
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
		 * Method signature, return type and name. Really is a part of signature before ()
		 */
		private String nameTypeSignature;

		private String[] parameters;

		public MethodInfo(int id, String name, String nameTypeSignature, String[] parameters) {
			super();
			this.id = id;
			this.name = name;
			this.nameTypeSignature = nameTypeSignature;
			this.parameters = parameters;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getNameTypeSignature() {
			return nameTypeSignature;
		}

		public String[] getParameters() {
			return parameters;
		}
	}

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

	public XmxObjectDetails(int objectId, XmxClassInfo classInfo, Object value,
							List<String> classesNames,
							Map<String, List<FieldInfo>> fieldsByClass,
							Map<String, List<MethodInfo>> methodsByClass) {
		super(objectId, classInfo, value);
		this.classesNames = classesNames;
		this.fieldsByClass = fieldsByClass;
		this.methodsByClass = methodsByClass;
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

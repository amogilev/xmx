// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import com.gilecode.xmx.dto.XmxClassInfo;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static com.gilecode.xmx.ui.UIConstants.ARRAY_PAGE_LENGTH;

public class ExtendedXmxObjectDetails extends ExtendedXmxObjectInfo {
	/**
	 * Information about a single field.
	 */
	public static class FieldInfo {

		/**
		 * Field ID, unique within the managed object.
		 */
		private final String id;

		/**
		 * Field name.
		 */
		private final String name;

		/**
		 * Field modifiers
		 */
		private final int modifiers;

		/**
		 * The text representation of the field value.
		 */
		private final XmxObjectTextRepresentation text;

		public FieldInfo(String fid, String name, int modifiers, XmxObjectTextRepresentation text) {
			super();
			this.id = fid;
			this.name = name;
			this.modifiers = modifiers;
			this.text = text;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public XmxObjectTextRepresentation getText() {
			return text;
		}

		public boolean isStaticField() {
			return (modifiers & Modifier.STATIC) != 0;
		}
	}

	/**
	 * Information about a single method.
	 */
	public static class MethodInfo {

		/**
		 * Method ID, unique within the managed object.
		 */
		private final String id;

		/**
		 * Simple method name. Several methods with same name may exist.
		 */
		private final String name;

		/**
		 * Method signature, return type and name. Really is a part of signature before ()
		 */
		private final String nameTypeSignature;

		private final String[] parameters;

		/**
		 * Method modifiers
		 */
		private final int modifiers;

		public MethodInfo(String id, String name, String nameTypeSignature, String[] parameters,
						  int modifiers) {
			super();
			this.id = id;
			this.name = name;
			this.nameTypeSignature = nameTypeSignature;
			this.parameters = parameters;
			this.modifiers = modifiers;
		}

		public String getId() {
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

		public boolean isStaticMethod() {
			return (modifiers & Modifier.STATIC) != 0;
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

	/**
	 * The current page of the array, only valid if this object is an array.
	 */
	final private ArrayPageDetails arrayPage;

	public static class ArrayPageDetails {

		/**
		 * The array length
		 */
		final private int arrLength;

		/**
		 * The page number, starting from 0.
		 */
		final private int pageNum;

		/**
		 * The current page of array elements
		 */
		final private XmxObjectTextRepresentation[] pageElements;

		public ArrayPageDetails(int arrLength, int pageNum, XmxObjectTextRepresentation[] pageElements) {
			this.arrLength = arrLength;
			this.pageNum = pageNum;
			this.pageElements = pageElements;
		}

		public int getArrLength() {
			return arrLength;
		}

		public int getPageNum() {
			return pageNum;
		}

		public int getTotalPages() {
			return 1 + (arrLength - 1) / ARRAY_PAGE_LENGTH;
		}

		public int getPageStart() {
			return pageNum * ARRAY_PAGE_LENGTH;
		}

		public XmxObjectTextRepresentation[] getPageElements() {
			return pageElements;
		}
	}


	public ExtendedXmxObjectDetails(int objectId, XmxClassInfo classInfo, Object value,
									XmxObjectTextRepresentation text,
									List<String> classesNames,
									Map<String, List<FieldInfo>> fieldsByClass,
									Map<String, List<MethodInfo>> methodsByClass,
									ArrayPageDetails arrayPage) {
		super(objectId, classInfo, value, text);
		this.classesNames = classesNames;
		this.fieldsByClass = fieldsByClass;
		this.methodsByClass = methodsByClass;
		this.arrayPage = arrayPage;
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

	public boolean isArray() {
		return arrayPage != null;
	}

	public ArrayPageDetails getArrayPage() {
		return arrayPage;
	}
}


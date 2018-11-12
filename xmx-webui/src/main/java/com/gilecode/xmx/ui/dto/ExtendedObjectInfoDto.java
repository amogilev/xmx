// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.ui.dto;

import java.util.List;
import java.util.Map;

import static com.gilecode.xmx.ui.UIConstants.ARRAY_PAGE_LENGTH;

/**
 * A detailed object information. The basic information is extended with fields, methods and array details.
 */
public class ExtendedObjectInfoDto extends ObjectInfoDto {

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
	final private Map<String, List<XmxFieldInfo>> fieldsByClass;

	/**
	 * Lists of methods for object and its parents.
	 */
	final private Map<String, List<XmxMethodInfo>> methodsByClass;

	/**
	 * The current page of the array, only valid if this object is an array.
	 */
	final private ArrayPageDetails arrayPage;

	/**
	 * The permanent path to an object which can be applied while the root reference object is singleton,
	 * or {@code null} if not available.
	 */
	final private String permaRefPath;

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


	public ExtendedObjectInfoDto(int objectId, ClassInfoDto classInfo,
			XmxObjectTextRepresentation text,
			List<String> classesNames,
			Map<String, List<XmxFieldInfo>> fieldsByClass,
			Map<String, List<XmxMethodInfo>> methodsByClass,
			ArrayPageDetails arrayPage, String permaRefPath, String proxyClass) {
		super(objectId, classInfo, text, proxyClass);
		this.classesNames = classesNames;
		this.fieldsByClass = fieldsByClass;
		this.methodsByClass = methodsByClass;
		this.arrayPage = arrayPage;
		this.permaRefPath = permaRefPath;
	}

	public List<String> getClassesNames() {
		return classesNames;
	}

	public Map<String, List<XmxFieldInfo>> getFieldsByClass() {
		return fieldsByClass;
	}

	public Map<String, List<XmxMethodInfo>> getMethodsByClass() {
		return methodsByClass;
	}

	public boolean isArray() {
		return arrayPage != null;
	}

	public ArrayPageDetails getArrayPage() {
		return arrayPage;
	}

	public String getPermaRefPath() {
		return permaRefPath;
	}
}


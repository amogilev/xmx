// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.model;

import java.util.Collections;
import java.util.List;

public class NotSingletonException extends Exception {

	public enum Reason {
		MISSING_APP, MULTIPLE_CLASSES, MULTIPLE_OBJECTS, NO_OBJECTS
	}

	private final SingletonPermanentId permanentId;
	private final Reason reason;
	private final List<Integer> objectInfos;

	public NotSingletonException(SingletonPermanentId permanentId, Reason reason) {
		this(permanentId, reason, Collections.<Integer>emptyList());
	}

	public NotSingletonException(SingletonPermanentId permanentId, Reason reason, List<Integer> objectIds) {
		this.permanentId = permanentId;
		this.reason = reason;
		this.objectInfos = objectIds;
	}

	public static NotSingletonException of(SingletonPermanentId permanentId, boolean multipleClasses, List<Integer> objectIds) {
		Reason reason = multipleClasses ? Reason.MULTIPLE_CLASSES :
				(objectIds.size() == 0 ? Reason.NO_OBJECTS : Reason.MULTIPLE_OBJECTS);
		return new NotSingletonException(permanentId, reason, objectIds);
	}

	public Reason getReason() {
		return reason;
	}

	public List<Integer> getObjectInfos() {
		return objectInfos;
	}

	public SingletonPermanentId getPermanentId() {
		return permanentId;
	}
}

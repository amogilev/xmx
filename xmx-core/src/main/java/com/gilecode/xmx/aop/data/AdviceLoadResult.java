// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.aop.data;

import java.util.Collections;
import java.util.Map;

public class AdviceLoadResult {

	/**
	 * Loaded advice classes by "jar:class" advice description.
	 *
	 */
	private final Map<String, AdviceClassInfo> adviceClassesByDesc;

	public AdviceLoadResult(Map<String, AdviceClassInfo> adviceClassesByDesc) {
		this.adviceClassesByDesc = adviceClassesByDesc;
	}

	public static AdviceLoadResult empty() {
		return new AdviceLoadResult(Collections.<String, AdviceClassInfo>emptyMap());
	}

	public Map<String, AdviceClassInfo> getAdviceClassesByDesc() {
		return adviceClassesByDesc;
	}

	public boolean isEmpty() {
		return adviceClassesByDesc.isEmpty();
	}
}

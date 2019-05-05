// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.pattern.impl;

import com.gilecode.xmx.cfg.pattern.ITypeMatcher;
import com.gilecode.xmx.cfg.pattern.TypeSpec;
import org.objectweb.asm.Type;

/**
 * Generic implementation of the type matcher.
 */
public class TypeMatcher implements ITypeMatcher {

    private final boolean fullyQualified;
    private final String name;
    private final int arrayLevel;

    public TypeMatcher(boolean fullyQualified, String name, int arrayLevel) {
        this.fullyQualified = fullyQualified;
        this.name = name;
        this.arrayLevel = arrayLevel;
    }

	@Override
	public boolean matches(TypeSpec typeSpec) {
		if (typeSpec instanceof ClassTypeSpec) {
			return matchesClass(((ClassTypeSpec) typeSpec).getType());
		} else if (typeSpec instanceof DescriptorTypeSpec) {
			return matchesDescriptor(((DescriptorTypeSpec) typeSpec).getDescriptor());
		} else if (typeSpec == null) {
			return false;
		} else {
			throw new IllegalArgumentException("Unknown TypeSpec class:" + typeSpec.getClass());
		}
	}

    private boolean matchesClass(Class<?> type) {
        for (int i = arrayLevel; i > 0; i--) {
            if (type.isArray()) {
                type = type.getComponentType();
            } else {
                return false;
            }
        }
        if (type.isArray()) {
        	return false;
        }

        // NOTE: do not use getSimpleName() here, as it strips 'Outer$' along with package
        String typeName = type.getName();
	    if (!fullyQualified) {
		    typeName = getTypeNameWithoutPackage(typeName);
	    }

	    return name.equals(typeName);
    }

	private boolean matchesDescriptor(String typeDesc) {
	    Type t = Type.getType(typeDesc);

	    if (t.getSort() == Type.ARRAY) {
	    	if (arrayLevel != t.getDimensions()) {
			    return false;
		    }
		    t = t.getElementType();
	    } else if (arrayLevel != 0) {
		    return false;
	    }

	    String typeName = t.getClassName();
	    if (!fullyQualified) {
		    typeName = getTypeNameWithoutPackage(typeName);
	    }

	    if (name.equals(typeName)) {
	    	return true;
		}
	    return matchesAnyNestedClassName(name, typeName);
    }

	private boolean matchesAnyNestedClassName(String name, String typeName) {
		int start = 0;
    	while (true) {
			int i = typeName.indexOf('$', start);
			if (i < 0) {
				return false;
			} else if (typeName.regionMatches(i + 1, name, 0, name.length())) {
				return true;
			}
			start = i + 1;
		}
	}

	private String getTypeNameWithoutPackage(String typeName) {
		// NOTE: works even if there is no dot, as (1 + -1 == 0)
		typeName = typeName.substring(1 + typeName.lastIndexOf('.'));
		return typeName;
	}
}

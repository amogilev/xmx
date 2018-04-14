// Copyright © 2018 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.service;

import java.lang.reflect.Method;

public interface ISignatureService {

	String getTypeSignature(Class<?> c);

	Class<?> findTypeBySignature(ClassLoader cl, String typeSignature) throws ClassNotFoundException;

	String getMethodSignature(Method m);

	Method findMethodBySignature(ClassLoader cl, String methodSignature) throws ClassNotFoundException, NoSuchMethodException;

}

package am.xmx.core;

import am.xmx.service.IXmxServiceEx;

public interface IXmxCoreService extends IXmxServiceEx {

	ManagedClassInfo getManagedClassInfo(Class<?> clazz);

}

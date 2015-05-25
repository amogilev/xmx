package am.xmx.core;

import am.xmx.dto.XmxClassInfo;
import am.xmx.service.IXmxServiceEx;

public interface IXmxCoreService extends IXmxServiceEx {

	XmxClassInfo getManagedClassInfo(Class<?> clazz);

}

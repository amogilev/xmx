package am.xmx.core.jmx;

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

import am.xmx.cfg.IXmxPropertiesSource;
import am.xmx.dto.XmxClassInfo;

public class JmxSupport {
	
	public static ModelMBeanInfo createModelForClass(Class<?> objClass, String appName, IXmxPropertiesSource config) {
		// TODO: check if JMX is enabled for class
		// TODO: check JMX visibility level, and separate fields/methods
		
		try {
			ArrayList<ModelMBeanOperationInfo> operations = new ArrayList<>();
			for (Method m : objClass.getMethods()) {
				// TODO: this is for public only! Test with private too (getDeclaredMethods())
				// TODO: remove getters/setters (put to attributes instead?)
				Descriptor descr = new DescriptorSupport();
				descr.setField("class", objClass.getName());
				descr.setField("name", m.getName());
				descr.setField("descriptorType", "operation");
				operations.add(new ModelMBeanOperationInfo(m.toGenericString(), m, descr)); 
			}
	        ModelMBeanAttributeInfo[] attributes = {}; // TODO: fields
			ModelMBeanConstructorInfo[] constructors = {};
			ModelMBeanNotificationInfo[] notifications = {};
			
			ModelMBeanInfo mbi = new ModelMBeanInfoSupport(objClass.getName(), objClass.getName(), 
	        		attributes, constructors, 
	        		operations.toArray(new ModelMBeanOperationInfo[operations.size()]), 
	        		notifications);
			
			return mbi;
			
		} catch (Exception e) {
			System.err.println("Failed to create JMX model for " + objClass.getName());
			e.printStackTrace();
			return null;
		}
	}
	
	public static String createClassObjectNamePart(Class<?> objClass, String appName) {
		String type = objClass.getSimpleName();
		String packageName = getPackageName(objClass);
		
		StringBuilder sb = new StringBuilder(64);
		sb.append("XMX:");
		
		if (appName != null && !appName.isEmpty()) {
			sb.append("app=").append(appName).append(',');
		}
		sb.append("type=").append(type);
		sb.append(",package=").append(packageName);
		return sb.toString();
	}
	

	private static String getPackageName(Class<?> objClass) {
		Package p = objClass.getPackage();
		if (p != null) {
			return p.getName();
		} else {
			// not correct for local classes or arrays, but fine for our aims...
			String className = objClass.getName();
			int lastDotIdx = className.lastIndexOf('.');
			if (lastDotIdx > 0) {
				return className.substring(0, lastDotIdx);
			} else {
				return "";
			}
		}
	}

	public static ObjectName registerBean(MBeanServer jmxServer, int objectId,
			Object obj, XmxClassInfo classInfo) {
		
		if (classInfo.getJmxClassModel() == null || classInfo.getJmxObjectNamePart() == null) {
			return null;
		}
		
		try {
			// TODO: maybe skip id for singletons
			ObjectName objectName = new ObjectName(classInfo.getJmxObjectNamePart() + ",id=" + objectId);
			
			// TODO: use custom ModelMBean to (1) set context CL and (2) support WeakRefs
			RequiredModelMBean bean = new RequiredModelMBean(classInfo.getJmxClassModel());
			bean.setManagedResource(obj, "ObjectReference");
			
			// FIXME: somehow provide WeakRef? otherwise objects are not GC'ed
			
			jmxServer.registerMBean(bean, objectName);
			
			return objectName;
		} catch (Exception e) {
			System.err.println("Failed to register object as JMX bean; class=" + obj.getClass().getName());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static void unregisterBean(MBeanServer jmxServer,
			ObjectName jmxObjectName) {
		
		try {
			jmxServer.unregisterMBean(jmxObjectName);
		} catch (MBeanRegistrationException | InstanceNotFoundException e) {
			System.err.println("Failed to unregister bean from JMX: " + jmxObjectName);
			e.printStackTrace(System.err);
		}
	}

}

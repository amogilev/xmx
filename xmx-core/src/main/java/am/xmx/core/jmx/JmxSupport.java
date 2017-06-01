package am.xmx.core.jmx;

import am.xmx.cfg.IXmxPropertiesSource;
import am.xmx.core.ManagedClassInfo;
import am.xmx.service.IXmxService;

import javax.management.*;
import javax.management.modelmbean.*;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JmxSupport {

	public static ModelMBeanInfoSupport createModelForClass(Class<?> objClass, String appName, 
			List<Method> managedMethods, List<Field> managedFields,
			IXmxPropertiesSource config) {

		// TODO: check if JMX is enabled for class
		// TODO: check JMX visibility level, and separate fields/methods

		try {
			ArrayList<ModelMBeanOperationInfo> operations = new ArrayList<>();
			for (int methodId = 0; methodId < managedMethods.size(); methodId++) {
				Method m = managedMethods.get(methodId);

				// TODO: remove getters/setters (put to attributes instead?)
				Descriptor descr = new DescriptorSupport();
				descr.setField("class", objClass.getName());
				descr.setField("name", m.getName());
				descr.setField("descriptorType", "operation");
				descr.setField("methodId", methodId);

				Class<?>[] paramClasses = m.getParameterTypes();
				final MBeanParameterInfo[] signature =
						new MBeanParameterInfo[paramClasses.length];

				for (int i = 0; i < paramClasses.length; i++) {
					// TODO: try extract parameter names (debug info during load, or Java8, see Spring's ParameterNameDiscoverer)
					final String pn = "p" + (i + 1);
					String type = getJmxType(paramClasses[i]);
					signature[i] = new MBeanParameterInfo(pn, type, "", null);
				}

				ModelMBeanOperationInfo opInfo = new ModelMBeanOperationInfo(
						m.getName(), 
						m.toGenericString(), // description
						signature, 
						getJmxType(m.getReturnType()), 
						MBeanOperationInfo.UNKNOWN, // impact 
						descr);

				operations.add(opInfo); 
			}

			ArrayList<ModelMBeanAttributeInfo> attributes = new ArrayList<>();
			for (int fieldId = 0; fieldId < managedFields.size(); fieldId++) {
				Field f = managedFields.get(fieldId);

				Descriptor descr = new DescriptorSupport();
				descr.setField("class", objClass.getName());
				descr.setField("name", f.getName());
				descr.setField("descriptorType", "attribute");
				descr.setField("fieldId", fieldId);

				String type = getJmxType(f.getType());

				ModelMBeanAttributeInfo attrInfo = new ModelMBeanAttributeInfo(
						f.getName(), 
						type, 
						f.toGenericString(), // description 
						true, true, //isReadable, isWritable, 
						false, //isIs
						descr
						);

				attributes.add(attrInfo); 
			}


			ModelMBeanConstructorInfo[] constructors = {};
			ModelMBeanNotificationInfo[] notifications = {};

			ModelMBeanInfoSupport mbi = new ModelMBeanInfoSupport(objClass.getName(), objClass.getName(), 
					attributes.toArray(new ModelMBeanAttributeInfo[attributes.size()]),
					constructors, 
					operations.toArray(new ModelMBeanOperationInfo[operations.size()]), 
					notifications);

			return mbi;

		} catch (Exception e) {
			System.err.println("Failed to create JMX model for " + objClass.getName());
			e.printStackTrace();
			return null;
		}
	}

	private static String getJmxType(Class<?> clazz) {
		// non-standard classes are missing in remote JMX agents, so have to use CompositeData instead
		ClassLoader cl = clazz.getClassLoader();
		if (cl == null || cl.equals(ClassLoader.getSystemClassLoader())) {
			return clazz.getName();
		} else {
			return CompositeDataSupport.class.getName();
		}
	}
	
	private static ConcurrentMap<List<String>, AtomicInteger> versionByAppAndClassNames = new ConcurrentHashMap<>(); 

	public static String createClassObjectNamePart(String className, String appName) {
		String[] packageAndSimpleName = getPackageAndSimpleName(className);
		String packageName = packageAndSimpleName[0];
		String type = packageAndSimpleName[1];

		StringBuilder sb = new StringBuilder(64);
		if (appName != null && !appName.isEmpty()) {
			sb.append("XMX_").append(appName).append(':');
		} else {
			sb.append("XMX:");
		}
		
		List<String> appAndClassNames = new ArrayList<>(2);
		appAndClassNames.add(appName);
		appAndClassNames.add(className);
		versionByAppAndClassNames.putIfAbsent(appAndClassNames, new AtomicInteger());
		AtomicInteger versionCounter = versionByAppAndClassNames.get(appAndClassNames);
		int version = versionCounter.incrementAndGet();
		
		// type includes simple name class (w/o package), then the package and optional version
		// the version is added to prevent non-unique ObjectNames in case of web app reloads or
		// loading of class with the same name by different class loaders
		sb.append("type=").append(type);
		sb.append(" (").append(packageName);
		if (version > 1) {
			sb.append(" | v").append(version);
		}
		sb.append(")");
		
		
		return sb.toString();
	}

	private static String[] getPackageAndSimpleName(String className) {
		String packageName, simpleName;
		// not correct for arrays, but fine for our aims...
		int lastDotIdx = className.lastIndexOf('.');
		if (lastDotIdx > 0) {
			packageName = className.substring(0, lastDotIdx);
			simpleName = className.substring(lastDotIdx + 1);
		} else {
			packageName = "";
			simpleName = className;
		}
		
		return new String[]{packageName, simpleName};
	}

	public static ObjectName registerBean(IXmxService xmxService, MBeanServer jmxServer, int objectId,
										  ManagedClassInfo classInfo, boolean singleton) {

		if (classInfo.getJmxClassModel() == null || classInfo.getJmxObjectNamePart() == null) {
			return null;
		}

		try {
			ObjectName objectName = makeObjectName(objectId, classInfo, singleton);

			JmxBridgeModelBean bean = new JmxBridgeModelBean(objectId, classInfo.getJmxClassModel(), xmxService);
			jmxServer.registerMBean(bean, objectName);
			
			return objectName;
		} catch (Exception e) {
			System.err.println("Failed to register object as JMX bean; class=" + classInfo.getClassName());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static ObjectName makeObjectName(int objectId, ManagedClassInfo classInfo, boolean singleton) {
		try {
			// add id for non-singletons
			ObjectName objectName = new ObjectName(classInfo.getJmxObjectNamePart() + 
					(singleton ?  "" : ",id=" + objectId));
			return objectName;
		} catch (MalformedObjectNameException e) {
			System.err.println("Unexpected: Failed to create ObjectName; part=" + classInfo.getJmxObjectNamePart());
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

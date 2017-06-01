package am.xmx.core.jmx;

import am.xmx.dto.XmxRuntimeException;
import am.xmx.service.IXmxService;

import javax.management.*;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * JMX Bean which translates all requests to a managed XMX object
 * through a weak reference. 
 * 
 * @author Andrey Mogilev
 */
class JmxBridgeModelBean implements DynamicMBean {
	
	private final int objectId;
	private final ModelMBeanInfoSupport mbeanInfo;
	private final IXmxService xmxService;

	public JmxBridgeModelBean(int objectId, ModelMBeanInfoSupport mbeanInfo, IXmxService xmxService) {
		this.objectId = objectId;
		this.mbeanInfo = mbeanInfo;
		this.xmxService = xmxService;
	}

	@Override
	public Object getAttribute(String name)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		
		Object obj = getObject();
		Field f = getField(name);
		
		try {
			return f.get(obj);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to read field " + name + ":" + e);
		}
	}

	@Override
	public void setAttribute(Attribute attr) throws RuntimeException {
		
		Object obj = getObject();
		
		try {
			Field f = getField(attr.getName());
			f.set(obj, attr.getValue());
		} catch (IllegalAccessException | MBeanException e) {
			throw new RuntimeException("Failed to read field " + attr.getName() + ":" + e);
		}
	}

	@Override
	public AttributeList getAttributes(String[] attrNames) {
        AttributeList responseList = new AttributeList(attrNames.length);
        for (String name : attrNames) {
            try {
				responseList.add(new Attribute(name, getAttribute(name)));
			} catch (AttributeNotFoundException | MBeanException | ReflectionException e) {
				throw new RuntimeException(e);
			}
        }
        
        return responseList;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		RuntimeException lastException = null;
		AttributeList responseList = new AttributeList();
        for (Attribute attr : attributes.asList()) {
            try {
            	setAttribute(attr);
            	responseList.add(attr);
			} catch (RuntimeException e) {
				lastException = e;
			}
        }
        
        if (lastException != null) {
        	throw lastException;
        }
		
        return responseList;
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		
		Object obj = getObject();
		
		MBeanOperationInfo foundOp = null;
		for (MBeanOperationInfo op : mbeanInfo.getOperations()) {
			if (actionName.equals(op.getName()) && signatureMatches(signature, op.getSignature())) {
				foundOp = op;
				break;
			}
		}
		
		if (foundOp == null) {
			throw new XmxRuntimeException("Failed to find method to invoke: name=" + actionName + 
					", signature=" + Arrays.toString(signature));
		}
		
		Descriptor descr = foundOp.getDescriptor();
		
		Object methodIdField = descr.getFieldValue("methodId");
		if (!(methodIdField instanceof Integer)) {
			throw new XmxRuntimeException("Missing methodId in operation descriptor: " + descr);
		}
		int methodId = (Integer)methodIdField;
		Method m = xmxService.getObjectMethodById(obj, methodId);

		// set context class loader to enable functionality which depends on it, like JNDI
		ClassLoader prevClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(obj.getClass().getClassLoader());
		try {
			Object result = m.invoke(obj, params);
			return Objects.toString(result);
		} catch (XmxRuntimeException | InvocationTargetException | IllegalAccessException e) {
			// convert to standard exception
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				throw new RuntimeException(e.getCause());
			}
		} finally {
			Thread.currentThread().setContextClassLoader(prevClassLoader);
		}
	}

	private Object getObject() {
		Object obj = xmxService.getObjectById(objectId);
		if (obj == null) {
			throw new XmxRuntimeException("Object not found. It may be already GC'ed: objectId=" + objectId);
		}
		return obj;
	}
	
	private Field getField(String name) throws RuntimeOperationsException, MBeanException {
		Object obj = getObject();
		
		ModelMBeanAttributeInfo foundAttr = mbeanInfo.getAttribute(name);
		if (foundAttr == null) {
			throw new XmxRuntimeException("Failed to find attribute: name=" + name);
		}
		
		Descriptor descr = foundAttr.getDescriptor();
		
		Object fieldIdField = descr.getFieldValue("fieldId");
		if (!(fieldIdField instanceof Integer)) {
			throw new XmxRuntimeException("Missing fieldId in operation descriptor: " + descr);
		}
		int fieldId = (Integer)fieldIdField;
		return xmxService.getObjectFieldById(obj, fieldId);
	}

	private boolean signatureMatches(String[] signature, MBeanParameterInfo[] paramsInfo) {
		if (signature.length != paramsInfo.length) {
			return false;
		}
		for (int i = 0; i < signature.length; i++) {
			String signaturePart = signature[i];
			MBeanParameterInfo paramInfo = paramsInfo[i];
			if (!signaturePart.equals(paramInfo.getType())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return mbeanInfo;
	}
}

package am.xmx.core.jmx;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;

import am.xmx.core.XmxManager;
import am.xmx.dto.XmxRuntimeException;
import am.xmx.service.IXmxService;

/**
 * JMX Bean which translates all requests to a managed XMX object
 * through a weak reference. 
 * 
 * @author Andrey Mogilev
 */
class JmxBridgeModelBean implements DynamicMBean {
	
	private int objectId;
	private ModelMBeanInfoSupport mbeanInfo;

	public JmxBridgeModelBean(int objectId, ModelMBeanInfoSupport mbeanInfo) {
		this.objectId = objectId;
		this.mbeanInfo = mbeanInfo;
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
		
		IXmxService xmxService = XmxManager.getService();
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
		
		try {
			Object result = xmxService.invokeObjectMethod(obj, m, params);
			return result;
		} catch (XmxRuntimeException | InvocationTargetException e) {
			// convert to standard exception
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				throw new RuntimeException(e.getCause());
			}
		}
	}

	private Object getObject() {
		Object obj = XmxManager.getService().getObjectById(objectId);
		if (obj == null) {
			throw new XmxRuntimeException("Object not found. It may be already GC'ed: objectId=" + objectId);
		}
		return obj;
	}
	
	private Field getField(String name) throws RuntimeOperationsException, MBeanException {
		IXmxService xmxService = XmxManager.getService();
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

package am.xmx.core.jmx;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
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

	// TODO: use ClassInfo instead?
	public JmxBridgeModelBean(int objectId, ModelMBeanInfoSupport mbeanInfo) {
		this.objectId = objectId;
		this.mbeanInfo = mbeanInfo;
	}

	@Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		
		IXmxService xmxService = XmxManager.getService();
		Object obj = xmxService.getObjectById(objectId);
		if (obj == null) {
			throw new XmxRuntimeException("Object not found. It may be already GC'ed: objectId=" + objectId);
		}
		
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
		
		Object result = xmxService.invokeObjectMethod(obj, m, params);
		return result;
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

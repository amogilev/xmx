// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.core;

import com.gilecode.xmx.cfg.IXmxConfig;
import com.gilecode.xmx.core.jmx.JmxSupport;
import com.gilecode.xmx.service.ISignatureService;
import com.gilecode.xmx.service.IXmxClassMembersLookup;
import com.gilecode.xmx.service.SignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.modelmbean.ModelMBeanInfoSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class XmxClassManager {

	private static final Logger logger = LoggerFactory.getLogger(XmxClassManager.class);

	static final ISignatureService signatureService = new SignatureService();

	/**
	 * Unique ID of the class in XMX system, or {@code 0} for unmanaged classes
	 */
	private final int id;
	
	/**
	 * Name of the class
	 */
	private final String className;
	
	/**
	 * Corresponding class loader information.
	 */
	private final ManagedClassLoaderWeakRef classLoaderInfo;
	
	/**
	 * Max managed instances allowed.
	 */
	private final int maxInstances;
	
	/**
	 * Part of JMX ObjectName (without object ID) used for managed 
	 * class instances, or {@code null} if they do not need to be
	 * published to JMX.
	 */
	private final String jmxObjectNamePart;
	private final IXmxConfig config;

	//
	// these fields are set during init()
	//

	/**
	 * The managed class.
	 */
	private Class<?> clazz;

	/**
	 * Managed objects of this class, if any.
	 * <strong>NOTE:</strong> This set is not thread-safe, require
	 * external synchronization.
	 */
	private Set<Integer> objectIds;

	private IXmxClassMembersLookup membersLookup;

	//
	// other fields are initiated lazily
	//

	/**
	 * JMX model for managed class instances, or {@code null}
	 * if they need not to be published to JMX.
	 */
	private ModelMBeanInfoSupport jmxClassModel;

	/**
	 * Cached list of managed methods mapped by unique IDs.
	 */
	// volatile // not actually required, as Map values are always the same
	private WeakReference<Map<String, Method>> cachedManagedMethodsRef;

	/**
	 * Cached list of managed methods mapped by unique IDs.
	 */
	private WeakReference<Map<String, Field>> cachedManagedFieldsRef;

	private boolean disabledByMaxInstances; // TODO maybe extend to custom flags

	public XmxClassManager(int id, String className, ManagedClassLoaderWeakRef classLoaderInfo,
	                       int maxInstances, String jmxObjectNamePart, IXmxConfig config) {
		this.id = id;
		this.className = className;
		this.classLoaderInfo = classLoaderInfo;
		this.maxInstances = maxInstances;
		this.jmxObjectNamePart = jmxObjectNamePart;
		this.config = config;
		this.membersLookup = null;
	}

	public void init(Class<?> clazz, boolean initJmxModel) {
		this.clazz = clazz;
		this.objectIds = new HashSet<>(2);
		this.membersLookup = new ClassMembersLookup();
		if (initJmxModel) {
			jmxClassModel = JmxSupport.createModelForClass(clazz, getAppInfo().getName(), getManagedMethods(), getManagedFields(), config);

		}

		logger.debug("Initialized class info for class (classId={})", getClassName(), getId());
	}

	public boolean isInitialized() {
		return clazz != null;
	}

	/**
	 * Resets the previous initialization, reverts to uninitialized form with only basic information about the 
	 * class. This method may be invoked only when there are no alive managed instances of the class left.
	 */
	// synchronized // NOTE: actually, all uses are synchronized on XmxManager.instance, similar to all accesses to objectIds
	public void reset() {

		if (objectIds != null && objectIds.size() > 0) {
			throw new IllegalStateException("Cannot reset ManagedClassInfo while some instances are still "
					+ "alive; class=" + className);
		}
		
		this.cachedManagedFieldsRef = null;
		this.cachedManagedMethodsRef = null;
		this.jmxClassModel = null;
		this.disabledByMaxInstances = false;
		this.objectIds = null;
		this.clazz = null; // required for Class GC

		logger.debug("Reset class {} (classId={})", getClassName(), getId());
	}
	
	public int getId() {
		return id;
	}

	public boolean isManaged() {
		return id != 0;
	}

	public String getClassName() {
		return className;
	}

	public ManagedClassLoaderWeakRef getClassLoaderInfo() {
		return classLoaderInfo;
	}

	public ManagedAppInfo getAppInfo() {
		return classLoaderInfo.getAppInfo();
	}

	public ModelMBeanInfoSupport getJmxClassModel() {
		return jmxClassModel;
	}
	
	public String getJmxObjectNamePart() {
		return jmxObjectNamePart;
	}

	public Class<?> getManagedClass() {
		return clazz;
	}

	public Map<String, Method> getManagedMethods() {
		Map<String, Method> managedMethods = getCachedManagedMethods();
		if (managedMethods == null) {
			managedMethods = buildManagedMethodsList();
			setCachedManagedMethods(managedMethods);
		}
		return managedMethods;
	}

	public Map<String, Field> getManagedFields() {
		Map<String, Field> managedFields = getCachedManagedFields();
		if (managedFields == null) {
			managedFields = buiildManagedFieldsList();
			setCachedManagedFields(managedFields);
		}
		return managedFields;
	}

	public boolean isDisabled() {
		return disabledByMaxInstances;
	}

	public boolean isDisabledByMaxInstances() {
		return disabledByMaxInstances;
	}

	public void setDisabledByMaxInstances(boolean disabledByMaxInstances) {
		this.disabledByMaxInstances = disabledByMaxInstances;
	}

	public Set<Integer> getObjectIds() {
		return objectIds;
	}
	
	public int getMaxInstances() {
		return maxInstances;
	}

	// NOTE: no ID and JMX info in hashCode & equals! That's OK as it not used as Map keys
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XmxClassManager other = (XmxClassManager) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "XmxManagedClassInfo [id=" + id + ", className=" + className
				+ "]";
	}

	Map<String, Method> getCachedManagedMethods() {
		final Reference<Map<String, Method>> ref = this.cachedManagedMethodsRef;
		return ref == null ? null : ref.get();
	}

	private void setCachedManagedMethods(Map<String, Method> managedMethods) {
		cachedManagedMethodsRef = new WeakReference<>(managedMethods);
	}

	Map<String, Field> getCachedManagedFields() {
		final Reference<Map<String, Field>> ref = this.cachedManagedFieldsRef;
		return ref == null ? null : ref.get();
	}

	private void setCachedManagedFields(Map<String, Field> managedFields) {
		cachedManagedFieldsRef = new WeakReference<>(managedFields);
	}

	private Map<String, Method> buildManagedMethodsList() {
		Map<String, Method> methods = new LinkedHashMap<>(32);
		Class<?> c = clazz;
		while (c != null) {
			Method[] declaredMethods = c.getDeclaredMethods();

			// sort methods declared in one class
			Arrays.sort(declaredMethods, ReflectionUtils.METHOD_COMPARATOR);

			for (Method m : declaredMethods) {
				// TODO: check if managed (e.g. by level or pattern)
				// TODO: skip overridden methods
				if (!m.isSynthetic()) {
					methods.put(signatureService.getMethodSignature(m), m);
				}
			}
			c = c.getSuperclass();
		}
		return methods;
	}

	/**
	 * Returns all managed fields, mapped by their unique ID (which is the field name with optional
	 * '^superlevel' suffix in case of duplicated field names, i.e. hidden fields).
	 */
	private Map<String, Field> buiildManagedFieldsList() {
		Map<String, Field> fields = new LinkedHashMap<>(16);
		int superLevel = 0;
		Class<?> c = clazz;
		while (c != null) {
			Field[] declaredFields = c.getDeclaredFields();
			Arrays.sort(declaredFields, ReflectionUtils.FIELD_COMPARATOR);

			for (Field f : declaredFields) {
				// TODO: check if managed (e.g. by level or pattern)
				// TODO: skip overridden methods
				String fname = f.getName();
				String fid = fields.containsKey(fname) ? fname + "^" + superLevel : fname;
				fields.put(fid, f);
			}
			c = c.getSuperclass();
			superLevel++;
		}
		return fields;
	}

	public IXmxClassMembersLookup getMembersLookup() {
		return membersLookup;
	}

	class ClassMembersLookup implements IXmxClassMembersLookup {

		@Override
		public Map<String, Field> listManagedFields() {
			return getManagedFields();
		}

		@Override
		public Map<String, Method> listManagedMethods() {
			return getManagedMethods();
		}

		@Override
		public Field getManagedField(String fid) {
			// if cached fields list is available, use it
			Map<String, Field> managedFields = getCachedManagedFields();
			if (managedFields != null) {
				return managedFields.get(fid);
			}

			// otherwise, parse fid and find the field by name (i.e. do not list all managed fields again!)
			Class<?> c = getManagedClass();
			int iSuffix = fid.indexOf('^');
			if (iSuffix > 0) {
				String fname = fid.substring(0, iSuffix);
				String suffix = fid.substring(iSuffix + 1);
				try {
					int superLevel = Integer.parseInt(suffix);
					for (int i = 0; i < superLevel; i++) {
						c = c.getSuperclass();
						if (c == null) {
							return null;
						}
					}
					return c.getDeclaredField(fname);
				} catch (Exception e) {
					// field not found or wrong format
					return null;
				}
			} else {
				while (c != null) {
					try {
						return c.getDeclaredField(fid);
					} catch (Exception e) {
						c = c.getSuperclass();
					}
				}
				return null;
			}
		}

		@Override
		public Method getManagedMethod(String mid) {
			Map<String, Method> managedMethods = getCachedManagedMethods();
			if (managedMethods != null) {
				return managedMethods.get(mid);
			} else {
				// parse and find in class
				try {
					return signatureService.findMethodBySignature(clazz.getClassLoader(), mid);
				} catch (ClassNotFoundException | NoSuchMethodException e) {
					return null;
				}
			}
		}
	}
}


/*
 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.cluster;

import com.ethercis.servicemanager.common.ReplaceVariable;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxMBeanHandle;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.I_ExtendedService;
import com.ethercis.servicemanager.service.I_Service;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.ethercis.servicemanager.service.ServiceRegistry;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Helper Class to ease setting up a service.
 * <p>
 * A service can be simply implemented by extending this class logonservice follows:
 * <p>
 * 
 * <pre>
 * {@code
 * public class DummyService extends ClusterInfo implements DummyConsumerMXBean {
 * private ClusterController glob;
 * private String ME = "DummyConsumer";
 * 
 * public void shutdown() {
 * 	...	
 * }
 *  
 *  public String getType() {
 *  	return ME;
 *  }
 *  
 *  public String getVersion() {
 *  	return "1.0";
 *  }
 *  
 *  protected void doInit(ClusterController global, ServiceInfo serviceInfo)
 *  		throws ServiceManagerException {
 *  	this.glob = global;
 *        //add JMX details...
 *        putObject(I_Info.JMX_PREFIX+"DummyService", this);
 *        ... do something else
 *  }
 * }
 * </pre>
 */
public abstract class ClusterInfo implements I_Service, I_Info {
	public final static String ORIGINAL_ENGINE_GLOBAL = "_originalEngineGlobal";
	public final static int UNTOUCHED = 0;
	public final static int UPPER_CASE = 1;
	public final static int LOWER_CASE = 2;

    protected I_ServiceRunMode.DialectSpace dialectSpace = null;

	private static Logger log = Logger.getLogger(ClusterInfo.class);
	protected RunTimeSingleton global;
	protected ServiceInfo serviceInfo;
	private Map objects = new HashMap();
	private Set propsOfOwnInterest;
	private InfoHelper helper;
	private static String key = "com.ethercis.servicemanager.ClusterController";

	/** My JMX registration */
	private Set jmxHandleSet = new HashSet();
	private ContextNode contextNode;

	public static String getStrippedString(String pureVal) {
		String corrected = RunTimeSingleton.getStrippedString(pureVal);
		return ReplaceVariable.replaceAll(corrected, "-", "_");
	}

	private final static String fixCase(String val, int chCase) {
		if (val == null)
			return null;
		if (chCase == UPPER_CASE)
			return val.toUpperCase();
		if (chCase == LOWER_CASE)
			return val.toLowerCase();
		return val;
	}

	/**
	 * Convenience to allow the usage of a name mapped to the hostname which can
	 * be used logonservice an identifier in a database. Specifically used for the prefix
	 * in the replication.
	 * 
	 * @param info
	 *            can be null, in which case only system properties are changed.
	 */
	public static String setStrippedHostname(I_Info info, int chCase) {
		String hostName = System.getProperty("host.name");
		if (hostName == null) {
			try {
				hostName = fixCase(InetAddress.getLocalHost().getHostName(),
						chCase);
				if (hostName == null) {
					log.warn("The property 'host.name' was not set and it was not possible to retrieve the default host name (will try the IP Address instead)");
					hostName = fixCase(InetAddress.getLocalHost()
							.getHostAddress(), chCase);
					if (hostName == null) {
						log.warn("the property 'host.name' is not set, will not set 'stripped.host.name'");
						return null;
					} else {
						log.warn("the property 'host.name' is not set and the default is set to the IP '"
								+ hostName + "'");
					}
				}
				log.info("Setting 'host.name' to '" + hostName + "'");
				System.setProperty("host.name", hostName);
				info.put("host.name", hostName);
			} catch (UnknownHostException ex) {
				log.warn("Could not retrieve the local hostname (I wanted it since 'host.name' was not set)");
				return null;
			}
		}
		String strippedHostName = getStrippedString(hostName);
		String oldStrippedHostName = System.getProperty("stripped.host.name");
		if (oldStrippedHostName != null) {
			if (!oldStrippedHostName.equals(strippedHostName))
				log.warn("The system property 'stripped.host.name' was already set to '"
						+ oldStrippedHostName
						+ "' will NOT change it to '"
						+ strippedHostName + "'");
		} else {
			System.setProperty("stripped.host.name", strippedHostName);
			log.debug("Set system property 'stripped.host.name' to '"
					+ strippedHostName + "'");
		}
		if (info != null) {
			oldStrippedHostName = info.get("stripped.host.name", null);
			if (oldStrippedHostName != null) {
				if (!oldStrippedHostName.equals(strippedHostName))
					log.warn("The info property 'stripped.host.name' was already set to '"
							+ oldStrippedHostName
							+ "' will NOT change it to '"
							+ strippedHostName + "'");
			} else {
				info.put("stripped.host.name", strippedHostName);
				log.debug("Set info property 'stripped.host.name' to '"
						+ strippedHostName + "'");
			}
		}
		return strippedHostName;
	}

	/**
	 * Checks in the registry if such an object exists and if not it creates one
	 * for you and initializes it.
	 * 
	 * @param info
	 *            The info object to use.
	 * @param serviceClassName
	 *            The complete name of the service to load.
	 * @param registryName
	 *            The name to search in the registry for this instance. The
	 *            registry will be in the info object passed. If you specify
	 *            null, the lookup is skipped.
	 * @return
	 * @throws Exception
	 */
	public static Object loadService(I_Info info, String serviceClassName,
			String registryName) throws Exception {
		synchronized (info) {
			I_ExtendedService service = null;
			if (serviceClassName == null || serviceClassName.length() < 1)
				throw new Exception(
						"loadService: The name of the service has not been specified");
			if (registryName != null)
				service = (I_ExtendedService) info.getObject(registryName);
			if (service != null) {
				log.debug(serviceClassName
						+ " returned (was already initialized)");
				return service;
			}
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			service = (I_ExtendedService) cl.loadClass(serviceClassName)
					.newInstance();
			service.init(info);
			if (registryName != null)
				info.putObject(registryName, service);
			log.debug(serviceClassName + " created and initialized");
			return service;
		}
	}

	/**
	 * constructor with local properties (these properties are not propagated to
	 * the global instance controller)
	 */
	public ClusterInfo(Set propsOfOwnInterest) {
		this.propsOfOwnInterest = propsOfOwnInterest;
		if (this.propsOfOwnInterest == null)
			this.propsOfOwnInterest = new HashSet();
		this.helper = new InfoHelper(this);
	}

	/**
	 * constructor with local properties (these properties are not propagated to
	 * the global instance controller)
	 */
	public ClusterInfo(String[] propKeysAsString) {
		this.propsOfOwnInterest = new HashSet();
		if (propKeysAsString != null) {
			for (int i = 0; i < propKeysAsString.length; i++)
				this.propsOfOwnInterest.add(propKeysAsString[i]);
		}
		this.helper = new InfoHelper(this);
	}

	/**
	 * Implicit constructor. Should be OK for most services
	 */
	public ClusterInfo() {
		this(new String[] { null });
	} // implicit constructor

	/**
	 * Additional infos are added on top of the initial Global configuration.
	 * 
	 * @param otherGlobal
	 *            can not be null.
	 * @param additionalInfo
	 *            can be null. If not null, these properties will be added on
	 *            top of the already set in global.
	 */
	public ClusterInfo(RunTimeSingleton otherGlobal, I_Info additionalInfo)
			throws ServiceManagerException {
		this.propsOfOwnInterest = new HashSet();
		this.helper = new InfoHelper(this);
		init(otherGlobal, null);
		InfoHelper.fillInfoWithEntriesFromInfo(this, additionalInfo);
	}

	/**
	 * @param additionalInfo
	 *            can be null. If not null, these properties will be added on
	 *            top of the already set in global.
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public ClusterInfo(ClusterInfo baseInfo, I_Info additionalInfo)
			throws ServiceManagerException {
		this(baseInfo.global, additionalInfo);
	}

	/**
	 * 
	 * @param global
	 *            The global passed by the RunLevelManager, this is not the
	 *            object owned by the service. It is the original
	 *            clustercontroller.
	 * @param serviceInfo
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	protected abstract void doInit(RunTimeSingleton global,
			ServiceInfo serviceInfo) throws ServiceManagerException;

	/**
	 * actual generic service initialization
	 */
	public final void init(RunTimeSingleton global_, ServiceInfo serviceInfo)
			throws ServiceManagerException {
		String[] additionalAttributes = null;
		// global_.getProperty().getProperties().list(System.out);
		// boolean wantsClone = wantGlobalClone(global_);
		// if (this.onServer)

		this.global = global_;
		putObject(key, this.global);

		setStrippedHostname(this, UPPER_CASE);

		log.debug(this.getClass().getName() + " initializeSession");
		this.serviceInfo = serviceInfo;
		if (this.serviceInfo != null) {

			log.debug("initializeSession: service parameters: '"
					+ this.serviceInfo.dumpServiceParameters() + "'");
			log.debug("initializeSession: service user data  : '"
					+ this.serviceInfo.getUserData() + "'");

		}

		// add the property 'id' if not set explicitly already ...
		String id = get(ID, null);
		if (id == null) {
			if (this.serviceInfo != null)
				put(ID, this.serviceInfo.getType());
			else
				log.warn("No id has been defined for this info, please add one since this could be used to find your instance: example '"
						+ ID + "=someId'");
		}

		// For JMX instanceName may not contain ","
		if (serviceInfo != null) {
			String instanceName = serviceInfo.getType();
			this.contextNode = new ContextNode(ContextNode.SERVICE_MARKER_TAG,
					instanceName, this.global.getScopeContextNode());
		}

		helper.replaceAllEntries(this, propsOfOwnInterest);
		if (serviceInfo != null && serviceInfo.getParameters() != null) {
			I_Info tmpServiceInfos = new PropertiesInfo(
					serviceInfo.getParameters());
			helper.replaceAllEntries(tmpServiceInfos, propsOfOwnInterest);
		}

		doInit(global_, serviceInfo);
		initJmx();
		replaceAllEntries();
	}

	protected void replaceAllEntries() {
		helper.replaceAllEntries(this, propsOfOwnInterest);
	}

	private void initJmx() {
		Map jmxMap = InfoHelper.getObjectsWithKeyStartingWith(JMX_PREFIX, this);
		if (jmxMap.size() < 1)
			return;
		String[] keys = (String[]) jmxMap.keySet().toArray(
				new String[jmxMap.size()]);
		for (int i = 0; i < keys.length; i++) {
			Object obj = jmxMap.get(keys[i]);
			String name = keys[i];
			ContextNode child = new ContextNode(ContextNode.ADMIN_MARKER_TAG,
					name, this.contextNode);
			log.info("MBean '" + name + "' found. Will attach it logonservice '"
					+ child.getRelativeName() + "' to '"
					+ this.contextNode.getAbsoluteName() + "'");
			try {
				JmxMBeanHandle mBeanHandle = this.global.registerMBean(child,
						obj);
				this.jmxHandleSet.add(mBeanHandle);
			} catch (ServiceManagerException e) {
				log.error(e.getMessage());
			}
		}
	}

    protected void initCompatibilityMode(){
        if (dialectSpace != null)
            return;

        String compatibilityValue = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_PARAMETER, I_ServiceRunMode.DialectSpace.STANDARD.toString());
        dialectSpace =  I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue);
        log.info("Running in Query compatibility mode:"+ dialectSpace.name());
    }

	/**
	 * helper to perform registration of JMX class if needed...
	 * 
	 * @param info
	 * @param clazz
	 */
	public void registerJMX(I_Info info, Class clazz) {
		// Add JMX Registration
		String jmxName = I_Info.JMX_PREFIX + clazz.getName();
		info.putObject(jmxName, this);
		log.info("Added object '" + jmxName
				+ "' to I_Info to be added logonservice an MBean");
	}

	/**
	 * The service name logonservice configured im <tt>services.xml</tt>
	 */
	public String getType() {
		if (this.serviceInfo != null)
			return this.serviceInfo.getType();
		return null;
	}

	/**
	 * The service version logonservice configured in <tt>services.xml</tt>
	 */
	public String getVersion() {
		if (this.serviceInfo != null)
			return this.serviceInfo.getVersion();
		return null;
	}

	/**
    */
	public void shutdown() throws ServiceManagerException {
		if (this.jmxHandleSet.size() < 1)
			return;
		JmxMBeanHandle[] handles = (JmxMBeanHandle[]) this.jmxHandleSet.toArray(new JmxMBeanHandle[this.jmxHandleSet.size()]);
		for (int i = 0; i < handles.length; i++) {
			log.info("Unregister MBean '"+ (handles[i]==null? "*UNDEF*":handles[i].getContextNode().getAbsoluteName()) + "'");
			this.global.unregisterMBean(handles[i]);
		}
	}

	/**
	 * get raw attribute or property value (no variable substitution)
	 */
	public String getRaw(String key) {
		if (key == null)
			return null;
		try {
			if (this.propsOfOwnInterest.contains(key)) {
				String ret = (this.serviceInfo == null) ? null
						: this.serviceInfo.getParameters().getProperty(key,
								null);
				String prefix = (this.serviceInfo == null) ? ""
						: this.serviceInfo.getPrefix();
				return this.global.getProperty().get(prefix + key, ret);
			}

			String value = this.global.get(key, null, null, this.serviceInfo);
			if ("jdbc.drivers".equals(key)
					&& (value == null || value.length() < 1))
				return this.global.getProperty().get("JdbcDriver.drivers", ""); // ask
																				// services.properties
			log.debug("Resolving " + key + " to '" + value + "'");
			return value;
		} catch (ServiceManagerException e) {
			log.warn(e.toString());
			return null;
		}
	}

	/**
	 * get property value or return default 'def' value
	 */
	public String get(String key, String def) {
		if (key == null)
			return def;
		def = this.helper.replace(def);
		key = this.helper.replace(key);

		// hack: if in topic name is a ${..} our global tries to replace it and
		// throws an exception, but we need it without replacement:
		// $_{...} resolves this issue, but nicer would be:
		// <attribute id='db.queryMeatStatement' replace='false'>...</attribute>
		try {
			if (this.propsOfOwnInterest.contains(key)) {
				String ret = (this.serviceInfo == null) ? def
						: this.serviceInfo.getParameters()
								.getProperty(key, def);
				ret = this.helper.replace(ret);
				String prefix = (this.serviceInfo == null) ? ""
						: this.serviceInfo.getPrefix();
				return this.global.getProperty().get(prefix + key, ret);
			}

			String value = this.global.get(key, def, null, this.serviceInfo);
			value = this.helper.replace(value);

			log.debug("Resolving " + key + " to '" + value + "'");
			return value;
		} catch (ServiceManagerException e) {
			log.warn(e.toString());
			return def;
		}
	}

	/**
	 * put a property value
	 */
	public void put(String key, String value) {
		if (key != null)
			key = this.helper.replace(key);
		if (value != null)
			value = this.helper.replace(value);
		if (value == null)
			this.global.getProperty().removeProperty(key);
		else {
			try {
				String prefix = (this.serviceInfo == null) ? "" : serviceInfo
						.getPrefix(); // "service/" + getType() + "/"
				this.global.getProperty().set(prefix + key, value);
			} catch (Exception e) {

				log.warn(e.toString() + ": Ignoring setting " + key + "="
						+ value);
			}
		}
	}

	/**
	 * put a property value (no variable substitution)
	 */
	public void putRaw(String key, String value) {
		if (value == null)
			this.global.getProperty().removeProperty(key);
		else {
			try {
				String prefix = (this.serviceInfo == null) ? "" : serviceInfo
						.getPrefix(); // "service/" + getType() + "/"
				this.global.getProperty().set(prefix + key, value);
			} catch (Exception e) {
				log.warn(e.toString());
			}
		}
	}

	/**
	 * get a property logonservice Long
	 */
	public long getLong(String key, long def) {
		if (key == null)
			return def;
		key = this.helper.replace(key);
		try {
			return this.global.get(key, def, null, this.serviceInfo);
		} catch (ServiceManagerException e) {
			log.warn(e.toString());
			return def;
		}
	}

	/**
	 * get a property logonservice Int
	 */
	public int getInt(String key, int def) {
		if (key == null)
			return def;
		key = this.helper.replace(key);
		try {
			return this.global.get(key, def, null, this.serviceInfo);
		} catch (ServiceManagerException e) {
			log.warn(e.toString());
			return def;
		}
	}

	/**
	 * get a property logonservice boolean
	 */
	public boolean getBoolean(String key, boolean def) {
		if (key == null)
			return def;
		key = this.helper.replace(key);
		try {
			return this.global.get(key, def, null, this.serviceInfo);
		} catch (ServiceManagerException e) {
			log.warn(e.toString());
			return def;
		}
	}

	/**
	 * get an object
	 */
	public Object getObject(String key) {
		return this.objects.get(key);
	}

	/**
	 * set an object
	 */
	public Object putObject(String key, Object o) {
		if (o == null)
			return this.objects.remove(key);
		return this.objects.put(key, o);
	}

	/**
	 * get the key set of properties
	 */
	public Set getKeys() {
		Iterator iter = this.global.getProperty().getProperties().keySet()
				.iterator();
		HashSet out = new HashSet();
		String prefix = "";
		if (this.serviceInfo != null)
			prefix = this.serviceInfo.getPrefix();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			if (key.startsWith(prefix))
				key = key.substring(prefix.length());
			out.add(key);
		}
		if (this.serviceInfo != null)
			PropertiesInfo.addSet(out, this.serviceInfo.getParameters()
					.keySet());
		return out;
	}

	/**
	 * get the key set of objects
	 */
	public Set getObjectKeys() {
		return this.objects.keySet();
	}

	/**
	 * dump the contextual info of instance
	 * 
	 * @param info
	 * @return
	 */
	public static String dump(I_Info info) {
		StringBuffer buf = new StringBuffer();
		Iterator iter = info.getKeys().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			String val = info.get(key, "");
			buf.append(key).append("=").append(val).append("\n");
		}
		return buf.toString();
	}

	/**
	 * get the cluster controller of instance
	 * 
	 * @return
	 */
	public RunTimeSingleton getGlobal() {
		return this.global;
	}

	private static RunTimeSingleton getOriginalRunTimeSingleton(
            RunTimeSingleton global, boolean recursive) {
		if (global == null)
			return null;
		RunTimeSingleton glob = (RunTimeSingleton) global
				.getObjectEntry(ClusterInfo.ORIGINAL_ENGINE_GLOBAL);

		if (glob == null)
			glob = (RunTimeSingleton) global.getObjectEntry(key);
		if (recursive) {
			if (glob != null && glob != global)
				return getOriginalRunTimeSingleton(glob, recursive);
		} else {
			if (glob == null)
				return global;
			return glob;
		}
		return global;
	}

	/**
	 * Returns the Base global. The Base Global is the Global
	 * 
	 * @param info
	 * @return
	 */
	public static RunTimeSingleton getOriginalRunTimeSingleton(I_Info info) {
		final boolean recursive = false;
		RunTimeSingleton glob = (RunTimeSingleton) info
				.getObject(ClusterInfo.ORIGINAL_ENGINE_GLOBAL);

		if (glob == null)
			glob = (RunTimeSingleton) info.getObject(key);

		if (glob != null)
			return getOriginalRunTimeSingleton(glob, recursive);
		return null;
	}

	/**
	 * returns the interface to a defined service in the registry.
	 * <p>
	 * For example:
	 * <p>
	 * <code>I_Dummy mydummyIF = ServiceGetter.get(global, "DUMMY", "1.0");</code>
	 * 
	 * @param name
	 * @param version
	 * @param parms optional parameters (provisional for specifying service location f.ex. proxied service)
	 * @return
	 */
	//the same in static mode...
	public static <IF extends I_Service> IF getRegisteredService(RunTimeSingleton c, String name, String version, Object... parms) throws ServiceManagerException {
		ServiceRegistry services = c.getServiceRegistry();
		I_Service srv = services.getService(name + "," + version);

		if (srv == null) {
			log.error("Unable to load service identified as:" + name + "," + version + ". Please check your code...");
			throw new ServiceManagerException(c,
					SysErrorCode.INTERNAL_NULLPOINTER, "Internal", "Unable to load service :" + name + "," + version + ". Please check your code...");
		}

		return (IF) srv;
	}
	
	
	public <IF extends I_Service> IF getRuntimeService(RunTimeSingleton c,
			String name, String version, Object... parms) throws ServiceManagerException {
		return getRegisteredService(c, name, version, parms);
	}
	


	/**
	 * returns a service stub corresponding to the named service<p>
	 * The service stub requires that actual calls to a published method is done
	 * with the StubMethodInvoke. This is required logonservice the service may run in another
	 * process and/or system and access to the service is done using various protocols.
	 * @param c
	 * @param name
	 * @param version
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
//	public <IF extends I_Service> IF getServiceStub(ClusterController c, String name, String version) throws ServiceManagerException
//	{
//		// for the time being, the service is internal (a service manager will be used later)
//		ServiceRegistry services = c.getServiceRegistry();
//		I_Service srv = services.getService(name + "," + version);
//		
//		
//		
//	}
//	
	/**
	 * return the service info for this service
	 */
	public ServiceInfo getServiceInfo() {
		return this.serviceInfo;
	}

    public I_ServiceRunMode.DialectSpace getDialectSpace(){
//        String compatibilityValue = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_PARAMETER, I_ServiceRunMode.DialectSpace.STANDARD.toString());
//        return I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue);
        return dialectSpace;
    }
}

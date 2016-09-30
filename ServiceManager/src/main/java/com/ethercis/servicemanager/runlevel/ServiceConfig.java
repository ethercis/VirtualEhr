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


package com.ethercis.servicemanager.runlevel;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.common.I_AttributeUser;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxMBeanHandle;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

/**
 * This class contains the information on how to configure a certain service and
 * when a certain service is invoked by the run level manager
 */
public class ServiceConfig implements ServiceConfigMBean, I_AttributeUser {
	private final RunTimeSingleton glob;
	private static Logger log = LogManager.getLogger(ServiceConfig.class);

	/** the id specifying a given service configuration */
	private String id = "";

	/** the complete class name for the service to be loaded */
	private String className = "";

	public static boolean DEFAULT_CREATE = true;
	/** Shall this service be instantiated? */
	private boolean create;

	/** the coloumn separated list of jar files on which to look for the class */
	private String jarPath;

	/* the actions to trigger (all actions are put here) */
	private Vector<RunLevelAction> actions;

	/** stores the action for the upgoing run level (if none it will be null) */
	private RunLevelAction upAction;

	/** stores the action for the down going run level (if none it will be null) */
	private RunLevelAction downAction;

	/** the properties for the service */
	private Properties attributes;

	/* Set containing all attributes which are wrapped inside a CDATA */
	private HashSet<String> wrappedAttributes;

	/**
	 * timestamp used to get uniquity (since runlevel + sequeuence is not
	 * unique)
	 */
	public TimeStamp uniqueTimestamp;

	/** My JMX registration */
	private JmxMBeanHandle mbeanHandle;
	private ContextNode contextNode;

	/**
	 * This constructor takes all parameters needed
	 */
	public ServiceConfig(RunTimeSingleton glob, String id, boolean create,
			String className, String jar, Properties attributes,
			Vector<RunLevelAction> actions) {
		this.uniqueTimestamp = new TimeStamp();
		this.glob = glob;

		// if (log.isLoggable(Level.FINER)) this.log.call(ME, "constructor");
		// if (log.isLoggable(Level.FINE))
		// log.trace(ME, "constructor id='" + id + "', className='" + className
		// + "'");
		this.id = id;
		this.create = create;
		this.className = className;
		this.jarPath = jar;

		if (attributes != null)
			this.attributes = attributes;
		else
			this.attributes = new Properties();

		if (actions != null)
			this.actions = actions;
		else
			this.actions = new Vector<RunLevelAction>();
		this.wrappedAttributes = new HashSet<String>();
	}

	/**
	 * Construtor where we can define attributes (no need to define actions)
	 */
	public ServiceConfig(RunTimeSingleton glob, String id, boolean create,
			String className, Properties attributes) {
		this(glob, id, create, className, (String) null, attributes,
				(Vector<RunLevelAction>) null);
	}

	/**
	 * Minimal constructor
	 */
	public ServiceConfig(RunTimeSingleton glob, String id, boolean create,
			String className) {
		this(glob, id, create, className, (String) null, (Properties) null,
				(Vector<RunLevelAction>) null);
	}

	/**
	 * Really minimal constructor
	 */
	public ServiceConfig(RunTimeSingleton glob) {
		this(glob, "", DEFAULT_CREATE, "", (String) null, (Properties) null,
				(Vector<RunLevelAction>) null);
	}

	public String getId() {
		return this.id;
	}

	/**
	 * Shall the service be created?
	 * 
	 * @return true if create
	 */
	public boolean isCreate() {
		return this.create;
	}

	/**
	 * Shall the service be created? <br />
	 * Called from JMX (jconsole)
	 * 
	 * @param create
	 *            true/false
	 */
	public void setCreate(boolean create) {

		this.glob.getRunlevelManager().toggleCreate(this, create);

		this.create = create;
	}

	/**
	 * Shall the service be created? <br />
	 * Called from SAX parser
	 * 
	 * @param create
	 *            true/false
	 */
	public void setCreateInternal(boolean create) {
		this.create = create;
	}

	public String getClassName() {
		return this.className;
	}

	public void addAction(RunLevelAction action) {
		if (action == null) {
			log.warn("addAction the action is null");
			return;
		}
		if (action.getOnStartupRunlevel() > -1)
			this.upAction = action;
		else if (action.getOnShutdownRunlevel() > -1)
			this.downAction = action;
		// adds it also to the common actions (in future there could be more
		// action types)
		this.actions.add(action);
	}

	public RunLevelAction getUpAction() {
		return this.upAction;
	}

	public RunLevelAction getDownAction() {
		return this.downAction;
	}

	public void addAttribute(String key, String value) {
		this.attributes.setProperty(key, value);
	}

	public RunLevelAction[] getActions() {
		return (RunLevelAction[]) this.actions
				.toArray(new RunLevelAction[this.actions.size()]);
	}

	public void setId(String id) {
		if (id != null) {
			this.id = id;
		}
	}

	public void registerMBean() {
		if (this.id != null) {
			JmxMBeanHandle handle = this.mbeanHandle;
			this.mbeanHandle = null;
			if (handle != null)
				this.glob.unregisterMBean(handle);

			ContextNode parent = this.glob.getRunlevelManager()
					.getContextNode();
			this.contextNode = new ContextNode(
					ContextNode.RUNLEVEL_SERVICE_MARKER_TAG, this.id, parent);
			try {
				this.mbeanHandle = this.glob.registerMBean(this.contextNode,
						this);
			} catch (ServiceManagerException e) {
				log.warn(e.getMessage());
			}

		}
	}

	public void shutdown() {
		JmxMBeanHandle handle = this.mbeanHandle;
		this.mbeanHandle = null;
		if (handle != null)
			this.glob.unregisterMBean(handle);
	}

	public void setClassName(String className) {
		if (className != null)
			this.className = className;
	}

	public void setJar(String jar) {
		if (jar != null)
			this.jarPath = jar;
	}

	/**
	 * returns the ServiceInfo object out of this configuration
	 */
	public ServiceInfo getServiceInfo() {
		return new ServiceInfo(this.glob, this.id, this.className,
				this.attributes);
	}

	/**
	 * When the attribute is written to a string in the toXml methods it is
	 * wrapped inside a CDATA in case you pass 'true' here.
	 */
	public void wrapAttributeInCDATA(String attributeKey) {
		this.wrappedAttributes.add(attributeKey);
	}

	/**
	 * When the attribute is written to a string in the toXml methods it is
	 * wrapped inside a CDATA. This can be undone if you pass 'true' here.
	 */
	public void unwrapAttributeFromCDATA(String attributeKey) {
		this.wrappedAttributes.remove(attributeKey);
	}

	/**
	 * returns an xml litteral string representing this object.
	 */
	public String toXml(String extraOffset) {
		StringBuffer sb = new StringBuffer(512);
		if (extraOffset == null)
			extraOffset = "";
		String offset = Constants.OFFSET + extraOffset;

		sb.append(offset).append("<service ");
		sb.append("id='").append(this.id).append("' ");
		sb.append("create='").append(this.create).append("' ");
		sb.append("className='").append(this.className).append("' ");
		if (this.jarPath != null) {
			sb.append("jar='").append(this.jarPath).append("' ");
		}
		sb.append(">");

		// and now the child elements (first attributes and then actions)
		String offset2 = offset + "   ";
		Enumeration enumer = this.attributes.keys();
		while (enumer.hasMoreElements()) {
			String key = (String) enumer.nextElement();
			String value = this.attributes.getProperty(key);
			sb.append(offset2).append("<attribute id='").append(key)
					.append("'>");
			if (this.wrappedAttributes.contains(key)) {
				sb.append("<![CDATA[").append(value).append("]]>");
			} else
				sb.append(value);
			sb.append("</attribute>");
		}

		enumer = this.actions.elements();
		while (enumer.hasMoreElements()) {
			RunLevelAction value = (RunLevelAction) enumer.nextElement();
			sb.append(value.toXml(extraOffset + "   "));
		}
		sb.append(offset).append("</service>");
		return sb.toString();
	}

	public String toXml() {
		return toXml("");
	}

	public java.lang.String usage() {
		return "not implemented";
	}

	public java.lang.String getUsageUrl() {
		return "not implemented";
	}

	public void setUsageUrl(java.lang.String url) {
	}
}

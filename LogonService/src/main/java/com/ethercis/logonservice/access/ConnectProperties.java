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
 * Project: EtherCIS openEHR system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */
//Copyright
package com.ethercis.logonservice.access;

import java.util.Properties;

import com.ethercis.logonservice.security.SecurityProperties;
import com.ethercis.logonservice.session.ConnectionStateEnum;
import com.ethercis.logonservice.session.SessionName;
import com.ethercis.logonservice.session.SessionProperties;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.NodeId;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.property.PropBoolean;
import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_ConnectionStateEnum;
import com.ethercis.servicemanager.common.session.I_SecurityProperties;
import com.ethercis.servicemanager.common.session.I_SessionProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class encapsulates the properties of a login() or connect().
 * 
 * NOTE: This class is not synchronized and not Serializable (add these
 * functionality when you need it)
 */
public final class ConnectProperties extends SessionProperties implements
		java.io.Serializable, Cloneable, ConnectPropertiesMBean, I_ConnectProperties {
	private static Logger log = LogManager.getLogger(ConnectProperties.class.getName());
	private static final long serialVersionUID = 1L;
	// private final String ME = "ConnectData";
	private I_ConnectionStateEnum initialConnectionState = ConnectionStateEnum.UNDEF;

	/**
	 * PtP messages wanted? True is default
	 * <p />
	 * 
	 * <pre>
	 * &lt;ptp>false&lt;ptp/> <!-- Don't send me any PtP messages (prevents spamming) -->
	 * </pre>
	 */
	protected PropBoolean ptpAllowed = new PropBoolean(true);

	/**
	 * Allows to mark that we are an ehrserver cluster node.
	 */
	protected PropBoolean clusterNode = new PropBoolean(false);

	/**
	 * If the client automatically notifies that it is alive and the login
	 * session is extended
	 */
	protected PropBoolean refreshSession = new PropBoolean(false);

	/**
	 * If duplicateUpdates=false we will send only one update, even if the same
	 * client subscribed multiple times on the same message. Defaults to true.
	 */
	protected PropBoolean duplicateUpdates = new PropBoolean(true);

	/**
	 * Used for ConnectReturnQos only: If reconnected==true a client has
	 * reconnected to an existing session
	 */
	protected PropBoolean reconnected = new PropBoolean(false);

	protected String serverInstanceId;

	/** Session settings */
	private SessionProperties sessionProps;

	// private String defaultType; // This address type is chosen from all
	// available addresses (we need a loadbalancing algo!)

	/** The node id to which we want to connect */
	private NodeId nodeId;

	/**
	 * Constructor for client side.
	 */
	public ConnectProperties(RunTimeSingleton glob) throws ServiceManagerException {
		this(glob, null, null);
		initialize(glob);
	}

	/**
	 * Constructs the specialized quality of service object for a connect() or
	 * connect-return call. NOTE: The serialData is not parsed - use the factory
	 * for it. NOTE: The security service is not initialized, use
	 * loadClientService() to do it
	 * 
	 * @param factory
	 *            The factory which knows how to serialize and parse me
	 * @param serialData
	 *            The connect properties (SessionClientProperties)
	 * @param nodeId
	 *            The node id with stripped special characters (see
	 *            ClusterController#getStrippedId)
	 */
	public ConnectProperties(RunTimeSingleton glob,	I_SessionClientProperties serialData, NodeId nodeId)
			throws ServiceManagerException {
		super(glob, serialData, MethodName.CONNECT);
		this.nodeId = (nodeId == null) ? new NodeId(this.controller.getStrippedId()) : nodeId;
		this.sessionProps = new SessionProperties(this.controller, this.nodeId);
		initialize(glob);
	}

	/**
	 * Constructor for cluster server.
	 * <p />
	 * 
	 * @param nodeId
	 *            The the unique cluster node id, supports configuration per
	 *            node
	 */
	public ConnectProperties(RunTimeSingleton glob, NodeId nodeId)
			throws ServiceManagerException {
		this(glob, null, nodeId);
		initialize(glob);
	}

	/**
	 * Constructor for cluster server.
	 * <p />
	 * 
	 * @param nodeId
	 *            The the unique cluster node id, supports configuration per
	 *            node
	 */
	public ConnectProperties(RunTimeSingleton glob, I_SessionProperties props)
			throws ServiceManagerException {
		this(glob, null, null);
		initialize(glob);
	}

	private void initialize(RunTimeSingleton glob) throws ServiceManagerException {
		this.serverInstanceId = this.controller.getInstanceId();
		this.securityprops = new SecurityProperties(glob);
		this.securityprops.setCredential(accessPassword(null));
		if (this.sessionProps.getSessionName() != null) {
			this.securityprops.setUserId(this.sessionProps.getSessionName()
					.getLoginName());
		}
	}

	/**
	 * Force a security configuration.
	 * <p>
	 * You can use loadClientPlugin() or setUserId() instead which loads the
	 * given/default security plugin and does a lookup in the environment.
	 */
	public void setSecurityProperties(I_SecurityProperties securityProps) {
		this.securityprops = securityProps;
		if (!this.sessionProps.isSessionNameModified()) {
			SessionName sessionName = new SessionName(this.controller,
					this.securityprops.getUserId()); // parse it and strip it if
														// user has given an
														// absolute name
			this.sessionProps.setSessionName(sessionName, false);
		}
	}

	/**
	 * Allows to set or overwrite the login name for I_SecurityQos.
	 * <p />
	 * 
	 * @param userId
	 *            The unique user id (loginName)
	 */
	public void setUserId(String userId) throws ServiceManagerException {
		if (this.securityprops == null) {
			this.securityprops = new SecurityProperties(controller);
			this.securityprops.setCredential(accessPassword(null));
		}

		SessionName sessionName = new SessionName(this.controller, userId); // parse
																			// it
																			// and
																			// strip
																			// it
																			// if
																			// user
																			// has
																			// given
																			// an
																			// absolute
																			// name
		this.securityprops.setUserId(sessionName.getLoginName());

		if (!this.sessionProps.isSessionNameModified()) {
			this.sessionProps.setSessionName(sessionName, false);
		}
	}

	/**
	 * Private environment lookup if given passwd/credential is null
	 * 
	 * @return Never null, defaults to "secret"
	 */
	private String accessPassword(String passwd) {
		if (passwd != null) {
			return passwd;
		}
		passwd = this.controller.getProperty().get("passwd", "secret");
		if (this.nodeId != null) {
			passwd = this.controller.getProperty().get(
					"passwd[" + this.nodeId + "]", passwd);
		}

		log.debug("Initializing passwd=" + passwd + " nodeId=" + this.nodeId);
		return passwd;
	}

	/**
	 * @return The session QoS, never null
	 */
	public I_SessionProperties getSessionProperties() {
		return this.sessionProps;
	}

	/**
	 * Get our unique SessionName.
	 * <p />
	 * 
	 * @return The unique SessionName (never null)
	 */
	public SessionName getSessionName() {
		if (this.sessionProps.getSessionName() == null
				&& getSecurityProperties() != null) {
			this.sessionProps.setSessionName(new SessionName(controller,
					getSecurityProperties().getUserId()), false);
		}
		return this.sessionProps.getSessionName();
	}

	/**
	 * Set our unique SessionName.
	 */
	public void setSessionName(SessionName sessionName) {
		this.sessionProps.setSessionName(sessionName, true);
	}

	/**
	 * @param Set
	 *            if we accept point to point messages
	 */
	public void setPtpAllowed(boolean ptpAllowed) {
		this.ptpAllowed.setValue(ptpAllowed);
	}

	public boolean isPtpAllowed() {
		return this.ptpAllowed.getValue();
	}

	public PropBoolean isPtpAllowedProp() {
		return this.ptpAllowed;
	}

	/**
	 * @param Set
	 *            if we are a cluster node.
	 */
	public void setClusterNode(boolean clusterNode) {
		this.clusterNode.setValue(clusterNode);
	}

	/**
	 * @return Are we a cluster?
	 */
	public boolean isClusterNode() {
		return this.clusterNode.getValue();
	}

	/**
	 * @return The isClusterNode flag object
	 */
	public PropBoolean getClusterNodeProp() {
		return this.clusterNode;
	}

	/**
	 * @return refreshSession is true if the client automatically notifies
	 *         xmlBlaster that it is alive and the login session is extended
	 */
	public final boolean getRefreshSession() {
		return this.refreshSession.getValue();
	}

	/**
	 * @param refreshSession
	 *            true: The client automatically notifies xmlBlaster that it is
	 *            alive and the login session is extended
	 */
	public final void setRefreshSession(boolean refreshSession) {
		this.refreshSession.setValue(refreshSession);
	}

	/**
	 * @return The isClusterNode flag object
	 */
	public PropBoolean getRefreshSessionProp() {
		return this.refreshSession;
	}

	/**
	 * @param Set
	 *            if we allow multiple updates for the same message if we have
	 *            subscribed multiple times to it.
	 */
	public void setDuplicateUpdates(boolean duplicateUpdates) {
		this.duplicateUpdates.setValue(duplicateUpdates);
	}

	/**
	 * @return true if we allow multiple updates for the same message if we have
	 *         subscribed multiple times to it.
	 */
	public boolean duplicateUpdates() {
		return this.duplicateUpdates.getValue();
	}

	/**
    */
	public PropBoolean duplicateUpdatesProp() {
		return this.duplicateUpdates;
	}

	/**
	 * Used for ConnetReturnQos only: If reconnected==true a client has
	 * reconnected to an existing session
	 */
	public void setReconnected(boolean reconnected) {
		this.reconnected.setValue(reconnected);
	}

	/**
	 * Used for ConnetReturnQos only.
	 * 
	 * @return true A client has reconnected to an existing session
	 */
	public boolean isReconnected() {
		return this.reconnected.getValue();
	}

	/**
    */
	public PropBoolean getReconnectedProp() {
		return this.reconnected;
	}

	/**
	 * @return the login credentials or null if not set
	 */
	public I_SecurityProperties getSecurityProperties() {
		return this.securityprops;
	}

	public String getUserId() {
		I_SecurityProperties securityProps = getSecurityProperties();
		if (securityProps == null)
			return "NoLoginName";
		else
			return securityProps.getUserId();
	}

	/**
	 * Returns the connection state directly after the connect() method returns
	 * (client side only).
	 * 
	 * @return Usually ConnectionStateEnum.ALIVE or ConnectionStateEnum.POLLING
	 */
	public I_ConnectionStateEnum getInitialConnectionState() {
		return this.initialConnectionState;
	}

	/**
	 * Set the connection state directly after the connect() (client side only).
	 */
	public void setInitialConnectionState(
			I_ConnectionStateEnum initialConnectionState) {
		this.initialConnectionState = initialConnectionState;
	}

	/**
	 * The number of bytes of stringified qos
	 */
	public int size() {
		return this.toXml().length();
	}

	/**
	 * Unique id of the server (or a client), changes on each restart. If
	 * 'node/heron' is restarted, the instanceId changes.
	 * 
	 * @return nodeId + timestamp, '/node/heron/instanceId/33470080380'
	 */
	public String getInstanceId() {
		return this.serverInstanceId;
	}

	/**
	 * Unique id of the server (or a client), changes on each restart. If
	 * 'node/heron' is restarted, the instanceId changes.
	 * 
	 * @param instanceId
	 *            e.g. '/node/heron/instanceId/33470080380'
	 */
	public void setInstanceId(String instanceId) {
		this.serverInstanceId = instanceId;
	}

	/**
	 * Converts the data into a valid XML ASCII string.
	 * 
	 * @return An XML ASCII string
	 */
	public String toString() {
		return toXml();
	}

	/**
	 * Dump state of this object into a XML ASCII string.
	 */
	public String toXml() {
		return toXml((String) null);
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *            indenting of tags for nice response
	 * @return internal state of the connect QoS logonservice a XML ASCII string
	 */
	public String toXml(String extraOffset) {
		return toXml(extraOffset, (Properties) null);
	}

	public String toXml(String extraOffset, Properties props) {
		// return this.factory.writeObject(this, extraOffset, props);
		return "not implemented";
	}

	public String toXml(String extraOffset, boolean forceReadable) {
		Properties props = null;
		if (forceReadable) {
			props = new Properties();
			props.put(Constants.TOXML_FORCEREADABLE, "" + forceReadable);
		}
		// return this.factory.writeObject(this, extraOffset, props);
		return "not implemented";
	}

	/**
	 * Returns a shallow clone, you can change safely all basic or immutable
	 * types like boolean, String, int. Currently TopicProperty and RouteInfo is
	 * not cloned (so don't change it)
	 */
	public Object clone() {
		log.warn("clone() is not tested");
		ConnectProperties newOne = null;
		newOne = (ConnectProperties) super.clone();
		synchronized (this) {
			newOne.ptpAllowed = (PropBoolean) this.ptpAllowed.clone();
			newOne.clusterNode = (PropBoolean) this.clusterNode.clone();
			newOne.refreshSession = (PropBoolean) this.refreshSession.clone();
			newOne.duplicateUpdates = (PropBoolean) this.duplicateUpdates
					.clone();
			newOne.reconnected = (PropBoolean) this.reconnected.clone();
			// newOne.sessionQos = (SessionQos)this.sessionQos.clone();
			// newOne.securityQos = (I_SecurityQos)this.securityQos.clone();
			newOne.nodeId = this.nodeId;
		}
		return newOne;
	}

	@Override
	public boolean isPersistent() {
		// TODO Auto-generated method stub
		return false;
	}

}

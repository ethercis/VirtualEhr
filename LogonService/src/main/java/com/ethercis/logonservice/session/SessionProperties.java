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
//Copyright
package com.ethercis.logonservice.session;


import org.apache.log4j.Logger;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.NodeId;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.property.PropBoolean;
import com.ethercis.servicemanager.common.property.PropInt;
import com.ethercis.servicemanager.common.property.PropLong;
import com.ethercis.servicemanager.common.session.I_ConnectionStateEnum;
import com.ethercis.servicemanager.common.session.I_SecurityProperties;
import com.ethercis.servicemanager.common.session.I_SessionName;
import com.ethercis.servicemanager.common.session.I_SessionProperties;

/**
 * This class provides the properties used for a connect/disconnect or login to the service.
 * A PropConnect map is passed to the authenticate instance before allowing further 
 * accesses to the backend.
 * @author Christian Chevalley
 *
 */
public class SessionProperties implements java.io.Serializable, Cloneable, I_SessionProperties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8527345016332011845L;

	private static Logger log = Logger.getLogger(SessionProperties.class);

	private I_ConnectionStateEnum initialConnectionState = ConnectionStateEnum.UNDEF;

	/** Default session span of life is one day, given in millis "-session.timeout 86400000" */
	private PropLong sessionTimeout = new PropLong(Constants.DAY_IN_MILLIS);

	/** Maximum of ten parallel logins for the same client "session.maxSessions 10" */
	public static final int DEFAULT_maxSessions = 10;
	private PropInt maxSessions = new PropInt(DEFAULT_maxSessions);

	/** Clear on login all other sessions of this user (for recovery reasons) "session.clearSessions false" */
	private PropBoolean clearSessions = new PropBoolean(false);

	/**
	 * If reconnectSameClientOnly=true a client with a given publicSessionId
	 * can only reconnect to ehrserver if it is the the client instance which
	 * created the login session (other clients can't capture the session).
	 * This option is useful if you want to assure a singleton client (set additionally
	 * maxSessions=1).
	 * Defaults to false.
	 */
	protected PropBoolean reconnectSameClientOnly = new PropBoolean(false);
	
	/**
	 * true if reconnect without credential checking
	 */
	private boolean bypassCredentialCheck = false;

	/** Passing own secret sessionId */
	private String sessionId = null;

	/** The unified session name which is a clusterwide unique identifier
	 */
	private SessionName sessionName;
	private boolean sessionNameModified = false;

	/** The node id to which we want to connect */
	private NodeId nodeId = null;
	
	/** 
	 * used to add client specific data
	 */
	
	private I_SessionClientProperties clientProperties;

	/**
	 * cache user security settings for this session
	 */
	protected I_SecurityProperties securityprops;

	protected RunTimeSingleton controller;
	
	/**
	 * session flags
	 */
	/**
	 * If reconnected==true a client has reconnected to an existing session
	 */
	protected PropBoolean reconnected = new PropBoolean(false);
	
	/**
	 * If the client automatically notifies that it is alive
	 * and the login session is extended
	 */
	protected PropBoolean refreshSession = new PropBoolean(false);
	

	
	
	protected String serverInstanceId;

	/**
	 * hardcoded attribute to the security manager
	 */
	private String clientSecurityServiceType = Constants.DEFAULT_SERVICE_SECURITY_MANAGER_ID;
	private String clientSecurityServiceVersion = Constants.DEFAULT_SERVICE_SECURITY_MANAGER_VERSION;

	/**
	 * defaults to false
	 * If true the pubSessionId<0 and pubSessionId>0 can have
	 * different maxSessions and clearSession specified
	 */
	private boolean sessionLimitsPubSessionIdSpecific = false;

	private MethodName methodName;

	private I_SessionClientProperties serialData;
	/**
	 * Constructor for cluster server. 
	 * @param nodeId The the unique cluster node id, supports configuration per node
	 */
	public SessionProperties(RunTimeSingleton controller, NodeId nodeId) {
		this.controller = (controller == null) ? RunTimeSingleton.instance() : controller;
		this.nodeId = nodeId;
		this.clientProperties = new SessionClientProperties(controller);
		
		initialize();
	}

	public SessionProperties(RunTimeSingleton controller, I_SessionClientProperties serialData, MethodName methodName) {
		this.controller = (controller == null) ? RunTimeSingleton.instance() : controller;
		this.methodName = methodName;
		this.serialData = serialData;
		this.clientProperties = new SessionClientProperties(controller);
		initialize();
	}
	
	
	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getClientProperties()
	 */
	@Override
	public I_SessionClientProperties getClientProperties() {
		return clientProperties;
	}

	public void setClientProperties(I_SessionClientProperties clientProperties) {
		this.clientProperties = clientProperties;
	}

	private final void initialize() {

		// login name: As default use the JVM System.property ${user.name} which is usually the login name of the OS
		String defaultLoginName = controller.getProperty().get("user.name", "guest");

		String sessionNameStr = controller.getProperty().get("session.name", (String)null);
		if (sessionNameStr == null) {
			String loginName = controller.getProperty().get("loginName", (String)null);
			if (loginName != null) {
				sessionNameStr = loginName;
			}
			else {
				sessionNameStr = defaultLoginName;
			}
		}
		
		
		setSessionTimeout(controller.getProperty().get("session.timeout", Constants.DAY_IN_MILLIS)); // One day
		setMaxSessions(controller.getProperty().get("session.maxSessions", DEFAULT_maxSessions));
		clearSessions(controller.getProperty().get("session.clearSessions", false));
		setReconnectSameClientOnly(controller.getProperty().get("session.reconnectSameClientOnly", false));
		setSecretSessionId(controller.getProperty().get("session.secretSessionId", (String)null));
		if (nodeId != null) {
			sessionNameStr = controller.getProperty().get("session.name["+nodeId+"]", sessionNameStr);
			setSessionTimeout(controller.getProperty().get("session.timeout["+nodeId+"]", getSessionTimeout()));
			setMaxSessions(controller.getProperty().get("session.maxSessions["+nodeId+"]", getMaxSessions()));
			clearSessions(controller.getProperty().get("session.clearSessions["+nodeId+"]", clearSessions()));
			setReconnectSameClientOnly(controller.getProperty().get("session.reconnectSameClientOnly["+nodeId+"]", reconnectSameClientOnly()));
			setSecretSessionId(controller.getProperty().get("session.secretSessionId["+nodeId+"]", getSecretSessionId()));
		}

		this.sessionName = new SessionName(controller, nodeId, sessionNameStr);
//		this.sessionName = null;
		//if (log.isLoggable(Level.FINE)) log.trace(ME, "sessionName =" + sessionName.getRelativeName() + " absolute=" + sessionName.getAbsoluteName());

//		{
//			// user warning for the old style loginName
//			String loginName = controller.getProperty().get("loginName", (String)null);
//			if (loginName != null && !loginName.equals(sessionNameStr))
//				log.warning("session.name=" + this.sessionName + " is stronger than loginName=" + loginName + ", we proceed with " + this.sessionName);
//		}

		log.debug("initialize session.name=" + this.sessionName + " nodeId=" + nodeId);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getSessionTimeout()
	 */
	@Override
	public final long getSessionTimeout() {
		return this.sessionTimeout.getValue();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setSessionTimeout(long)
	 */
	@Override
	public final void setSessionTimeout(long timeout) {
		if (timeout < 0L)
			this.sessionTimeout.setValue(0L);
		else
			this.sessionTimeout.setValue(timeout);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getMaxSessions()
	 */
	@Override
	public final int getMaxSessions() {
		return this.maxSessions.getValue();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setMaxSessions(int)
	 */
	@Override
	public final void setMaxSessions(int max) {
		if (max < 0)
			this.maxSessions.setValue(0);
		else
			this.maxSessions.setValue(max);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#clearSessions()
	 */
	@Override
	public boolean clearSessions() {
		return this.clearSessions.getValue();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#clearSessions(boolean)
	 */
	@Override
	public void clearSessions(boolean clear) {
		this.clearSessions.setValue(clear);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setReconnectSameClientOnly(boolean)
	 */
	@Override
	public void setReconnectSameClientOnly(boolean reconnectSameClientOnly) {
		this.reconnectSameClientOnly.setValue(reconnectSameClientOnly);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#reconnectSameClientOnly()
	 */
	@Override
	public boolean reconnectSameClientOnly() {
		return this.reconnectSameClientOnly.getValue();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#reconnectSameClientOnlyProp()
	 */
	@Override
	public PropBoolean reconnectSameClientOnlyProp() {
		return this.reconnectSameClientOnly;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setSecretSessionId(java.lang.String)
	 */
	@Override
	public void setSecretSessionId(String id) {
		if(id==null || id.equals("")) id = null;
		this.sessionId = id;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getSecretSessionId()
	 */
	@Override
	public final String getSecretSessionId() {
		return this.sessionId;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getPublicSessionId()
	 */
	@Override
	public final long getPublicSessionId() {
		if (this.sessionName != null) {
			return this.sessionName.getPublicSessionId();
		}
		return 0L;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#hasPublicSessionId()
	 */
	@Override
	public final boolean hasPublicSessionId() {
		if (this.sessionName != null) {
			return this.sessionName.isSession();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setSessionName(com.ethercis.servicemanager.common.session.I_SessionName)
	 */
	@Override
	public void setSessionName(I_SessionName sessionName) {
		this.sessionName = (SessionName)sessionName;
		this.sessionNameModified = true;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setSessionName(com.ethercis.servicemanager.common.session.I_SessionName, boolean)
	 */
	@Override
	public void setSessionName(I_SessionName sessionName, boolean markAsModified) {
		this.sessionName = (SessionName)sessionName;
		if (markAsModified) {
			this.sessionNameModified = markAsModified;
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#isSessionNameModified()
	 */
	@Override
	public boolean isSessionNameModified() {
		return sessionNameModified;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getSessionName()
	 */
	@Override
	public SessionName getSessionName() {
		return this.sessionName;
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setReconnected(boolean)
	 */
	@Override
	public void setReconnected(boolean reconnected) {
		this.reconnected.setValue(reconnected);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#isReconnected()
	 */
	@Override
	public boolean isReconnected() {
		return this.reconnected.getValue();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getServerInstanceId()
	 */
	@Override
	public String getServerInstanceId() {
		return this.serverInstanceId;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setServerInstanceId(java.lang.String)
	 */
	@Override
	public void setServerInstanceId(String instanceId) {
		this.serverInstanceId = instanceId;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getInitialConnectionState()
	 */
	@Override
	public I_ConnectionStateEnum getInitialConnectionState() {
		return this.initialConnectionState;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setInitialConnectionState(com.ethercis.servicemanager.common.session.I_ConnectionStateEnum)
	 */
	@Override
	public void setInitialConnectionState(I_ConnectionStateEnum initialConnectionState) {
		this.initialConnectionState = initialConnectionState;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#bypassCredentialCheck(boolean)
	 */
	@Override
	public void bypassCredentialCheck(boolean bypassCredentialCheck) {
		this.bypassCredentialCheck = bypassCredentialCheck;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#bypassCredentialCheck()
	 */
	@Override
	public boolean bypassCredentialCheck() {
		return this.bypassCredentialCheck;
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getSecurityProperties()
	 */
	@Override
	public I_SecurityProperties getSecurityProperties() {
		return this.securityprops;
	}	
	
	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setSecurityProperties(com.ethercis.servicemanager.security.I_SecurityProperties)
	 */
	@Override
	public void setSecurityProperties(I_SecurityProperties securityprops) {
		this.securityprops = securityprops;
		if (!this.isSessionNameModified()) {
			SessionName sessionName = new SessionName(controller, this.securityprops.getUserId()); // parse it and strip it if user has given an absolute name
			this.setSessionName(sessionName, false);
		}
	}	
	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#toString()
	 */
	@Override
	public String toString() {
		return toXml();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#toXml()
	 */
	@Override
	public String toXml() {
		return toXml((String)null);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#toXml(java.lang.String)
	 */
	@Override
	public String toXml(String extraOffset) {
		StringBuffer sb = new StringBuffer(250);
		if (extraOffset == null) extraOffset = "";
		String offset = Constants.OFFSET + extraOffset;

		sb.append(offset).append("<session");
		if (getSessionName() != null) {
			sb.append(" name='").append(getSessionName().getAbsoluteName()).append("'");
		}
		if (this.sessionTimeout.isModified()) {
			sb.append(" timeout='").append(getSessionTimeout()).append("'");
		}
		if (this.maxSessions.isModified()) {
			sb.append(" maxSessions='").append(getMaxSessions()).append("'");
		}
		if (this.clearSessions.isModified()) {
			sb.append(" clearSessions='").append(clearSessions()).append("'");
		}
		if (this.reconnectSameClientOnly.isModified()) {
			sb.append(" reconnectSameClientOnly='").append(reconnectSameClientOnly()).append("'");
		}
		if (this.sessionId!=null)
			sb.append(" sessionId='").append(this.sessionId).append("'");
		sb.append("/>");

		return sb.toString();
	}

	/**
	 * Returns a shallow clone, you can change safely all basic or immutable types
	 * like boolean, String, int.
	 */
	public Object clone() {
		try {
			log.error("clone() is not tested");
			SessionProperties newOne = null;
			newOne = (SessionProperties)super.clone();
			synchronized(this) {
				newOne.sessionTimeout = (PropLong)this.sessionTimeout.clone();
				newOne.maxSessions = (PropInt)this.maxSessions.clone();
				newOne.clearSessions = (PropBoolean)this.clearSessions.clone();
				newOne.reconnectSameClientOnly = (PropBoolean)this.reconnectSameClientOnly.clone();
				//newOne.sessionName = (SessionName)this.sessionName.clone();
				//newOne.nodeId = (NodeId)this.nodeId.clone();
			}
			return newOne;
		}
		catch (CloneNotSupportedException e) {
			log.error("clone",e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#usage()
	 */
	@Override
	public String usage() {
		String text = "\n";
		text += "Control my session settings\n";
		text += "   -session.name       The name for login, e.g. 'joe' or with public session ID 'joe/2' []\n";
		text += "   -session.timeout    How long lasts our login session in milliseconds, 0 is forever,\n";
		text += "                       defaults to one day [" + Constants.DAY_IN_MILLIS + "].\n";
		text += "   -session.maxSessions\n";
		text += "                       Maximum number of simultanous logins per client [" + DEFAULT_maxSessions + "].\n";
		text += "   -session.clearSessions\n";
		text += "                       Kill other sessions running under my login name [false]\n";
		text += "   -session.reconnectSameClientOnly\n";
		text += "                       Only creator client may reconnect to session [false]\n";
		text += "   -session.secretSessionId\n";
		text += "                       The secret sessionId []\n";
		text += "\n";
		return text;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getClientPluginType()
	 */
	@Override
	public String getClientPluginType() {
		return this.clientSecurityServiceType;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getClientPluginVersion()
	 */
	@Override
	public String getClientPluginVersion() {
		return this.clientSecurityServiceVersion;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#getClientIp()
	 */
	@Override
	public String getClientIp() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#isSessionLimitsPubSessionIdSpecific()
	 */
	@Override
	public boolean isSessionLimitsPubSessionIdSpecific() {
		return sessionLimitsPubSessionIdSpecific ;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.servicemanager.session.I_SessionProperties#setSessionLimitsPubSessionIdSpecific(boolean)
	 */
	@Override
	public void setSessionLimitsPubSessionIdSpecific(boolean sessionLimitsPubSessionIdSpecific) {
		this.sessionLimitsPubSessionIdSpecific = sessionLimitsPubSessionIdSpecific;
	}
}

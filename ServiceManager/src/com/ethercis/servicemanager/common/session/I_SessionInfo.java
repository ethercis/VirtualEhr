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
package com.ethercis.servicemanager.common.session;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.common.ClientPropertiesInfo;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.security.I_SubjectInfo;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public interface I_SessionInfo {

	public abstract boolean isAlive();

	/**
	 * The unique name of this session instance.
	 * @return Never null
	 */
	public abstract ContextNode getContextNode();

	/**
	 * Configure server with '-xmlBlaster/acceptWrongSenderAddress true'
	 * or "-xmlBlaster/acceptWrongSenderAddress/joe true".
	 * Is available using JMX.
	 * @return true: We accept wrong sender address in PublishQos.getSender() (not myself)
	 */
	public abstract boolean isAcceptWrongSenderAddress();

	/**
	 * The protector prevents direct access to this sessionInfo instance.
	 */
	public abstract I_SessionInfoProtector getSessionInfoProtector();

	/**
	 * This is a unique instance id per JVM (it is the pubSessionId if the client hasn't specified its own).
	 * <p>
	 * It is NOT the secret sessionId and may be published with PtP messages
	 * without security danger
	 * </p>
	 */
	public abstract long getInstanceId();

	/**
	 * Access the synchronization object of this SessionInfo instance.
	 */
	public abstract ReentrantLock getLock();

	/**
	 * Freeing sessionInfo lock with test/assert code. 
	 * @param errorInfo
	 * @return number of holds released
	 */
	public abstract long releaseLockAssertOne(String errorInfo);

	/**
	 * This is the publicSessionId which is unique in the authenticate scope.
	 * <p />
	 * It is NOT the secret sessionId and may be published with PtP messages
	 * without security danger
	 * <p />
	 * @return The same logonservice getInstanceId()
	 * @see #getInstanceId
	 */
	public abstract long getPublicSessionId();

	public abstract boolean isShutdown();

	public abstract void removeExpiryTimer();

	/**
	 * Call this to reactivate the session expiry to full value
	 */
	public abstract void refreshSession() throws ServiceManagerException;

	public abstract I_ConnectProperties getConnectProperties();

	/**
	 * Access the unique login name of a client.
	 * <br />
	 * @return loginName
	 */
	public abstract String getLoginName();

	/**
	 * Accessing the SubjectInfo object
	 * <p />
	 * @return SubjectInfo
	 */
	public abstract I_SubjectInfo getSubjectInfo();

	/**
	 * @return The secret sessionId of this login session
	 */
	public abstract String getSecretSessionId();

	public abstract com.ethercis.servicemanager.common.security.I_Session getSecuritySession();

	/**
	 * Cluster wide unique identifier: /node/heron/client/<loginName>/<publicSessionId>,
	 * e.g. for logging only
	 * <p />
	 * @return e.g. "/node/heron/client/joe/2
	 */
	public abstract String getId();

	public abstract I_SessionName getSessionName();

	/**
	 * Check cluster wide if the sessions are identical
	 */
	public abstract boolean isSameSession(I_SessionInfo sessionInfo);

	//=========== Enforced by I_AdminSession ================
	public abstract String getQos();

	public abstract long getUptime();

	public abstract String getLoginDate();

	public abstract String getSessionTimeoutExpireDate();

	// JMX
	public abstract String getAliveSinceDate();

	// JMX
	public abstract String getPollingSinceDate();

	/** JMX */
	public abstract java.lang.String getUsageUrl();

	/**
	 * @return Returns the remoteProperties or null
	 */
	public abstract ClientPropertiesInfo getRemoteProperties();

	/**
	 * @return never null
	 */
	public abstract ClientProperty[] getRemotePropertyArr();

	public abstract boolean isStalled();

	/**
	 * Can be called when client connection is lost (NOT the callback connection).
	 * Currently only detected by the SOCKET protocol plugin.
	 * Others can only detect lost clients with their callback protocol pings
	 */
	public abstract void lostClientConnection();

	/**
	 * If the connection failed the reason is stored here, like this
	 * cleanup code knows what happened.
	 * @return the transportConnectFail
	 */
	public abstract ServiceManagerException getTransportConnectFail();

	/**
	 * Can be optionally used by the current authorization plugin.
	 */
	public abstract Object getAuthorizationCache();

	public abstract boolean isBlockClientSessionLogin();

	/**
	 * Map to store arbitrary info for this client, is cleaned up automatically when session dies
	 * Useful for example for plugins
	 */
	public abstract Object getUserObject(String key, Object defaultValue);

	public abstract boolean hasUserObject(String key);

	/**
	 * Use carefully to not harm other plugins. 
	 * @return of type Collections.synchronizedMap(new HashMap<String, Object>()
	 */
	public abstract Map<String, Object> getUserObjectMap();

	public abstract String killSession() throws ServiceManagerException;

	public abstract String getConnectionState();

	public abstract boolean isInitialized();

	public abstract String toXml();

	public abstract void updateConnectProperties(I_ConnectProperties newConnectProperties);

	public abstract void init(I_SubjectInfo subjectInfo, I_Session session,
			I_ConnectProperties connectprops) throws ServiceManagerException;
	

}
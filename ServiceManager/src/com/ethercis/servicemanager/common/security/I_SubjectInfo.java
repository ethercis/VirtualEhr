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
package com.ethercis.servicemanager.common.security;

import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanNotificationInfo;


import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.cluster.NodeId;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionInfoProtector;
import com.ethercis.servicemanager.common.session.I_SessionName;
import com.ethercis.servicemanager.common.session.I_SessionProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public interface I_SubjectInfo {

	/**
	 * The unique name of this authenticate instance.
	 * 
	 * @return Never null, for example "/xmlBlaster/node/heron/client/joe"
	 */
	public abstract ContextNode getContextNode();

	/**
	 * Access the synchronization object of this SubjectInfo instance.
	 */
	public abstract ReentrantLock getLock();

	/**
	 * Find a session by its pubSessionId or return null if not found
	 */
	public abstract I_SessionInfo getSessionInfo(I_SessionName sessionName);

	/**
	 * @return not null if client is a cluster node, else null
	 */
	public abstract NodeId getNodeId() throws ServiceManagerException;

	/**
	 * @return true if this client is an xmlBlaster cluster node
	 */
	public abstract boolean isCluster() throws ServiceManagerException;

	/**
	 * Subject specific informations from the security framework
	 * 
	 * @return null if created without login (for example with a PtP message)
	 */
	public abstract I_Authenticate getSecurityCtx();

	/**
	 * Is the client currently logged in?
	 * 
	 * @return true yes false client is not on line
	 */
	public abstract boolean isLoggedIn();

	/**
	 * Access the collection containing all SessionInfo objects of this user.
	 * 
	 * @return never null
	 */
	public abstract I_SessionInfo[] getSessions();

	/** @return true it publicSessionId is given by xmlBlaster server (if < 0) */
	public abstract int getCountSessionsInternal();

	/** @return true it publicSessionId is given by user/client (if > 0) */
	public abstract int getCountSessionsUser();

	/**
	 * Find a session by its absolute name.
	 * 
	 * @param absoluteName
	 *            e.g. "/node/heron/client/joe/2"
	 * @return SessionInfo or null if not found
	 */
	public abstract I_SessionInfo getSessionByAbsoluteName(String absoluteName);

	/**
	 * Find a session by its public session ID.
	 * 
	 * @param sessionName
	 * @return SessionInfo or null if not found
	 */
	public abstract I_SessionInfo getSession(I_SessionName sessionName);

	public abstract I_SessionInfo getFirstSession();

	/**
	 * @exception Throws
	 *                ServiceManagerException if max. sessions is exhausted
	 */
	public abstract void checkNumberOfSessions(I_SessionProperties props)
			throws ServiceManagerException;

	/**
	 * Check if client does a re-login and wants to destroy old sessions.
	 * 
	 * @return never null
	 */
	public abstract I_SessionInfo[] getSessionsToClear(I_SessionProperties q);

	/**
	 * Access the unique login name of a client. <br />
	 * If not known, its unique key (subjectId) is delivered
	 * 
	 * @return The SessionName object specific for a authenticate (pubSessionId is
	 *         null)
	 */
	public abstract I_SessionName getSubjectName();

	/**
	 * Cluster wide unique identifier "/node/heron/client/<loginName>" e.g. for
	 * logging
	 * <p />
	 * 
	 * @return e.g. "client/joe
	 */
	public abstract String getId();

	/**
	 * Access the unique login name of a client. <br />
	 * If not known, its unique key (subjectId) is delivered
	 * 
	 * @return loginName
	 */
	public abstract String getLoginName();

	/**
	 * Find a session by its public session ID.
	 * 
	 * @param pubSessionId
	 *            e.g. "-2"
	 * @return I_AdminSession or null if not found
	 */
	public abstract I_SessionInfoProtector getSessionByPubSessionId(
			long pubSessionId);

	/**
	 * Kills all sessions of this client
	 * 
	 * @return The list of killed sessions (public session IDs), in a human
	 *         readable string
	 */
	public abstract String killClient() throws ServiceManagerException;

	/**
	 * Get the SessionInfo with its public session identifier e.g. "5"
	 * 
	 * @return null if not found
	 */
	public abstract I_SessionInfo getSessionByPublicId(long publicSessionId);

	public abstract boolean isUndef();

	public abstract boolean isAlive();

	public abstract boolean isDead();

	public abstract String getStateStr();

	// =========== Enforced by I_AdminSubject and SubjectInfoProtector.java
	// ================
	/**
	 * @return startupTime in seconds
	 */
	public abstract long getUptime();

	public abstract String getCreationDate();

	/**
	 * How many update where sent for this client, the sum of all session and
	 * authenticate queues of this clients.
	 */
	public abstract long getNumUpdate();

	public abstract boolean isBlockClientLogin();

	public abstract String blockClientAndResetConnections();

	/**
	 * Access the number of sessions of this user.
	 * 
	 * @return The number of sessions of this user
	 */
	public abstract int getNumSessions();

	public abstract int getNumAliveSessions();

	/**
	 * @return The max allowed simultaneous logins of this user
	 */
	public abstract int getMaxSessions();

	/**
	 * Access a list of public session identifier e.g. "1,5,7,12"
	 * 
	 * @return An empty string if no sessions available
	 */
	public abstract String getSessionList();

	/** JMX */
	public abstract java.lang.String getUsageUrl();

	/**
	 * JMX: Enforced by interface NotificationBroadcasterSupport
	 */
	public abstract MBeanNotificationInfo[] getNotificationInfo();

	public abstract I_SessionInfo getOrCreateSessionInfo(
			I_SessionName sessionName, I_SessionProperties sessionProperties) throws ServiceManagerException;

	public abstract void toAlive(I_Authenticate subject) throws ServiceManagerException;

	public abstract void notifyAboutLogout(String id,
			boolean forceShutdownEvenIfEntriesExist) throws ServiceManagerException;

}
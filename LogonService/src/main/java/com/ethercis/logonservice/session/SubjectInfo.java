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


//Copyright
package com.ethercis.logonservice.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;


import com.ethercis.logonservice.session.protectors.SubjectInfoProtector;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.cluster.NodeId;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_SubjectInfo;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionInfoProtector;
import com.ethercis.servicemanager.common.session.I_SessionName;
import com.ethercis.servicemanager.common.session.I_SessionProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxMBeanHandle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The SubjectInfo stores all known data about a client.
 * <p>
 * It also contains a authenticate queue, where messages are stored until they are
 * delivered at the next login of this client.
 * </p>
 * <p>
 * There are three states for SubjectInfo namely UNDEF, ALIVE, DEAD. A
 * transition from UNDEF directly to DEAD is not supported. Transitions from
 * ALIVE or DEAD to UNDEF are not possible.
 * </p>
 * 
 * @author <a href="mailto:xmlBlaster@marcelruff.info">Marcel Ruff</a>
 */
public final class SubjectInfo extends NotificationBroadcasterSupport /*
																	 * implements
																	 * I_AdminSubject
																	 * ,
																	 * SubjectInfoMBean
																	 * -> is
																	 * delegated
																	 * to
																	 * SubjectInfoProtector
																	 */ implements I_SubjectInfo
{
	private String ME = "SubjectInfo";
	private final RunTimeSingleton glob;
	private static Logger log = LogManager.getLogger(SubjectInfo.class);
	private final ContextNode contextNode;

	private final SessionManager sessionmgr;

	/**
	 * The cluster wide unique identifier of the authenticate e.g.
	 * "/node/heron/client/joe"
	 */
	private I_SessionName subjectName;
	/** The partner class from the security framework */
	private I_Authenticate securityCtx = null;

	private boolean blockClientLogin;

	/**
	 * All sessions of this authenticate are stored in this map. The
	 * absoluteSessionName == sessionInfo.getId() is the key, the SessionInfo
	 * object the value
	 */
	private Map<String, SessionInfo> sessionMap = new HashMap<String, SessionInfo>();
	private volatile SessionInfo[] sessionArrCache;

	private final SubjectInfoProtector<?> subjectInfoProtector;

	private NodeId nodeId = null;
	private boolean determineNodeId = true;

	// Enforced by I_AdminSubject
	/** Incarnation time of this object instance in millis */
	private long startupTime;
	private int maxSessions;

	/** State during and after construction */
	public final int UNDEF = -1;
	/** State after calling toAlive() */
	public final int ALIVE = 0;
	/** State after calling shutdown() */
	public final int DEAD = 1;
	private int state = UNDEF;

	private ReentrantLock lock = new ReentrantLock();

	/** Statistics */
	private static long instanceCounter = 0L;
	private long instanceId = 0L;

	/** My JMX registration */
	private JmxMBeanHandle mbeanHandle;

	/**
	 * <p />
	 * 
	 * @param subjectName
	 *            The unique loginName
	 * @param securityCtx
	 *            The security context of this authenticate
	 * @param prop
	 *            The property from the authenticate queue, usually from
	 *            connectQos.getSubjectQueueProperty()
	 */
	public SubjectInfo(RunTimeSingleton glob, SessionManager sessionmgr,
			I_SessionName subjectName) // , I_Subject securityCtx, CbQueueProperty
										// prop)
			throws ServiceManagerException {
		synchronized (SubjectInfo.class) {
			this.instanceId = instanceCounter;
			instanceCounter++;
		}
		this.glob = glob;

		this.sessionmgr = sessionmgr;
		this.subjectInfoProtector = new SubjectInfoProtector(this);

		this.subjectName = subjectName; // new SessionName(glob,
										// glob.getNodeId(), loginName);
		if (this.subjectName.isSession()) {
			log.error(ME + ": Didn't expect a session name for a authenticate: "
					+ this.subjectName.toXml());
			Thread.dumpStack();
		}

		String instanceName = this.glob.validateJmxValue(this.subjectName
				.getLoginName());
		this.contextNode = new ContextNode(ContextNode.SUBJECT_MARKER_TAG,
				instanceName, this.glob.getContextNode());

		this.ME = this.instanceId + "-" + this.subjectName.getAbsoluteName();

		// JMX register "client/joe"
		this.mbeanHandle = this.glob.registerMBean(this.contextNode,
				this.subjectInfoProtector);

		log.debug(ME + ": Created new SubjectInfo");
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getContextNode()
	 */
	@Override
	public final ContextNode getContextNode() {
		return this.contextNode;
	}

	/**
	 * if state==UNDEF we block until we are ALIVE (or DEAD)
	 * 
	 * @exception If
	 *                we are DEAD or on one minute timeout, subjectInfo is never
	 *                locked in such a case
	 */
	public void waitUntilAlive(boolean returnLocked) throws ServiceManagerException {
		if (this.state == ALIVE) {
			if (returnLocked)
				this.lock.lock();
			return;
		}
		if (this.state == DEAD)
			throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, ME
					+ ".waitUntilAlive()",
					"Did not expect state DEAD, please try again.");

		log.debug(ME + ": is going to wait max. one minute");
		long msecs = 1000 * 60;
		while (true) {
			synchronized (this) {
				try {
					this.wait(msecs);
					break;
				} catch (InterruptedException e) {
					log.error(ME + ": Ignoring unexpected exception: "
							+ e.toString());
				}
			}
		}

		if (returnLocked)
			this.lock.lock();
		if (this.state != ALIVE) {
			if (returnLocked)
				this.lock.unlock();
			throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, ME
					+ ".waitUntilAlive()", "ALIVE not reached, state="
					+ this.state);
		}

		return;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getLock()
	 */
	@Override
	public ReentrantLock getLock() {
		return this.lock;
	}

	SubjectInfoProtector<?> getSubjectInfoProtector() {
		return this.subjectInfoProtector;
	}

	/**
	 * Initialize SubjectInfo
	 * 
	 * @param securityCtx
	 *            Can be null for PtP message with implicit SubjectInfo creation
	 * @param prop
	 *            The property to configure the PtP message queue
	 */
	public void toAlive(I_Authenticate securityCtx) throws ServiceManagerException {
		if (isAlive()) {
			return;
		}

		this.lock.lock();
		try {
			if (securityCtx != null) {
				this.securityCtx = securityCtx;
			}

			this.startupTime = System.currentTimeMillis();

			this.maxSessions = glob.getProperty().get("session.maxSessions",
					SessionProperties.DEFAULT_maxSessions);
			if (glob.getId() != null)
				this.maxSessions = glob.getProperty().get(
						"session.maxSessions[" + glob.getId() + "]",
						this.maxSessions);

			this.state = ALIVE;

			synchronized (this) {
				this.notifyAll(); // notify waitUntilAlive()
			}

			log.debug(ME + ": Transition from UNDEF to ALIVE done");
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * The shutdown is synchronized and checks if there is no need for this
	 * authenticate anymore.
	 * <p>
	 * clearQueue==false&&forceIfEntries==true: We shutdown and preserve
	 * existing PtP messages
	 * </p>
	 * 
	 * @param clearQueue
	 *            Shall the message queue of the client be destroyed logonservice well on
	 *            last session logout?
	 * @param forceIfEntries
	 *            Shutdown even if there are messages in the queue
	 */
	public void shutdown(boolean forceIfEntries) {
		log.debug(ME + ": forceIfEntries=" + forceIfEntries);

		this.lock.lock();
		try {

			if (!this.isAlive()) {
				log.debug(ME
						+ ": Ignoring shutdown request logonservice we are in state "
						+ getStateStr());
				return;
			}

			if (isLoggedIn()) {
				log.debug(ME
						+ ": Ignoring shutdown request logonservice there are still login sessions");
				return;
			}

			this.glob.unregisterMBean(this.mbeanHandle);

			this.sessionmgr.removeLoginName(this); // deregister

			this.state = DEAD;

			if (getSessions().length > 0) {
				log.warn(ME + ": shutdown of authenticate " + getLoginName()
						+ " has still " + getSessions().length
						+ " sessions - memory leak?");
			}
			synchronized (this.sessionMap) {
				this.sessionArrCache = null;
				this.sessionMap.clear();
			}

			synchronized (this) {
				this.notifyAll(); // notify waitUntilAlive()
			}
			// Not possible to allow toAlive()
			// this.securityCtx = null;
		} finally {
			this.lock.unlock();
		}
	}

	/**
	 * Shutdown my queue
	 */
	public void finalize() {
		try {
			log.debug(ME + ": finalize - garbage collected");
			// boolean force = true;
			// this.subjectQueue.shutdown();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			super.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Blocks for existing SessionInfo until it is initialized. For new created
	 * SessionInfo you need to call sessionInfo.initializeSession()
	 * 
	 * @param sessionName
	 * @param connectProps
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public SessionInfo getOrCreateSessionInfo(I_SessionName sessionName,
			I_SessionProperties connectProps) throws ServiceManagerException {
		synchronized (this.sessionMap) {
			SessionInfo sessionInfo = getSessionInfo(sessionName);
			if (sessionInfo == null) {
				checkNumberOfSessions(connectProps);
				sessionInfo = new SessionInfo(glob, sessionName);
				this.sessionMap.put(sessionInfo.getId(), sessionInfo);
				this.sessionArrCache = null;
			} else {
				final int MAX = 10000;
				int i = 0;
				for (; i < MAX; i++) {
					if (sessionInfo.isInitialized())
						break;
					try {
						Thread.sleep(1L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (i >= MAX) {
					Thread.dumpStack();
					ServiceManagerException ex = new ServiceManagerException(
							glob,
							SysErrorCode.RESOURCE_TEMPORARY_UNAVAILABLE,
							ME,
							"Connection for "
									+ sessionName.getAbsoluteName()
									+ " failed, timeout while waiting for concurrently created same session name, please retry.");
					// The client shall retry (behave like a communication
					// exception)
					throw new ServiceManagerException(
							glob,
							SysErrorCode.COMMUNICATION_RESOURCE_TEMPORARY_UNAVAILABLE,
							ME, "", ex);
				}
			}
			return sessionInfo;
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessionInfo(com.ethercis.common.common.session.I_SessionName)
	 */
	@Override
	public SessionInfo getSessionInfo(I_SessionName sessionName) {
		SessionInfo[] sessions = getSessions();
		for (int ii = 0; ii < sessions.length; ii++) {
			if (sessions[ii].getSessionName().equalsRelative(sessionName)) {
				return sessions[ii];
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getNodeId()
	 */
	@Override
	public final NodeId getNodeId() throws ServiceManagerException {
		if (determineNodeId) {

			determineNodeId = false;

			if (this.subjectName.getLoginName().startsWith(
					Constants.INTERNAL_LOGINNAME_PREFIX_FOR_SERVICES))
				return null; // don't check for internal logins
		}
		return nodeId;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#isCluster()
	 */
	@Override
	public boolean isCluster() throws ServiceManagerException {
		return getNodeId() != null;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSecurityCtx()
	 */
	@Override
	public I_Authenticate getSecurityCtx() {
		return this.securityCtx;
	}

	public void setSecurityCtx(I_Authenticate securityCtx) {
		this.securityCtx = securityCtx;
	}

	/*
	 * Check if this authenticate is permitted to do something <p/>
	 * 
	 * @param String The action the user tries to perfrom
	 * 
	 * @param String whereon the user tries to perform the action
	 * 
	 * EXAMPLE: isAuthorized("PUBLISH", "thisIsAMessageKey");
	 * 
	 * The above line checks if this authenticate is permitted to >>publish<< a
	 * message under the key >>thisIsAMessageKey<<
	 * 
	 * Known action keys: PUBLISH, SUBSCRIBE, GET, ERASE, public boolean
	 * isAuthorized(MethodName actionKey, String key) { if (this.securityCtx ==
	 * null) { log.warn("No authorization for '" + actionKey + "' and msg=" +
	 * key); return false; } return this.securityCtx.isAuthorized(actionKey,
	 * key); }
	 */

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#isLoggedIn()
	 */
	@Override
	public final boolean isLoggedIn() {
		synchronized (this.sessionMap) {
			return this.sessionMap.size() > 0;
		}
		// return getSessions().length > 0;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessions()
	 */
	@Override
	public final SessionInfo[] getSessions() {
		final SessionInfo[] cache = this.sessionArrCache;
		if (cache != null) {
			return cache;
		}
		synchronized (this.sessionMap) {
			if (this.sessionArrCache != null) {
				return this.sessionArrCache;
			}
			this.sessionArrCache = (SessionInfo[]) this.sessionMap.values()
					.toArray(new SessionInfo[this.sessionMap.size()]);
			return this.sessionArrCache;
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getCountSessionsInternal()
	 */
	@Override
	public final int getCountSessionsInternal() {
		int count = 0;
		I_SessionInfo[] arr = getSessions();
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].getSessionName().isPubSessionIdInternal())
				count++;
		}
		return count;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getCountSessionsUser()
	 */
	@Override
	public final int getCountSessionsUser() {
		int count = 0;
		I_SessionInfo[] arr = getSessions();
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].getSessionName().isPubSessionIdUser())
				count++;
		}
		return count;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessionByAbsoluteName(java.lang.String)
	 */
	@Override
	public final I_SessionInfo getSessionByAbsoluteName(String absoluteName) {
		synchronized (this.sessionMap) {
			return (I_SessionInfo) this.sessionMap.get(absoluteName);
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSession(com.ethercis.common.common.session.I_SessionName)
	 */
	@Override
	public final I_SessionInfo getSession(I_SessionName sessionName) {
		synchronized (this.sessionMap) {
			return (I_SessionInfo) this.sessionMap.get(sessionName
					.getAbsoluteName());
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getFirstSession()
	 */
	@Override
	public final I_SessionInfo getFirstSession() {
		SessionInfo[] sessions = getSessions();
		return (sessions.length > 0) ? sessions[0] : null;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#checkNumberOfSessions(com.ethercis.common.common.session.I_SessionProperties)
	 */
	@Override
	public final void checkNumberOfSessions(I_SessionProperties props)
			throws ServiceManagerException {

		if (SessionProperties.DEFAULT_maxSessions != props.getMaxSessions())
			this.maxSessions = props.getMaxSessions();

		int count = getSessions().length;
		if (props.isSessionLimitsPubSessionIdSpecific()) {
			count = props.getSessionName().isPubSessionIdInternal() ? getCountSessionsInternal()
					: getCountSessionsUser();
		}
		if (count >= this.maxSessions) {
			log.warn(ME + ": Max sessions = " + this.maxSessions + " for user "
					+ getLoginName() + "@" + props.getClientIp()
					+ " exhausted, login denied.");
			throw new ServiceManagerException(glob,
					SysErrorCode.USER_CONFIGURATION_MAXSESSION, ME,
					"Max sessions = " + this.maxSessions
							+ " exhausted, login denied.");
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessionsToClear(com.ethercis.common.common.session.I_SessionProperties)
	 */
	@Override
	public I_SessionInfo[] getSessionsToClear(I_SessionProperties q) {
		if (q.clearSessions() == true && getNumSessions() > 0) {
			SessionInfo[] arr = getSessions();
			if (q.isSessionLimitsPubSessionIdSpecific()) {
				// Special case: only destroy pubSessionId<0 if we are also <0
				// and vice versa
				boolean isInternal = q.getSessionName()
						.isPubSessionIdInternal();
				ArrayList<SessionInfo> list = new ArrayList<SessionInfo>(
						arr.length);
				for (int i = 0; i < arr.length; i++) {
					if (isInternal == arr[i].getSessionName()
							.isPubSessionIdInternal())
						list.add(arr[i]);
				}
				log.warn(getId() + " clear " + list.size()
						+ " sessions, isInternal=" + isInternal
						+ " sessions, max=" + getNumSessions() + " reached");
				return (I_SessionInfo[]) list
						.toArray(new SessionInfo[list.size()]);
			} else {
				log.warn(getId() + " clear " + arr.length + " sessions, max="
						+ getNumSessions() + " reached");
				return arr;
			}
		}
		return new I_SessionInfo[0];
	}

	/**
	 * Get notification that the client did a login.
	 * <p />
	 * This instance may exist before a login was done, for example when some
	 * messages where directly addressed to this client.<br />
	 * This notifies about a client login.
	 */
	public final void notifyAboutLogin(SessionInfo sessionInfo)
			throws ServiceManagerException {
		if (!isAlive()) { // disconnect() and connect() are not synchronized, so
							// this can happen
			throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, ME,
					"SubjectInfo is shutdown, try to login again");
		}

		log.debug(ME + ": notifyAboutLogin(" + sessionInfo.getSecretSessionId()
				+ ")");
		synchronized (this.sessionMap) {
			this.sessionMap.put(sessionInfo.getId(), sessionInfo);
			this.sessionArrCache = null;
		}

	}

	/**
	 * Get notification that the client did a logout. <br />
	 * Note that the loginName is not reset.
	 * 
	 * @param absoluteSessionName
	 *            == sessionInfo.getId()
	 * @param clearQueue
	 *            Shall the message queue of the client be cleared&destroyed logonservice
	 *            well (e.g. disconnectQos.deleteSubjectQueue())?
	 * @param forceShutdownEvenIfEntriesExist
	 *            on last session
	 */
	public final void notifyAboutLogout(String absoluteSessionName,
			boolean forceShutdownEvenIfEntriesExist) throws ServiceManagerException {
		if (!isAlive()) { // disconnect() and connect() are not synchronized, so
							// this can happen
			throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, ME,
					"SubjectInfo is shutdown, no logout");
		}
		log.debug(ME + ": Entering notifyAboutLogout(" + absoluteSessionName
				+ ")");
		I_SessionInfo sessionInfo = null;
		synchronized (this.sessionMap) {
			sessionInfo = (I_SessionInfo) sessionMap.remove(absoluteSessionName);
			this.sessionArrCache = null;
		}

		// if (!isLoggedIn()) {
		// if (clearQueue || getSubjectQueue().getNumOfEntries() < 1) {
		shutdown(forceShutdownEvenIfEntriesExist); // Does shutdown only on last
													// session
		// }
		// }
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSubjectName()
	 */
	@Override
	public final I_SessionName getSubjectName() {
		return this.subjectName;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getId()
	 */
	@Override
	public final String getId() {
		return this.subjectName.getAbsoluteName();
	}

	/**
	 * @see #getId
	 */
	public final String toString() {
		return this.subjectName.getAbsoluteName();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getLoginName()
	 */
	@Override
	public final String getLoginName() {
		return this.subjectName.getLoginName();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessionByPubSessionId(long)
	 */
	@Override
	public I_SessionInfoProtector getSessionByPubSessionId(long pubSessionId) {
		I_SessionInfo sessionInfo = getSessionByPublicId(pubSessionId);
		return (sessionInfo == null) ? null : sessionInfo
				.getSessionInfoProtector();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#killClient()
	 */
	@Override
	public String killClient() throws ServiceManagerException {
		int numSessions = getNumSessions();

		if (numSessions < 1) {
			shutdown(true);
			return getId() + " killed. No sessions where available";
		}
		String sessionList = getSessionList();
		while (true) {
			I_SessionInfo sessionInfo = null;
			synchronized (sessionMap) {
				Iterator<SessionInfo> iterator = sessionMap.values().iterator();
				if (!iterator.hasNext())
					break;
				sessionInfo = iterator.next();
			}
			sessionInfo.killSession();
		}

		return getId() + " sessions " + sessionList + " killed.";
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @return internal state of SubjectInfo logonservice a XML ASCII string
	 */
	public final String toXml() {
		return toXml((String) null);
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *            indenting of tags for nice response
	 * @return internal state of SubjectInfo logonservice a XML ASCII string
	 */
	public final String toXml(String extraOffset) {
		StringBuffer sb = new StringBuffer(256);
		if (extraOffset == null)
			extraOffset = "";
		String offset = Constants.OFFSET + extraOffset;

		sb.append(offset).append("<SubjectInfo id='")
				.append(this.subjectName.getAbsoluteName()).append("'>");
		sb.append(offset).append(" <state>").append(getStateStr())
				.append("</state>");
		if (isAlive()) {
			sb.append(offset).append(" <subjectId>").append(getLoginName())
					.append("</subjectId>");
			SessionInfo[] sessions = getSessions();
			for (int i = 0; i < sessions.length; i++) {
				SessionInfo sessionInfo = sessions[i];
				sb.append(sessionInfo.toXml(extraOffset + Constants.INDENT,
						(Properties) null));
			}
		}
		sb.append(offset).append("</SubjectInfo>");

		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessionByPublicId(long)
	 */
	@Override
	public final I_SessionInfo getSessionByPublicId(long publicSessionId) {
		if (publicSessionId == 0L) {
			return null;
		}
		I_SessionName sessionName = new SessionName(glob, subjectName,
				publicSessionId);
		synchronized (this.sessionMap) {
			return (I_SessionInfo) this.sessionMap.get(sessionName
					.getAbsoluteName());
		}
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#isUndef()
	 */
	@Override
	public final boolean isUndef() {
		return this.state == UNDEF;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#isAlive()
	 */
	@Override
	public final boolean isAlive() {
		return this.state == ALIVE;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#isDead()
	 */
	@Override
	public final boolean isDead() {
		return this.state == DEAD;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getStateStr()
	 */
	@Override
	public final String getStateStr() {
		if (isAlive()) {
			return "ALIVE";
		} else if (isDead()) {
			return "DEAD";
		} else if (isUndef()) {
			return "UNDEF";
		} else {
			return "INTERNAL_ERROR";
		}
	}

	// =========== Enforced by I_AdminSubject and SubjectInfoProtector.java
	// ================
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getUptime()
	 */
	@Override
	public long getUptime() {
		return (System.currentTimeMillis() - this.startupTime) / 1000L;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getCreationDate()
	 */
	@Override
	public final String getCreationDate() {
		long ll = this.startupTime;
		java.sql.Timestamp tt = new java.sql.Timestamp(ll);
		return tt.toString();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getNumUpdate()
	 */
	@Override
	public long getNumUpdate() {
		// long numUpdates = this.dispatchStatistic.getNumUpdate(); // The
		// sessions which disappeared already are remembered here
		// SessionInfo[] sessions = getSessions();
		// for (int i=0; i<sessions.length; i++) {
		// SessionInfo sessionInfo = sessions[i];
		// numUpdates += sessionInfo.getNumUpdate();
		// }
		// return numUpdates;
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#isBlockClientLogin()
	 */
	@Override
	public boolean isBlockClientLogin() {
		return blockClientLogin;
	}

	public String setBlockClientLogin(boolean blockClient) {
		if (this.blockClientLogin == blockClient)
			return "Client is alread in state blocking=" + blockClient;
		this.blockClientLogin = blockClient;
		String text = blockClient ? "" + getNumAliveSessions()
				+ " ALIVE clients remain logged in, new ones are blocked"
				: "Blocking of " + getId() + " is switched off";
		log.info(text);
		if (blockClient == false) {
			SessionInfo[] sessionInfos = getSessions();
			for (int i = 0; i < sessionInfos.length; i++) {
				sessionInfos[i].setBlockClientSessionLogin(false);
			}
		}
		return text;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#blockClientAndResetConnections()
	 */
	@Override
	public String blockClientAndResetConnections() {
		setBlockClientLogin(true);
		StringBuffer buf = new StringBuffer(512);
		SessionInfo[] sessionInfos = getSessions();
		for (int i = 0; i < sessionInfos.length; i++) {
			if (i > 0)
				buf.append("\n");
			String ret = sessionInfos[i].disconnectClientKeepSession();
			buf.append(ret);
		}
		if (buf.length() < 1)
			buf.append("No client connections to shutdown");
		log.info(buf.toString());
		return buf.toString();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getNumSessions()
	 */
	@Override
	public int getNumSessions() {
		return getSessions().length;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getNumAliveSessions()
	 */
	@Override
	public int getNumAliveSessions() {
		int countAlive = 0;
		I_SessionInfo[] sessionInfos = getSessions();
		for (int i = 0; i < sessionInfos.length; i++) {
			if (sessionInfos[i].isAlive())
				countAlive++;
		}
		return countAlive;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getMaxSessions()
	 */
	@Override
	public int getMaxSessions() {
		return this.maxSessions;
	}

	/**
	 * JMX access.
	 * 
	 * @param Change
	 *            the max allowed simultaneous logins of this user
	 */
	public void setMaxSessions(int max) {
		this.maxSessions = max;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getSessionList()
	 */
	@Override
	public String getSessionList() {
		int numSessions = getNumSessions();
		if (numSessions < 1)
			return "";
		StringBuffer sb = new StringBuffer(numSessions * 30);
		I_SessionInfo[] sessions = getSessions();
		for (int i = 0; i < sessions.length; i++) {
			if (sb.length() > 0)
				sb.append(",");
			sb.append(sessions[i].getPublicSessionId());
		}
		return sb.toString();
	}

	/** JMX */
	public String usage() {
		return RunTimeSingleton.getJmxUsageLinkInfo(this.getClass().getName(),
                null);
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getUsageUrl()
	 */
	@Override
	public String getUsageUrl() {
		return RunTimeSingleton.getJavadocUrl(this.getClass().getName(), null);
	}

	/* JMX dummy to have a copy/paste functionality in jconsole */
	public void setUsageUrl(String url) {
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.session.I_SubjectInfo#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		String[] types = new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE };
		String name = AttributeChangeNotification.class.getName();
		String description = "TODO: An attribute of this MBean has changed";
		MBeanNotificationInfo info = new MBeanNotificationInfo(types, name,
				description);
		return new MBeanNotificationInfo[] { info };
	}
}

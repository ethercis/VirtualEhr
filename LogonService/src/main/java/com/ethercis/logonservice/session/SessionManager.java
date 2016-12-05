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

/*------------------------------------------------------------------------------
Name:      SessionManager.java
Comment:   Session Management for clients
------------------------------------------------------------------------------*/

//Copyright
package com.ethercis.logonservice.session;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_RunlevelListener;
import com.ethercis.servicemanager.runlevel.RunlevelManager;
import com.ethercis.servicemanager.common.IsoDateParser;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Manager;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_SubjectInfo;
import com.ethercis.servicemanager.common.session.*;
import com.ethercis.logonservice.access.ConnectProperties;
import com.ethercis.logonservice.security.SecurityProperties;
import com.ethercis.logonservice.security.ServiceSecurityManager;
import com.ethercis.servicemanager.service.I_Service;
import com.ethercis.servicemanager.service.ServiceRegistry;
//import com.ethercis.sessionlogger.I_SessionLoggerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;



/**
 * Authenticate a client via login.
 * <p>
 * The login method serves as a factory for a ehrserver.Server Reference
 */
final public class SessionManager implements I_RunlevelListener, com.ethercis.servicemanager.common.session.I_Authenticate
{
    final private String ME;

    /** Unique counter to generate IDs */
    private long counter = 1;

    private final RunTimeSingleton glob;
    private static Logger log = LogManager.getLogger(SessionManager.class.getName());
//    private I_SessionLoggerService sessionLoggerService;

    /**
     * With this map you can find a client using a sessionId.
     *
     * key   = sessionId A unique identifier
     * value = SessionInfo object, containing all data about a client
     */
    final private Map sessionInfoMap = new HashMap();

    /**
     * With this map you can find a client using his login name.
     *
     * key   = loginName, the unique login name of a client
     * value = SessionInfo object, containing all data about a client
     */
    final private Map loginNameSubjectInfoMap = new HashMap();

    /**
     * For listeners who want to be informed about login/logout
     */
    final private Set clientListenerSet = new HashSet();

    private boolean acceptWrongSenderAddress;

    // My security delegate layer which is exposed to the protocol plugins
    //private final AuthenticateProtector encapsulator;

    /**
     */
    public SessionManager(RunTimeSingleton global) throws ServiceManagerException
    {
        this.glob = global;

        this.ME = "Authenticate" + glob.getLogPrefixDashed();

        log.debug("Entering constructor");

        glob.getRunlevelManager().addRunlevelListener(this);

        // TODO: Decide by authorizer, see SessionInfo.java with specific setting
        this.acceptWrongSenderAddress = glob.getProperty().get("ehrserver/acceptWrongSenderAddress", false);
    }

    /**
     * Just to testing sync
     * @return
     */
    public Map getSessionInfoMap() {
        return this.sessionInfoMap;
    }



    public RunTimeSingleton getGlobal()
    {
        return this.glob;
    }


    public String login(String loginName, String passwd,
                        String xmlQoS_literal, String secretSessionId)
            throws ServiceManagerException
    {
        Thread.dumpStack();
        log.fatal("login() not implemented");
        throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_NOTIMPLEMENTED, ME, "login() not implemented and deprecated");
    }

    /**
     * Use this to create a user and session for internal users only.
     * This method is a security risk never allow external code to call it (there is no
     * passwd needed).
     * Note that the security instances are created rawish,
     * they are not registered with the Authentication server.
     */
    public SessionInfo unsecureCreateSession(SessionProperties props) throws ServiceManagerException
    {
        SessionName sessionName = props.getSessionName();
        log.debug("Entering unsecureCreateSession(" + sessionName + ")");
        String secretSessionId = createSessionId(sessionName.getLoginName());
        ServiceSecurityManager manager = new ServiceSecurityManager();
        manager.init(glob, null);
        I_Session session = new Session(manager, secretSessionId);
        I_SecurityProperties securityProps = new SecurityProperties(this.glob, sessionName.getLoginName(), "");
        session.initializeSession(securityProps);
        I_Authenticate subject = session.getAuthenticate();

        SubjectInfo subjectInfo = null;
        if (sessionName.getLoginName().startsWith("__")) { // __RequestBroker_internal
            // strip the pubSessionId and create a subjectInfo ...
            SessionName subjectName = new SessionName(glob, sessionName.getNodeId(), sessionName.getLoginName());
            subjectInfo = new SubjectInfo(getGlobal(), this, subjectName);
            synchronized(this.loginNameSubjectInfoMap) {
                this.loginNameSubjectInfoMap.put(subjectInfo.getLoginName(), subjectInfo);
            }
            subjectInfo.toAlive(subject);
        }
        else {
            subjectInfo = getOrCreateSubjectInfoByName(sessionName, false, subject);
        }

        ConnectProperties connectprops = new ConnectProperties(glob, glob.getNodeId());
        SessionInfo sessionInfo = subjectInfo.getOrCreateSessionInfo(sessionName, connectprops);
        if (!sessionInfo.isInitialized()) {
            sessionInfo.init(subjectInfo, session, connectprops);
        }

        return sessionInfo;
    }

    /**
     * Login to service.
     */
    public SessionProperties connect(I_ConnectProperties sessionprops) throws ServiceManagerException
    {
        return connect(sessionprops, null);
    }

    /**
     * Login to ehrserver.
     *
     * If no secretSessionId==null, the secretSessionId from properties is used,
     * if this is null as well, we generate one.
     * <p />
     * The given secretSessionId (in the properties) from the client could be from the service verifies it.
     * <p />
     *
     * @param connectProps  The login/connect QoS, see ConnectQosServer.java
     * @param secretSessionId   The caller (here CORBA-POA protocol driver) may insist to you its own secretSessionId
     */
    public SessionProperties connect(I_ConnectProperties connectProps, String secretSessionId) throws ServiceManagerException
    {
        if (connectProps.getSessionName().getLoginName().equals(this.glob.getId())) {
            String text = "You are not allowed to login with the cluster node name " + connectProps.getSessionName().toString() + ", access denied.";
            log.warn(text);
            throw new ServiceManagerException(glob, SysErrorCode.USER_CONFIGURATION_IDENTICALCLIENT, ME+".connect()", text);
        }

        { // Administrative block clients
            I_SubjectInfo si = getSubjectInfoByName(connectProps.getSessionName());
            if (si != null && si.isBlockClientLogin()) {
                // Future todo: Throw out existing session, not only block new
                // logins
                log.warn("Access for " + connectProps.getSessionName().toString() + " is blocked and denied (jconsole->blockClientLogin)");
                throw new ServiceManagerException(glob, SysErrorCode.COMMUNICATION_NOCONNECTION_SERVERDENY, ME + ".connect()",
                        "Access for "
                                + connectProps.getSessionName().toString()
                                + " is currently not possible, please contact the server administrator");
            }
            {
                I_SessionInfo sesi = getSessionInfoByName((SessionName) connectProps.getSessionName());
                if (sesi != null && sesi.isBlockClientSessionLogin()) {
                    // Future todo: Throw out existing session, not only block new
                    // logins
                    log.warn("Access for " + connectProps.getSessionName().toString()
                            + " is blocked and denied (jconsole->blockClientSessionLogin)");
                    throw new ServiceManagerException(glob, SysErrorCode.COMMUNICATION_NOCONNECTION_SERVERDENY, ME + ".connect()",
                            "Access for " + connectProps.getSessionName().toString()
                                    + " session is currently not possible, please contact the server administrator");
                }
            }
        }


        // [1] Try reconnecting with secret sessionId
        try {
            //TODO: check if the same credential is used too!
            log.debug("Entering connect(sessionName=" + connectProps.getSessionName().getAbsoluteName() + ")"); // " secretSessionId=" + secretSessionId + ")");
            log.debug("ConnectQos=" + connectProps.toXml());

            // Get or create the secretSessionId (we preserve a user supplied secretSessionId) ...
            if (secretSessionId == null || secretSessionId.length() < 2) {
                secretSessionId = connectProps.getSecretSessionId();
                if (secretSessionId != null && secretSessionId.length() >= 2)
                    log.info(connectProps.getSessionName().getAbsoluteName() + " is using secretSessionId '" + secretSessionId + "' from ConnectQos");
            }
            if (secretSessionId != null && secretSessionId.length() >= 2) {
                SessionInfo info = getSessionInfo(secretSessionId);
                if (info != null) {  // authentication succeeded

                    updateConnectProperties(info, (ConnectProperties) connectProps);

                    SessionProperties returnProps = new SessionProperties(glob, glob.getNodeId());
                    returnProps.setSecretSessionId(secretSessionId);
                    returnProps.setSessionName(info.getSessionName());
                    returnProps.setReconnected(true);
                    returnProps.getClientProperties().addClientProperty(Constants.CLIENTPROPERTY_RCVTIMESTAMPSTR, IsoDateParser.getCurrentUTCTimestampNanos());
                    log.info("Reconnected with given secretSessionId.");
                    return returnProps;
                }
            }
        }
        catch (Throwable e) {
            log.fatal("Internal error when trying to reconnect to session " + connectProps.getSessionName() + " with secret session ID: " + e.toString());
            e.printStackTrace();
            throw ServiceManagerException.convert(glob, ME, SysErrorCode.INTERNAL_CONNECTIONFAILURE.toString(), e);
        }

        // [2] Try reconnecting with publicSessionId
        if (connectProps.getSessionProperties().hasPublicSessionId()) {
            I_SessionInfo info = getSessionInfo((SessionName) connectProps.getSessionName());
            if (info != null && !info.isShutdown() && !info.getConnectProperties().bypassCredentialCheck()) {
                if (connectProps.reconnectSameClientOnly()) {
                    String text = "Only the creator of session " + connectProps.getSessionName().toString() + " may reconnect, access denied.";
                    log.warn(text);
                    throw new ServiceManagerException(glob, SysErrorCode.USER_CONFIGURATION_IDENTICALCLIENT, ME+".connect()", text);
                }
                try {
                    // Check password as we can't trust the public session ID
                    I_SecurityProperties retprops = info.getSecuritySession().initializeSession(connectProps.getSecurityProperties());
                    boolean ok = info.getSecuritySession().verify(connectProps.getSecurityProperties());
                    if (!ok)
                        throw new ServiceManagerException(glob, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED,
                                ME, "Access denied for " + connectProps.getSecurityProperties().getUserId());

                    String oldSecretSessionId = info.getSecretSessionId();

                    if (secretSessionId == null || secretSessionId.length() < 2) {
                        // Keep the old secretSessionId
                        connectProps.setSecretSessionId(oldSecretSessionId);
                    }
                    else {
                        // we insist in a new secretSessionId
                        changeSecretSessionId(oldSecretSessionId, secretSessionId);
                        connectProps.setSecretSessionId(secretSessionId);
                    }

                    updateConnectProperties((SessionInfo)info, (ConnectProperties)connectProps); // fires event

                    SessionProperties returnprops = new SessionProperties(glob, glob.getNodeId());
                    returnprops.setSessionName(info.getSessionName());
                    returnprops.setReconnected(true);
                    returnprops.getClientProperties().addClientProperty(Constants.CLIENTPROPERTY_RCVTIMESTAMPSTR, IsoDateParser.getCurrentUTCTimestampNanos());
                    log.info("Reconnected with given publicSessionId to '" + info.getSessionName() + "'.");
                    return returnprops;
                }
                catch (ServiceManagerException e) {
                    log.warn("Access is denied when trying to reconnect to session " + info.getSessionName() + ": " + e.getMessage());
                    throw e; // Thrown if authentication failed
                }
                catch (Throwable e) {
                    log.fatal("Internal error when trying to reconnect to session " + info.getSessionName() + " with public session ID: " + e.toString());
                    e.printStackTrace();
                    throw ServiceManagerException.convert(glob, ME, SysErrorCode.INTERNAL_CONNECTIONFAILURE.toString(), e);
                }
            }
        }

        // [3] Generate a secret session ID
        if (secretSessionId == null || secretSessionId.length() < 2) {
            secretSessionId = createSessionId(connectProps.getSecurityProperties().getUserId());
            connectProps.setSecretSessionId(secretSessionId); // assure consistency
            log.debug("Empty secretSessionId - generated secretSessionId=" + secretSessionId);
        }

        I_Session sessionCtx = null;
        I_Manager securityMgr = null;
        SessionInfo sessionInfo = null;

        // [4] Authenticate new client with password
        try {
            // Get suitable SecurityManager and context ...
            // The security manager is hardcoded for this implementation!!!
            securityMgr = (I_Manager)glob.getServiceRegistry().getService(Constants.DEFAULT_SERVICE_SECURITY_MANAGER_ID+","+Constants.DEFAULT_SERVICE_SECURITY_MANAGER_VERSION);
//         securityMgr = (I_Manager)glob.getServiceManager().getServiceObject(connectProps.getClientPluginType(), connectProps.getClientPluginVersion());
            if (securityMgr == null) {
                log.warn("Access is denied, there is no security manager configured for this connection: " + connectProps.toXml());
                throw new ServiceManagerException(glob, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME, "There is no security manager configured with the given connect QoS");
            }
            sessionCtx = securityMgr.reserveSession(secretSessionId);  // always creates a new I_Session instance
//            I_SecurityProperties retprops = sessionCtx.initializeSession(connectProps.getSecurityProperties());
            if (connectProps.bypassCredentialCheck()) {
                // This happens when a session is auto created by a PtP message
                // Only ConnectQosServer (which is under control of the core) can set this flag
                log.debug("SECURITY SWITCH OFF: Granted access to server without password, bypassCredentialCheck=true");
            }
            else {
                sessionCtx.initializeSession(connectProps.getSecurityProperties()); // throws ServiceManagerExceptions if authentication fails
//            if (securityInfo != null && securityInfo.length() > 1) log.warn("Ignoring security info: " + securityInfo);
            }
            // Now the client is authenticated
        }
        catch (ServiceManagerException e) {
            // If access is denied: cleanup resources
            log.warn("Access is denied: " + e.getMessage() );
            if (securityMgr != null) securityMgr.releaseSession(secretSessionId, null);  // Always creates a new I_Session instance
            throw e;
        }
        catch (Throwable e) {
            log.fatal("PANIC: Access is denied: " + e.getMessage() + "\n" + RunTimeSingleton.getStackTraceAsString(e));
            e.printStackTrace();
            // On error: cleanup resources
            securityMgr.releaseSession(secretSessionId, null);  // Always creates a new I_Session instance
            throw ServiceManagerException.convert(glob, ME, SysErrorCode.INTERNAL_CONNECTIONFAILURE.toString(), e);
        }

        log.debug("Checking if user is known ...");
        SubjectInfo subjectInfo = null;
        try {
      /*
         // Check if user is known, otherwise create an entry ...
         I_Subject subjectCtx = sessionCtx.getAuthenticate();
         SessionName subjectName = new SessionName(glob, connectQos.getSessionName(), 0L); // Force to be of type authenticate (no public session ID)

         boolean subjectIsAlive = false;
         synchronized(this.loginNameSubjectInfoMap) { // Protect against two simultaneous logins
            subjectInfo = (SubjectInfo)this.loginNameSubjectInfoMap.get(subjectName.getLoginName());
            //log.error(ME, "DEBUG ONLY, subjectName=" + subjectName.toString() + " loginName=" + subjectName.getLoginName() + " state=" + toXml());
            if (subjectInfo == null) {
               subjectInfo = new SubjectInfo(getGlobal(), this, subjectName);
               this.loginNameSubjectInfoMap.put(subjectInfo.getLoginName(), subjectInfo); // Protect against two simultaneous logins
            }

            subjectIsAlive = subjectInfo.isAlive();
         } // synchronized(this.loginNameSubjectInfoMap)

         if (!subjectInfo.isAlive()) {
            try {
               subjectInfo.toAlive(subjectCtx, connectQos.getSubjectQueueProperty());
            }
            catch(Throwable e) {
               synchronized(this.loginNameSubjectInfoMap) {
                  this.loginNameSubjectInfoMap.remove(subjectInfo.getLoginName());
               }
               throw e;
            }
         }
         */

            // [5] New client is authenticated, create the SessioInfo
            boolean returnLocked = true;
            subjectInfo = getOrCreateSubjectInfoByName((SessionName) connectProps.getSessionName(),
                    returnLocked, sessionCtx.getAuthenticate());
            try { // subjectInfo.getLock().release())
                if (subjectInfo.isAlive()) {
                    ; // overwrites only if not null
                }
                // Check if client does a relogin and wants to destroy old sessions
                if (connectProps.clearSessions() == true) {
                    I_SessionInfo[] sessions = subjectInfo.getSessionsToClear(connectProps.getSessionProperties());
                    if (sessions.length > 0) {
                        for (int i=0; i<sessions.length; i++ ) {
                            I_SessionInfo si = sessions[i];
                            log.warn("Destroying session '" + si.getSecretSessionId() + "' of user '" + subjectInfo.getSubjectName() + "' as requested by client");
                            disconnect(si.getSecretSessionId(), (String)null);
                        }
                        // will create a new SubjectInfo instance (which should be OK)
                        return connect(connectProps, secretSessionId);
                    }
                }

                log.debug("Creating sessionInfo for " + subjectInfo.getId());

                // A PtP with forceQueuing=true and a simultaneous connect of the same
                // client: This code is thread safe with new SessionInfo() below
                // to avoid duplicate SessionInfo
                sessionInfo = getOrCreateSessionInfo(connectProps.getSessionName(), connectProps.getSessionProperties());
                if (sessionInfo.isInitialized() &&
                        !sessionInfo.isShutdown() && sessionInfo.getConnectProperties().bypassCredentialCheck()) {
                    log.debug("connect: Reused session with had bypassCredentialCheck=true");
                    String oldSecretSessionId = sessionInfo.getSecretSessionId();
                    sessionInfo.setSecuritySession(sessionCtx);
                    if (secretSessionId == null || secretSessionId.length() < 2) {
                        // Keep the old secretSessionId
                        connectProps.setSecretSessionId(oldSecretSessionId);
                    }
                    else {
                        // The CORBA driver insists in a new secretSessionId
                        changeSecretSessionId(oldSecretSessionId, secretSessionId);
                        connectProps.setSecretSessionId(secretSessionId);
                    }
                    updateConnectProperties(sessionInfo, connectProps);
                }
                else {
                    // Create the new sessionInfo instance
                    log.debug("connect: sessionId='" + secretSessionId + "' connectQos='"  + connectProps.toXml() + "'");
                    sessionInfo.init(subjectInfo, sessionCtx, connectProps);
                    synchronized(this.sessionInfoMap) {
                        this.sessionInfoMap.put(secretSessionId, sessionInfo);
                    }
                }

                connectProps.getSessionProperties().setSecretSessionId(secretSessionId);
                connectProps.getSessionProperties().setSessionName(sessionInfo.getSessionName());
                subjectInfo.notifyAboutLogin(sessionInfo);
                fireClientEvent(sessionInfo, true);
            }
            finally {
                if (subjectInfo != null) subjectInfo.getLock().unlock();
            }

            // --- compose an answer -----------------------------------------------
            SessionProperties returnprops = new SessionProperties(glob, glob.getNodeId());
            returnprops.setSecretSessionId(secretSessionId); // securityInfo is not coded yet !
            returnprops.setSessionName(sessionInfo.getSessionName());
            returnprops.getClientProperties().addClientProperty(Constants.CLIENTPROPERTY_RCVTIMESTAMPSTR, IsoDateParser.getCurrentUTCTimestampNanos());
            returnprops.getClientProperties().addClientProperty(I_SessionManager.SESSIONS_IN_USE, subjectInfo.getNumSessions());
            returnprops.getClientProperties().addClientProperty(I_SessionManager.MAX_SESSION, subjectInfo.getMaxSessions());
            returnprops.getClientProperties().addClientProperty(I_SessionManager.SESSION_TIMEOUT, connectProps.getSessionProperties().getSessionTimeout());

            // Now some nice logging ...
            StringBuffer sb = new StringBuffer(256);
            if (connectProps.bypassCredentialCheck())
                sb.append("Created temporary and unsafe session for client ");
            else
                sb.append("Successful login for client ");
            sb.append(sessionInfo.getSessionName().getAbsoluteName());
            sb.append(", session");
            sb.append(((connectProps.getSessionProperties().getSessionTimeout() > 0L) ?
                    " expires after"+TimeStamp.millisToNice(connectProps.getSessionProperties().getSessionTimeout()) :
                    " lasts forever"));
            sb.append(", ").append(subjectInfo.getNumSessions()).append(" of ");
            sb.append(connectProps.getSessionProperties().getMaxSessions()).append(" sessions are in use.");
            log.info(sb.toString());
            log.debug(toXml());
            log.debug("Returned QoS:\n" + returnprops.toXml());
            log.debug("Leaving connect()");

            return returnprops;
        }
        catch (ServiceManagerException sme) {
            String id = (sessionInfo != null) ? sessionInfo.getId() : ((subjectInfo != null) ? subjectInfo.getId() : "");
            log.warn("Connection for " + id + " failed: " + sme.getMessage());
            // e.g. by TestPersistentSession.java
            // persistence/session/maxEntriesCache=1
            // persistence/session/maxEntries=2
            if (!sme.getErrorCode().isOfType(SysErrorCode.USER_SECURITY_AUTHENTICATION)) {
                // E.g. if sessionStore overflow: we don't want the client polling
                //e.changeSysErrorCode(SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED);
                sme = new ServiceManagerException(glob, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED,
                        ME, "Access to ehrserver denied", sme);
            }
            sme.setCleanupSession(true);
            // cleanup delayed to give our throw return a chance to reach client before the socket is closed
            // Too dangerous: The stale SessionInfo could be reaccessed during the sleep
            // There for we do the delay in CallbackSocketDriver ...
            //disconnectDelayed(secretSessionId, (String)null, 5000, e); // cleanup
            try {
                disconnect(secretSessionId, (String) null);
            }
            catch (Exception e1){
                ; //do nothing...
            }
            throw sme; //this exception is the one we want to see...
        }
        catch (Throwable t) {
            t.printStackTrace();
            log.fatal("Internal error: Connect failed: " + t.getMessage());
            //disconnectDelayed(secretSessionId, (String)null, 10000, t); // cleanup
            disconnect(secretSessionId, (String)null);
            // E.g. if NPE: we don't want the client polling: Should we change to USER_SECURITY_AUTHENTICATION_ACCESSDENIED?
            ServiceManagerException e = ServiceManagerException.convert(glob, ME, SysErrorCode.INTERNAL_CONNECTIONFAILURE.toString(), t);
            e.setCleanupSession(true);
            throw e;
        }
    }

   /*
    * Probably dangerous as the sessionInfo is visible and could be found
    * by a reconnecting client and the it is suddenly destroyed after the delay
   private void disconnectDelayed(final String secretSessionId, final String qos_literal, long delay, final Throwable reason) {
      Timeout timeout = new Timeout("DisconnectTimer", true);
      timeout.addTimeoutListener(new I_Timeout() {
         public void timeout(Object userData) {
            try {
               disconnect(secretSessionId, qos_literal); // cleanup delayed to give our throw return a chance to reach client
            }
            catch (ServiceManagerException e) {
               e.printStackTrace();
               log.warn("Ignoring problems during cleanup of exception: " + e.getMessage() + ((reason==null) ? "" : (": " + reason.getMessage())));
            }
         }
      }, delay, secretSessionId);
   }
    */

    public void disconnect(String secretSessionId, String literal) throws ServiceManagerException {
        try {
            log.debug("Entering disconnect()");
            //Thread.currentThread().dumpStack();
            log.debug(toXml().toString());
            if (secretSessionId == null) {
                throw new ServiceManagerException(glob, SysErrorCode.USER_ILLEGALARGUMENT, ME, "disconnect() failed, the given secretSessionId is null");
            }

            SessionInfo sessioninfo = check(secretSessionId);
            if (sessioninfo == null){
                log.fatal("Access denied, internal error...");
                throw new ServiceManagerException(glob, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME, "Supplied session id is unknown");
            }
            I_Session sessionSecCtx = sessioninfo.getSecuritySession();
            I_Manager securityMgr = sessionSecCtx.getManager();

            sessionSecCtx = securityMgr.getSessionById(secretSessionId);

            if (sessionSecCtx == null) {
                throw new ServiceManagerException(this.glob, SysErrorCode.USER_NOT_CONNECTED, ME + " Authenticate.disconnect", "You are not connected, your secretSessionId is invalid.");
            }


//         try {
//            CryptContextHolder dataHolder = new CryptContextHolder(MethodName.DISCONNECT, new MsgUnitRaw(null, (byte[])null, qos_literal), null);
//            securityMgr.releaseSession(secretSessionId, sessionSecCtx.);
//         }
//         catch(Throwable e) {
//            log.warn("Ignoring importMessage() problems, we continue to cleanup resources: " + e.getMessage());
//         }

            SessionInfo sessionInfo = getSessionInfo(secretSessionId);

            I_SubjectInfo subjectInfo = sessionInfo.getSubjectInfo();

            SessionProperties disconnectProps = new SessionProperties(glob, glob.getNodeId());

            boolean forceShutdownEvenIfEntriesExist = false;

            resetSessionInfo(sessionInfo, forceShutdownEvenIfEntriesExist, true);

            if (disconnectProps.clearSessions() == true && subjectInfo.getNumSessions() > 0) {
                //Specific deleting for pubSessionId< or >0 not yet implemented
                //SessionInfo[] sessions = subjectInfo.getSessionsToClear(connectQos);
                I_SessionInfo[] sessions = subjectInfo.getSessions();
                for (int i=0; i<sessions.length; i++ ) {
                    I_SessionInfo si = sessions[i];
                    log.warn("Destroying session '" + si.getSecretSessionId() + "' of user '" + subjectInfo.getSubjectName() + "' as requested by client");
                    disconnect(si.getSecretSessionId(), null);
                }
            }

            ServiceRegistry services = glob.getServiceRegistry();
//            I_SessionLoggerService sessionLoggerService = (I_SessionLoggerService) services.getService("SessionLoggerService" + "," + "1.0");
//
//            if (sessionLoggerService == null){
//                log.warn("SessionLoggerService could not be resolved, no session logging will be done...");
//            }
//            else
//                sessionLoggerService.delete(secretSessionId);

            log.debug(toXml().toString());
            log.debug("Leaving disconnect()");
        }
        catch (ServiceManagerException e) {
            log.debug("disconnect failed: " + e.getMessage());
            throw e;
        }
        catch (Throwable e) {
            e.printStackTrace();
            log.fatal("Internal error: Disconnect failed: " + e.getMessage());
            throw ServiceManagerException.convert(glob, ME, SysErrorCode.INTERNAL_DISCONNECT.toString(), e);
        }
    }

    public void disconnect(String secretSessionId) throws ServiceManagerException {
        disconnect(secretSessionId,"");
    }

    /**
     * Access a subjectInfo with the unique login name.
     * <p />
     * If the client is yet unknown, there will be instantiated a dummy SubjectInfo object
     * @param returnLocked true: The SubjectInfo is locked
     * @param prop Can be null
     * @return the SubjectInfo object, is never null
     * @exception the SubjectInfo object is never locked in such a case
     */
    public final SubjectInfo getOrCreateSubjectInfoByName(SessionName subjectName, boolean returnLocked, I_Authenticate subjectCtx) throws ServiceManagerException
    {
        if (subjectName == null || subjectName.getLoginName().length() < 2) {
            log.warn("Given loginName is null");
            throw new ServiceManagerException(this.glob, SysErrorCode.USER_ILLEGALARGUMENT, ME + ".InvalidClientName", "Your given loginName is null or shorter 2 chars, loginName rejected");
        }

        SubjectInfo subjectInfo = null;
        boolean isNew = false;
        synchronized(this.loginNameSubjectInfoMap) {
            subjectInfo = (SubjectInfo)this.loginNameSubjectInfoMap.get(subjectName.getLoginName());
            if (subjectInfo == null) {
                SessionName name = new SessionName(glob, glob.getNodeId(), subjectName.getLoginName()); // strip nodeId, strip pubSessionId
                //log.error(ME, "DEBUG ONLY: Stripped name=" + name.toString());
                subjectInfo = new SubjectInfo(getGlobal(), this, name);
                this.loginNameSubjectInfoMap.put(subjectName.getLoginName(), subjectInfo);
                isNew = true;
            }
        }

        if (isNew) {
            if (returnLocked) subjectInfo.getLock().lock();
            try {
                //log.error(ME, "DEBUG ONLY: REMOVE AGAIN");
                //if (subjectName.getLoginName().equals("subscriber")) {
                //   log.error(ME, "DEBUG ONLY: sleepig 20 sec for toAlive(): " + subjectName.toString());
                //   try { Thread.currentThread().sleep(20*1000L); } catch( InterruptedException i) {}
                //}
                subjectInfo.toAlive(subjectCtx);
            }
            catch(Throwable e) {
                synchronized(this.loginNameSubjectInfoMap) {
                    this.loginNameSubjectInfoMap.remove(subjectInfo.getLoginName());
                }
                if (returnLocked) subjectInfo.getLock().unlock();
                throw ServiceManagerException.convert(getGlobal(), SysErrorCode.INTERNAL_UNKNOWN, ME, e.toString(), e);
            }
        }
        else {
            subjectInfo.waitUntilAlive(returnLocked);
            if (subjectCtx != null && subjectInfo.getSecurityCtx() == null)
                subjectInfo.setSecurityCtx(subjectCtx); // If SubjectInfo was created by a PtP message the securityCtx is missing, add it here
        }

        return subjectInfo;
    }

    /**
     * Remove a SubjectInfo instance.
     */
    void removeLoginName(SubjectInfo subjectInfo) {
        Object entry = null;
        synchronized(this.loginNameSubjectInfoMap) {
            entry = this.loginNameSubjectInfoMap.remove(subjectInfo.getLoginName());
        }
        if (entry == null) {
            return;
        }

    }

    public int getNumSubjects() {
        return this.loginNameSubjectInfoMap.size();
    }

    /**
     * Access a subjectInfo with the unique login name
     * @return the SubjectInfo object<br />
     *         null if not found
     * @param subjectName
     */
    public final I_SubjectInfo getSubjectInfoByName(I_SessionName subjectName) {
        synchronized(this.loginNameSubjectInfoMap) {
            return (SubjectInfo)this.loginNameSubjectInfoMap.get(subjectName.getLoginName());
        }
    }

    public final I_SessionInfo getSessionInfoByName(SessionName sessionName) {
        I_SubjectInfo subjectInfo = getSubjectInfoByName(sessionName);
        if (subjectInfo == null) return null;
        return subjectInfo.getSession(sessionName);
    }

    /**
     * Replace the old by the new session id
     */
    public final void changeSecretSessionId(String oldSessionId, String newSessionId) throws ServiceManagerException {
        synchronized(this.sessionInfoMap) {
            SessionInfo sessionInfo = (SessionInfo)this.sessionInfoMap.get(oldSessionId);
            if (sessionInfo == null) {
                throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, ME+".changeSecretSessionId()", "Couldn't lookup secretSessionId.");
            }
            if (this.sessionInfoMap.get(newSessionId) != null) {
                throw new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, ME+".changeSecretSessionId()", "The new secretSessionId is already in use.");
            }
            this.sessionInfoMap.put(newSessionId, sessionInfo);
            this.sessionInfoMap.remove(oldSessionId);

            sessionInfo.getSecuritySession().changeSecretSessionId(newSessionId);
            sessionInfo.getConnectProperties().setSecretSessionId(newSessionId);
        }
    }

    /**
     * Access a sessionInfo with the unique secretSessionId.
     * <p />
     * @return the SessionInfo object or null if not known
     */
    private final SessionInfo getSessionInfo(String secretSessionId) {
        synchronized(this.sessionInfoMap) {
            SessionInfo sessionInfo = (SessionInfo)this.sessionInfoMap.get(secretSessionId);
            if (sessionInfo != null && sessionInfo.isInitialized())
                return sessionInfo;
        }
        return null;
    }

    /**
     * Returns a current snapshot of all sessions, never returns null.
     */
    public final SessionInfo[] getSessionInfoArr() {
        synchronized(this.sessionInfoMap) {
            return (SessionInfo[])this.sessionInfoMap.values().toArray((new SessionInfo[this.sessionInfoMap.size()]));
        }
    }

    /**
     * Find a session by its login name and pubSessionId or return null if not found
     */
    public final I_SessionInfo getSessionInfo(SessionName sessionName) {
        I_SubjectInfo subjectInfo = getSubjectInfoByName(sessionName);
        if (subjectInfo == null) {
            return null;
        }
        I_SessionInfo sessionInfo = subjectInfo.getSessionInfo(sessionName);
        if (sessionInfo != null && sessionInfo.isInitialized())
            return sessionInfo;
        return null;
    }

    /**
     * Blocks for existing SessionInfo until it is initialized.
     * For new created SessionInfo you need to call sessionInfo.initializeSession()
     * @param sessionName
     * @param connectQos
     * @return
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    private final SessionInfo getOrCreateSessionInfo(I_SessionName sessionName, I_SessionProperties connectQos) throws ServiceManagerException {
        I_SubjectInfo subjectInfo = getSubjectInfoByName(sessionName);
        if (subjectInfo == null)
            throw new IllegalArgumentException("subjectInfo is null for " + sessionName.getAbsoluteName());
        return (SessionInfo)subjectInfo.getOrCreateSessionInfo(sessionName, connectQos);
    }

    public boolean sessionExists(String secretSessionId) {
        synchronized(this.sessionInfoMap) {
            return this.sessionInfoMap.containsKey(secretSessionId);
        }
    }

    /**
     * Logout of a client.
     * <p>
     * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException If client is unknown
     */
    public final void logout(String secretSessionId) throws ServiceManagerException
    {
        log.fatal("logout not implemented");
        throw new ServiceManagerException(this.glob, SysErrorCode.INTERNAL_NOTIMPLEMENTED, ME + ".logout not implemented");
    }


    /**
     * @param sessionInfo
     * @param clearQueue Shall the message queue of the client be destroyed as well on last session logout?
     * @param forceShutdownEvenIfEntriesExist on last session
     * @param isDisconnecting true if this method is invoked while explicitly disconnecting a session, false
     *        otherwise. It is used to determine if the session queue (callback queue) has to be cleared or not.
     *    */
    private void resetSessionInfo(SessionInfo sessionInfo, boolean forceShutdownEvenIfEntriesExist, boolean isDisconnecting) throws ServiceManagerException
    {
        firePreRemovedClientEvent(sessionInfo);
        String secretSessionId = sessionInfo.getSecretSessionId();
        Object obj;
        synchronized(this.sessionInfoMap) {
            obj = this.sessionInfoMap.remove(secretSessionId);
        }

        if (obj == null) {
            log.warn("Sorry, '" + sessionInfo.getId() + "' is not known, no logout.");
            throw new ServiceManagerException(glob, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME,
                    "Client '" + sessionInfo.getId() + "' is not known, disconnect is not possible.");
        }

        log.info("Disconnecting client " + sessionInfo.getSessionName() + ", instanceId=" + sessionInfo.getInstanceId());

        I_Session oldSessionCtx = sessionInfo.getSecuritySession();
        oldSessionCtx.getManager().releaseSession(secretSessionId, null);

        fireClientEvent(sessionInfo, false); // informs all I_ClientListener

        I_SubjectInfo subjectInfo = sessionInfo.getSubjectInfo();
        subjectInfo.notifyAboutLogout(sessionInfo.getId(), forceShutdownEvenIfEntriesExist);
        //if (subjectInfo.isShutdown()) {
        //   subjectInfo = null; // Give GC a hint
        //}

        // with positive sessionId avoid to clear session queue: Such a DisconnectQos flag is currently not existing
//      if (isDisconnecting) sessionInfo.clear();
        sessionInfo.shutdown();

        sessionInfo = null;
        log.info("loginNameSubjectInfoMap has " + getNumSubjects() +
                " entries and sessionInfoMap has " + this.sessionInfoMap.size() + " entries");
    }


    /**
     *  Generate a unique (and secret) resource ID <br>
     *
     *  @param loginName
     *  @return unique ID
     *  @exception com.ethercis.servicemanager.exceptions.ServiceManagerException random generator
     */
    private String createSessionId(String loginName) throws ServiceManagerException
    {
        try {
            String ip = glob.getLocalIP();

            // This is a real random, but probably not necessary here:
            // Random random = new java.security.SecureRandom();
            java.util.Random ran = new java.util.Random();  // is more or less currentTimeMillis

            // Note: We should include the process ID from this JVM on this host to be granted unique

            // secretSessionId:<IP-Address>-<LoginName>-<TimestampMilliSec>-<RandomNumber>-<LocalCounter>
            StringBuffer buf = new StringBuffer(512);

            buf.append(Constants.SESSIONID_PREFIX).append(ip).append("-").append(loginName).append("-");
            buf.append(System.currentTimeMillis()).append("-").append(ran.nextInt()).append("-").append((counter++));

            String secretSessionId = buf.toString();
            log.debug("Created secretSessionId='" + secretSessionId + "'");
            return secretSessionId;
        }
        catch (Exception e) {
            String text = "Can't generate a unique secretSessionId: " + e.getMessage();
            log.fatal(text);
            throw new ServiceManagerException(glob, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME, text);
        }
    }

    /**
     * Returns a current snapshot of all ClientListeners
     */
    private final I_ClientListener[] getClientListenerArr() {
        synchronized(this.clientListenerSet) {
            return (I_ClientListener[])this.clientListenerSet.toArray((new I_ClientListener[this.clientListenerSet.size()]));
        }
    }

    private void firePreRemovedClientEvent(SessionInfo sessionInfo) throws ServiceManagerException {
        I_ClientListener[] clientListenerArr = getClientListenerArr();
        if (clientListenerArr.length == 0) return;
        ClientEvent event = new ClientEvent(sessionInfo);
        for (int ii=0; ii<clientListenerArr.length; ii++)
            clientListenerArr[ii].sessionPreRemoved(event);
        event = null;
    }

    /**
     * Used to fire an event if a client does a login / logout
     */
    private void fireClientEvent(SessionInfo sessionInfo, boolean login) throws ServiceManagerException {
        I_ClientListener[] clientListenerArr = getClientListenerArr();
        if (clientListenerArr.length == 0) return;
        ClientEvent event = new ClientEvent(sessionInfo);
        for (int ii=0; ii<clientListenerArr.length; ii++) {
            if (login)
                clientListenerArr[ii].sessionAdded(event);
            else
                clientListenerArr[ii].sessionRemoved(event);
        }
        event = null;
    }

    private void updateConnectProperties(SessionInfo sessionInfo, I_ConnectProperties newConnectProperties) throws ServiceManagerException {
        I_ConnectProperties previousConnectQosServer = sessionInfo.getConnectProperties();

        sessionInfo.updateConnectProperties(newConnectProperties);

        try {
            I_ClientListener[] clientListenerArr = getClientListenerArr();
            if (clientListenerArr.length == 0) return;
            ClientEvent event = new ClientEvent((I_SessionProperties) previousConnectQosServer, sessionInfo);
            for (int ii=0; ii<clientListenerArr.length; ii++) {
                clientListenerArr[ii].sessionUpdated(event);
            }
            event = null;
        }
        catch (ServiceManagerException e) {
            throw e;
        }
        catch (Throwable e) {
            e.printStackTrace();
            log.fatal("updateConnectQos for " + sessionInfo.getId() + " failed: " + e.toString());
        }
    }


    /**
     * Use this method to check a clients authentication.
     * <p>
     * This method is called from an invoked ehrserver-server
     * method (like subscribe()), using the delivered secretSessionId
     *
     * @return SessionInfo - if the client is OK otherwise an exception is thrown (returns never null)
     * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Access denied
     */
    public SessionInfo check(String secretSessionId) throws ServiceManagerException
    {
        // even the corba client should get a communication exception when the server is shutting down
        // (before this change he was getting "access denided" since the sessions were already killed).
      /* Removed check, Marcel 2003-03-26: This should be handled by loading specific plugins
         in ehrserverPlugins.xml
      if (glob.getRunlevelManager().getCurrentRunlevel() < RunlevelManager.RUNLEVEL_STANDBY) {
         String text = "The run level " + RunlevelManager.toRunlevelStr(glob.getRunlevelManager().getCurrentRunlevel()) +
                       " of ehrserver is not handling any communication anymore. " + glob.getId() + ".";
         log.warn(ME+".communication.noconnection", text);
         throw new ServiceManagerException(glob, SysErrorCode.COMMUNICATION_NOCONNECTION, ME, text);
      }
      */

        Object obj = null;
        synchronized(this.sessionInfoMap) {
            obj = this.sessionInfoMap.get(secretSessionId);
        }

        if (obj == null) {
            log.warn("SessionId '" + secretSessionId + "' is invalid, no access to ehrserver.");
            throw new ServiceManagerException(glob, SysErrorCode.USER_SECURITY, ME, "Your secretSessionId is invalid, no access to " + glob.getId() + ".");
        }
        SessionInfo sessionInfo = (SessionInfo)obj;

        sessionInfo.refreshSession(); // touch the session, expiry timer is spaned

        log.debug("Succesfully granted access for " + sessionInfo.toString());

        return sessionInfo;
    }


    /**
     * Adds the specified client listener to receive login/logout events.
     * <p />
     * This listener needs to implement the I_ClientListener interface.
     */
    public void addClientListener(I_ClientListener l) {
        if (l == null) {
            return;
        }
        synchronized (clientListenerSet) {
            clientListenerSet.add(l);
        }
    }


    /**
     * Removes the specified listener
     */
    public synchronized void removeClientListener(I_ClientListener l) {
        if (l == null) {
            return;
        }
        synchronized (clientListenerSet) {
            clientListenerSet.remove(l);
        }
    }

    public int getMaxSubjects() {
        return Integer.MAX_VALUE; // TODO: allow to limit max number of different clients (or login sessions?)
    }

    /**
     * Get a current snapshot of all known subjects.
     * @return The subjects known
     */
    public SubjectInfo[] getSubjectInfoArr() {
        synchronized(this.loginNameSubjectInfoMap) {
            return (SubjectInfo[])this.loginNameSubjectInfoMap.values().toArray(new SubjectInfo[this.loginNameSubjectInfoMap.size()]);
        }
    }

    /**
     * Access a list of login names e.g. "joe","jack","averell","william"
     * @return An array of length 0 if no clients available
     */
    public String[] getSubjects() {
        SubjectInfo[] arr = getSubjectInfoArr();
        if (arr == null || arr.length == 0)
            return new String[0];
        String[] ret = new String[arr.length];
        for (int i=0; i<arr.length; i++) {
            ret[i] = arr[i].getLoginName();
        }
        return ret;
    }

    /**
     * Access a list of login names e.g. "joe,jack,averell,william"
     * @return An empty string if no clients available
     */
    public String getSubjectList() {
        int numSubjects = getNumSubjects();
        if (numSubjects < 1)
            return "";
        StringBuffer sb = new StringBuffer(numSubjects * 30);
        synchronized(this.loginNameSubjectInfoMap) {
            Iterator iterator = this.loginNameSubjectInfoMap.values().iterator();
            while (iterator.hasNext()) {
                if (sb.length() > 0)
                    sb.append(",");
                SubjectInfo subjectInfo = (SubjectInfo)iterator.next();
                sb.append(subjectInfo.getLoginName());
            }
        }
        return sb.toString();
    }

    /**
     * Enforced by I_RunlevelListener
     */
    public String getName() {
        return this.ME;
    }

    /**
     * Helper method where protocol layers may report a lost connection (e.g. socket EOF).
     * <p>
     * The SessionInfo can than do an immediate ping() to trigger POLLING mode
     * @see com.ethercis.servicemanager.common.session.I_Authenticate#(String, ConnectionStateEnum)
     */
    public void connectionState(String secretSessionId, I_ConnectionStateEnum state) {
        if (state == ConnectionStateEnum.DEAD) {
            SessionInfo sessionInfo = getSessionInfo(secretSessionId);
            if (sessionInfo != null)
                sessionInfo.lostClientConnection();
        }
        else {
            log.warn("Ignoring unexpected connectionState notification + " + state.toString() + ", handling is not implemented");
        }
    }

    /**
     * Invoked on run level change, see RunlevelManager.RUNLEVEL_HALTED and RunlevelManager.RUNLEVEL_RUNNING
     * <p />
     * Enforced by I_RunlevelListener
     */
    public void runlevelChange(int from, int to, boolean force) throws ServiceManagerException {
        //if (log.isLoggable(Level.FINER)) log.call(ME, "Changing from run level=" + from + " to level=" + to + " with force=" + force);
        if (to == from)
            return;

        if (to > from) { // startup
            if (to == RunlevelManager.RUNLEVEL_CLEANUP_PRE) {
            }
        }

        if (to < from) { // shutdown
            if (to == RunlevelManager.RUNLEVEL_HALTED) {
                log.debug("Killing " + this.sessionInfoMap.size() + " login sessions");
                SessionInfo[] sessionInfoArr = getSessionInfoArr();
                for (int ii=0; ii<sessionInfoArr.length; ii++) {
                    try {
                        boolean clearQueue = false;
                        boolean forceShutdownEvenIfEntriesExist = true;
                        resetSessionInfo(sessionInfoArr[ii], forceShutdownEvenIfEntriesExist, false);
                    }
                    catch (Throwable e) {
                        log.fatal("Problem on session shutdown, we ignore it: " + e.getMessage());
                        if (!(e instanceof ServiceManagerException)) e.printStackTrace();
                    }
                } // for
            }
        }
    }



/*
   private I_SessionPersistencePlugin getSessionPersistencePlugin() {
      return ((I_SessionPersistencePlugin)this.glob.getPluginRegistry().getPlugin(I_SessionPersistencePlugin.ID));
   }

   private void persistenceConnect(SessionInfo info) throws ServiceManagerException {
      I_SessionPersistencePlugin plugin = getSessionPersistencePlugin();
      if (plugin == null) {
         log.error(ME, "persistenceConnect: the session persistence plugin is not registered (yet): can't make connection persistent");
         Thread.dumpStack();
         return;
      }
      ClientEvent event = new ClientEvent(info);
      plugin.sessionAdded(event);
   }

   private void persistenceDisConnect(SessionInfo info) throws ServiceManagerException {
      I_SessionPersistencePlugin plugin = getSessionPersistencePlugin();
      if (plugin == null) {
         log.error(ME, "persistenceConnect: the session persistence plugin is not registered (yet): can't make connection persistent");
         Thread.dumpStack();
         return;
      }
      ClientEvent event = new ClientEvent(info);
      plugin.sessionRemoved(event);
   }
*/
    /**
     * Dump state of this object into a XML ASCII string.
     * <br>
     * @return internal state of Authenticate as a XML ASCII string
     */
    public final String toXml() {
        return toXml((String)null);
    }

    /**
     * Dump state of this object into a XML ASCII string.
     * <br>
     * @param extraOffset indenting of tags for nice response
     * @return internal state of Authenticate as a XML ASCII string
     */
    public final String toXml(String extraOffset) {
        StringBuffer sb = new StringBuffer(1000);
        if (extraOffset == null) extraOffset = "";
        String offset = Constants.OFFSET + extraOffset;

        log.info("Client maps, sessionInfoMap.size()=" + this.sessionInfoMap.size() +
                " and loginNameSubjectInfoMap.size()=" + getNumSubjects());
        synchronized(this.loginNameSubjectInfoMap) {
            Iterator iterator = this.loginNameSubjectInfoMap.values().iterator();

            sb.append(offset).append("<Authenticate>");
            while (iterator.hasNext()) {
                SubjectInfo subjectInfo = (SubjectInfo)iterator.next();
                sb.append(subjectInfo.toXml(extraOffset+Constants.INDENT));
            }
            sb.append(offset).append("</Authenticate>\n");
        }

        return sb.toString();
    }

    /** For JMX MBean: The number of different users, the sessions may be higher */
    public int getNumClients() {
        return getNumSubjects();
    }
    /** For JMX MBean: The maximum number of different users, the sessions may be higher */
    public int getMaxClients() {
        return getMaxSubjects();
    }
    /** For JMX MBean: These are the login names returned, every client may be logged in multiple times
     which you can't see here */
    public String getClientList() {
        return getSubjectList();
    }

    /**
     * Authorization check (TODO: generic approach)
     * @param sessionInfo can be null to get the general setting
     * @return true: We accept wrong sender address in PublishQos.getSender() (not myself)
     */
    public boolean isAcceptWrongSenderAddress(SessionInfo sessionInfo) {
        if (this.acceptWrongSenderAddress)
            return this.acceptWrongSenderAddress;
        if (sessionInfo != null)
            return sessionInfo.isAcceptWrongSenderAddress();
        return this.acceptWrongSenderAddress;
    }

    /**
     * @param acceptWrongSenderAddress the acceptWrongSenderAddress to set
     */
    public void setAcceptWrongSenderAddress(boolean acceptWrongSenderAddress) {
        boolean old = this.acceptWrongSenderAddress;
        this.acceptWrongSenderAddress = acceptWrongSenderAddress;
        String tmp = "Changed acceptWrongSenderAddress from " + old + " to " + this.acceptWrongSenderAddress + ".";
        if (this.acceptWrongSenderAddress == true)
            log.warn(tmp + " Caution: All clients can now publish messages using anothers login name as sender");
        else
            log.info(tmp + " Faking anothers publisher address is not possible, but specific clients may allow it");
    }

    @Override
    public String ping(String addressServer, String qos) {
        // TODO Auto-generated method stub
        return null;
    }

}

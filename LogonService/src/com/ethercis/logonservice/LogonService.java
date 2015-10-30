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
package com.ethercis.logonservice;

import com.ethercis.logonservice.access.ConnectProperties;
import com.ethercis.logonservice.session.*;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Logon service for all clients connecting to the backend services This class
 * is a facade to the SessionManager
 * 
 * @author Christian Chevalley
 * 
 */
@Service(id = "LogonService" ,version="1.0",system=true)
//@Attributes({ @Attribute(id = "server.session.compatibility", value = I_ServiceRunMode.CompatibilityMode.EHRSCAPE.toString())
//})
@RunLevelActions(value = {
		@RunLevelAction(onStartupRunlevel = 7, sequence = 3, action = "LOAD"),
		@RunLevelAction(onShutdownRunlevel = 8, sequence = 5, action = "STOP") })

@ParameterSetting( identification = {
        @ParameterIdentification(id = "user", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "user", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "username", type = String.class)
        }),
        @ParameterIdentification(id = "password", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "password", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "password", type = String.class)
        })
})
public class LogonService extends ClusterInfo implements I_SessionManager {

	final private String ME = "LogonService";
	final private String Version = "1.0";
	private RunTimeSingleton global;
	private static Logger log = Logger.getLogger(LogonService.class);

    private String LOGON_PARAMETER;
    private String PASSWORD_PARAMETER;
    private String SESSION_ID_PARAMETER;

	// private SessionManager
	SessionManager manager;

	@Override
	public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)
			throws ServiceManagerException {

		this.global = global;

        initCompatibilityMode();

        LOGON_PARAMETER = ParameterAnnotationHelper.parameterName(dialectSpace, "user", this.getClass().getAnnotations());
        PASSWORD_PARAMETER = ParameterAnnotationHelper.parameterName(dialectSpace, "password", this.getClass().getAnnotations());
        SESSION_ID_PARAMETER = ParameterAnnotationHelper.parameterName(dialectSpace, "session_id", this.getClass().getAnnotations());

		manager = new SessionManager(this.global);

		// required for JMX usage
		global.setAuthenticate(manager);

		log.info("Logon service started...");
	}

    @QuerySetting(dialect = {
            @QuerySyntax(mode = DialectSpace.STANDARD, httpMethod = "GET", method = "connect", path = "vehr", responseType = ResponseType.Void),
            @QuerySyntax(mode = DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/session", responseType = ResponseType.String)
            })
	public ResponseHolder connect(I_SessionClientProperties props)
			throws ServiceManagerException {
		String SSID = props.getClientProperty(
				I_SessionManager.SECRET_SESSION_ID(dialectSpace), (String) null);
		return connect(props, SSID);
	}

	// implements public service methods (connect, disconnect, check...)
	/**
	 * connect (login) a client.
	 * <p>
	 * Several parameters can be supplied in SessionClientProperties (constants
	 * from I_SessionManager):
	 * <p>
	 * <ul>
	 * <li>SECRET_SESSION_ID: [OPTIONAL] force a secret session id for this
	 * session, or try to reconnect to an existing ssid
	 * <li>SESSION_NAME: [OPTIONAL] force a public session name. If it use for
	 * reconnect, check if the reconnect is legitimate (same user and check the
	 * credentials)
	 * <li>CLUSTER_NODE: [OPTIONAL] boolean, indicate this is a connection from
	 * a cluster node
	 * <li>REFRESH_SESSION: [OPTIONAL] boolean, indicate that this session is to
	 * be refreshed (push timeout back)
	 * <li>RECONNECT: [OPTIONAL] boolean, true if this is a reconnect
	 * <li>SESSION_TIMEOUT: [OPTIONAL] long, sets a timeout for session (default
	 * is no timeout)
	 * <li>MAX_SESSION: [OPTIONAL] sets the max number of allowed session for
	 * user (1 by default)
	 * <li>CLEAR_SESSION: [OPTIONAL] clear all sessions for this users if true
	 * <li>BYPASS_CREDENTIAL: [OPTIONAL] no credential check for this user
	 * (security hazard here!!!)
	 * <li>CLIENT_IP: [OPTIONAL] address of this user
	 * <li>USER_ID: [REQUIRED] user id for logon (defined in policy file)
	 * <li>USER_PASSWORD: [OPTIONAL] credential for user
	 * </ul>
	 * <p>
	 * Return a SessionClientProperties with the following properties:
	 * <p>
	 * <ul>
	 * <li>SECRET_SESSION_ID: secret session id for this session (use this for
	 * further transactions)
	 * <li>SESSION_NAME: public session name.
	 * <li>RECONNECT: boolean, true if this is a reconnect
	 * <li>SESSIONS_IN_USE: number of sessions used by user
	 * <li>Constants.CLIENTPROPERTY_RCVTIMESTAMPSTR: timestamp of session
	 * creation</li>
	 * </ul>
	 */
	public ResponseHolder connect(I_SessionClientProperties props, String sessionId)
			throws ServiceManagerException {
		I_ConnectProperties connectproperties = new ConnectProperties(global);

		String secretSessionId = sessionId;

		// get the other connection parameters
		String sessionName = props.getClientProperty(
				I_SessionManager.SESSION_NAME, (String) null);

		// build the respective connection property
		connectproperties.setPtpAllowed(props.getClientProperty(I_SessionManager.PTP_ALLOWED, connectproperties.isPtpAllowed()));
		connectproperties.setClusterNode(props.getClientProperty(I_SessionManager.CLUSTER_NODE, false));
		connectproperties.setRefreshSession(props.getClientProperty(I_SessionManager.REFRESH_SESSION, false));
		connectproperties.setDuplicateUpdates(props.getClientProperty(I_SessionManager.DUPLICATES_UPDATES, false));
		connectproperties.getSessionProperties().setReconnected(props.getClientProperty(I_SessionManager.RECONNECT, false));
		connectproperties.getSessionProperties().setSessionTimeout(props.getClientProperty(I_SessionManager.SESSION_TIMEOUT, connectproperties.getSessionProperties().getSessionTimeout())); // last 30' by default

        if (props.getClientProperty(I_SessionManager.RECONNECT_SAME_CLIENT_ONLY, connectproperties.reconnectSameClientOnly()))
            connectproperties.reconnectSameClientOnly();

		connectproperties.getSessionProperties().setMaxSessions(props.getClientProperty(I_SessionManager.MAX_SESSION, connectproperties.getSessionProperties().getMaxSessions()));
		connectproperties.getSessionProperties().clearSessions(props.getClientProperty(I_SessionManager.CLEAR_SESSION, connectproperties.getSessionProperties().clearSessions()));
		connectproperties.bypassCredentialCheck(props.getClientProperty(I_SessionManager.BYPASS_CREDENTIAL, connectproperties.bypassCredentialCheck()));
        connectproperties.getSecurityProperties().setClientIp(props.getClientProperty(I_SessionManager.CLIENT_IP, connectproperties.getSecurityProperties().getClientIp()));

        //query parameters
        connectproperties.getSecurityProperties().setUserId(props.getClientProperty(LOGON_PARAMETER, connectproperties.getSecurityProperties().getUserId()));
        connectproperties.getSecurityProperties().setCredential(props.getClientProperty(PASSWORD_PARAMETER,	connectproperties.getSecurityProperties().getCredential()));

		if (sessionName != null) {
			SessionName name = new SessionName(global, sessionName); // set the public session name
			connectproperties.getSessionProperties().setSessionName(name);
		}
        else { //force sessionName to null to create a new session name with current userId
            connectproperties.getSessionProperties().setSessionName(null);
        }



        // sets the SecurityInfo parameters
		I_SessionProperties sessionProperties;

		if (secretSessionId != null) {
			try {
				sessionProperties = manager.connect(connectproperties, secretSessionId);
			} catch (IllegalArgumentException iae) {
				throw new ServiceManagerException(
						global,
						SysErrorCode.USER_SECURITY_AUTHENTICATION_ILLEGALARGUMENT,
						"connect", iae.getMessage(), iae);
			}
		} else {
			try {
				sessionProperties = manager.connect(connectproperties);
			} catch (IllegalArgumentException iae) {
				throw new ServiceManagerException(
						global,
						SysErrorCode.USER_SECURITY_AUTHENTICATION_ILLEGALARGUMENT,
						"connect", iae.getMessage(), iae);
			}
		}

		ResponseHolder responseHolder = new ResponseHolder(
				"text/xml;charset=UTF-8", global);
		responseHolder.getSessionClientProperties().addClientProperty(
				I_SessionManager.SECRET_SESSION_ID(dialectSpace),
				sessionProperties.getSecretSessionId());
		responseHolder.getSessionClientProperties().addClientProperty(
                I_SessionManager.SESSION_NAME,
                sessionProperties.getSessionName().getAbsoluteName());
		responseHolder.getSessionClientProperties().addClientProperty(
				I_SessionManager.RECONNECT, sessionProperties.isReconnected());
		responseHolder.getSessionClientProperties().addClientProperty(
                I_SessionManager.SESSIONS_IN_USE,
                sessionProperties.getClientProperties().getClientProperty(
                        I_SessionManager.SESSIONS_IN_USE, 0));
		responseHolder.getSessionClientProperties().addClientProperty(
				Constants.CLIENTPROPERTY_RCVTIMESTAMPSTR,
				sessionProperties.getClientProperties().getClientProperty(
						Constants.CLIENTPROPERTY_RCVTIMESTAMPSTR, ""));

        //TODO: maybe for some dialect, do not set the secret session id into the content
        responseHolder.setData(sessionProperties.getSecretSessionId());

		return responseHolder;
	}

	/**
	 * disconnect a current session
	 * <p>
	 * Several parameters can be supplied in SessionClientProperties (constants
	 * from I_SessionManager):
	 * <p>
	 * <ul>
	 * <li>SECRET_SESSION_ID: secret session id for this session
	 * </ul>
	 */
    @QuerySetting(dialect = {
            @QuerySyntax(mode = DialectSpace.STANDARD, httpMethod = "GET", method = "disconnect", path = "vehr", responseType = ResponseType.String),
            @QuerySyntax(mode = DialectSpace.EHRSCAPE, httpMethod = "DELETE", method = "delete", path = "rest/v1/session", responseType = ResponseType.String)
            }
    )
	public String disconnect(I_SessionClientProperties props)
			throws ServiceManagerException {

		String SSID = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), (String) null);

		manager.disconnect(SSID, null);

        return SSID;
	}

	/**
	 * terminate all sessions
	 */
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	public String getType() {
		return ME;
	}

	public String getVersion() {
		return Version;
	}

	@Override
	public I_SessionInfo unsecureCreateSession(I_SessionProperties props)
			throws ServiceManagerException {
		//locally typecast to avoid cross referencing between projects
		return manager.unsecureCreateSession((SessionProperties)props);
	}

	@Override
	public I_SessionProperties connect(ConnectProperties sessionprops)
			throws ServiceManagerException {
		return manager.connect(sessionprops);
	}

	@Override
	public I_SessionProperties connect(ConnectProperties connectProps,
			String secretSessionId) throws ServiceManagerException {
		return manager.connect(connectProps, secretSessionId);
	}

	@Override
	public void disconnect(String secretSessionId, String literal)
			throws ServiceManagerException {
		manager.disconnect(secretSessionId, literal);

	}

	@Override
	public SessionInfo check(String secretSessionId) throws ServiceManagerException {
		return manager.check(secretSessionId);
	}

	@Override
	public void addClientListener(I_ClientListener l) {
		manager.addClientListener(l);

	}

	@Override
	public void removeClientListener(I_ClientListener l) {
		manager.removeClientListener(l);

	}

    @Override
    public Map<String, Object> getSessionUserMap(String secretSessionId) throws IllegalArgumentException{

        try {
            SessionInfo sessionInfo = check(secretSessionId);
            return sessionInfo.getUserObjectMap();
        } catch (ServiceManagerException e) {
            throw new IllegalArgumentException("Invalid session id:"+secretSessionId);
        }
    }

    @Override
    public String getSubjectName(String secretSessionId) throws IllegalArgumentException{

        try {
            SessionInfo sessionInfo = check(secretSessionId);
            return sessionInfo.getSubjectInfo().getLoginName();
        } catch (ServiceManagerException e) {
            throw new IllegalArgumentException("Invalid session id:"+secretSessionId);
        }

    }

}

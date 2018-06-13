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

import com.ethercis.logonservice.access.ConnectProperties;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;

import java.util.Map;

public interface I_SessionManager extends I_ServiceRunMode {

    //header fields
    /**
     * user supplied session id, returned session id [String] Header: x-session-id
     */
    String SECRET_SESSION_ID_STANDARD = "x-session-id";
    /**
     * user supplied session id in EhrScape mode, returned session id [String] Header: Ehr-Session
     */
    String SECRET_SESSION_ID_EHRSCAPE = "Ehr-Session";

    String SECRET_SESSION_ID_INTERNAL = "SecretSessionId";

    String AUTHORIZATION = "Authorization";

    /**
     * receive ptp messages, true by default [Boolean] Header: x-ptp-allowed
     */
    String PTP_ALLOWED = "x-ptp-allowed";
    /**
     * mark this logonservice cluster node [Boolean] Header: x-cluster_node
     */
    String CLUSTER_NODE = "x-cluster_node";
    /**
     * indicate client is alive and session is extended [Boolean] Header: x-refresh-session
     */
    String REFRESH_SESSION = "x-refresh-session";
    /**
     * true if client can receive duplicates messages, false by default [Boolean] Header: x-duplicate-updates
     */
    String DUPLICATES_UPDATES = "x-duplicate-updates";
    /**
     * client does reconnect to an existing session, false by default [Boolean] Header: x-reconnect
     */
    String RECONNECT = "x-reconnect";
    /**
     * timeout for autologout [Long], default is forever (0L) Header: x-session-timeout
     */
    String SESSION_TIMEOUT = "x-session-timeout";
    /**
     * max allowed session for user [Integer] Header: x-max-session
     */
    String MAX_SESSION = "x-max-session";
    /**
     * clear all previous session for this user Header: x-clear-session
     */
    String CLEAR_SESSION = "x-clear-session";
    /**
     * true if client can reconnect with a public session id, false by default [Boolean] Header: x-reconnect-same-client-only
     */
    String RECONNECT_SAME_CLIENT_ONLY = "x-reconnect-same-client-only";
    /**
     * true if client can connect without credential, false by default [Boolean] Header: x-bypass-credential
     */
    String BYPASS_CREDENTIAL = "x-bypass-credential";
    /**
     * public optional session name (must be unique) Header: x-session-name
     */
    String SESSION_NAME = "x-session-name";
    /**
     * client IP address Header: x-client-ip
     */
    String CLIENT_IP = "x-client-ip";
    /**
     * indicate the last login time
     */
    String LAST_LOGIN = "x-last-login";
    /**
     * indicate the number of sessions in use for this user Header:  x-sessions-in-use
     */
    String SESSIONS_IN_USE = "x-sessions-in-use";

    String USER_ID = "user";
    String USER_PASSWORD = "password";

    //used for error messages
    String ERROR_MESSAGE = "x-error-message";
    String ERROR_CODE = "x-error-code"; //ServiceManagerException Error Code logonservice String

    static String SECRET_SESSION_ID(DialectSpace dialectSpace) {
        switch (dialectSpace) {
            case STANDARD:
                return SECRET_SESSION_ID_STANDARD;
            case EHRSCAPE:
                return SECRET_SESSION_ID_EHRSCAPE;
            default:
                return null;
        }
    }

    /**
     * create a user and session for internal use only.
     *
     * @param props
     * @return
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    I_SessionInfo unsecureCreateSession(I_SessionProperties props) throws ServiceManagerException;

    /**
     * Login to service.
     */
    I_SessionProperties connect(ConnectProperties sessionprops) throws ServiceManagerException;

    I_SessionProperties connect(ConnectProperties connectProps, String secretSessionId) throws ServiceManagerException;

    ResponseHolder connect(I_SessionClientProperties props) throws ServiceManagerException;

    ResponseHolder connect(I_SessionClientProperties props, String ssid) throws ServiceManagerException;

    void disconnect(String secretSessionId, String literal) throws ServiceManagerException;

    Object disconnect(I_SessionClientProperties props) throws ServiceManagerException;

    I_SessionInfo check(String secretSessionId) throws ServiceManagerException;

    void addClientListener(I_ClientListener l);

    void removeClientListener(I_ClientListener l);

    Map<String, Object> getSessionUserMap(String secretSessionId) throws IllegalArgumentException;

    String getSubjectName(String secretSessionId) throws IllegalArgumentException;

    String getSubjectId(String secretSessionId) throws IllegalArgumentException;
}

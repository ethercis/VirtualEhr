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
	public static String SECRET_SESSION_ID_STANDARD = "x-session-id"; /** user supplied session id, returned session id [String]*/
    public static String SECRET_SESSION_ID_EHRSCAPE = "Ehr-Session";
    public static String SECRET_SESSION_ID_INTERNAL = "SecretSessionId";

    public static String PTP_ALLOWED = "x-ptp-allowed"; /** receive ptp messages, true by default [Boolean]*/
	public static String CLUSTER_NODE = "x-cluster_node"; /** mark this logonservice cluster node [Boolean]*/
	public static String REFRESH_SESSION = "x-refresh-session"; /** indicate client is alived and session is extended [Boolean]*/
	public static String DUPLICATES_UPDATES = "x-duplicate-updates"; /** true if client can receive duplicates messages, false by default [Boolean] */
	public static String RECONNECT = "x-reconnect"; /** client does reconnect to an existing session, false by default [Boolean] */
	public static String SESSION_TIMEOUT = "x-session-timeout"; /** timeout for autologout [Long], default is forever (0L) */
	public static String MAX_SESSION = "x-max-session"; /** max allowed session for user [Integer] */
	public static String CLEAR_SESSION = "x-clear-session"; /** clear all previous session for this user */
	public static String RECONNECT_SAME_CLIENT_ONLY = "x-reconnect-same-client-only"; /** true if client can reconnect with a public session id, false by default [Boolean]*/
	public static String BYPASS_CREDENTIAL = "x-bypass-credential"; /** true if client can connect without credential, false by default [Boolean] */
	public static String SESSION_NAME = "x-session-name"; /** public optional session name (must be unique) */
	public static String CLIENT_IP = "x-client-ip"; /** client IP address */
	public static String LAST_LOGIN = "x-last-login"; /** indicate the last login time */
	public static String SESSIONS_IN_USE = "x-sessions-in-use";

    static String USER_ID = "user";
    static String USER_PASSWORD = "password";

	//used for error messages
	public static String ERROR_MESSAGE = "x-error-message";
	public static String ERROR_CODE = "x-error-code"; //ServiceManagerException Error Code logonservice String

    public static String SECRET_SESSION_ID(DialectSpace dialectSpace){
        switch (dialectSpace){
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
	 * @param props
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public I_SessionInfo unsecureCreateSession(I_SessionProperties props) throws ServiceManagerException;
	/**
	 * Login to service.
	 */
	public I_SessionProperties connect(ConnectProperties sessionprops) throws ServiceManagerException;

	public I_SessionProperties connect(ConnectProperties connectProps, String secretSessionId) throws ServiceManagerException;
	
	public ResponseHolder connect(I_SessionClientProperties props) throws ServiceManagerException;
	
	public ResponseHolder connect(I_SessionClientProperties props, String ssid) throws ServiceManagerException;

	public void disconnect(String secretSessionId, String literal) throws ServiceManagerException;
	
	public Object disconnect(I_SessionClientProperties props) throws ServiceManagerException;

	public I_SessionInfo check(String secretSessionId) throws ServiceManagerException;

	public void addClientListener(I_ClientListener l);
	public void removeClientListener(I_ClientListener l);

    Map<String, Object> getSessionUserMap(String secretSessionId)  throws IllegalArgumentException;

    String getSubjectName(String secretSessionId) throws IllegalArgumentException;

	String getSubjectId(String secretSessionId) throws IllegalArgumentException;
}

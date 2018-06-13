/*
 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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

package com.ethercis.vehr;

import com.ethercis.logonservice.LogonService;
import com.ethercis.logonservice.access.ContextHolder;
import com.ethercis.logonservice.access.SessionHolder;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.logonservice.session.ResponseHolder;
import com.ethercis.logonservice.session.SessionInfo;
import com.ethercis.servicemanager.annotation.ParameterAnnotationHelper;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.common.session.I_SessionHolder;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 5/30/2018.
 */
public class SessionManagedQuery implements I_QueryWrapper {

    private String connectPath;
    private String connectMethod;
    private String connectAction;
    private String disconnectPath;
    private String disconnectMethod;
    private String disconnectAction;

    private LogonService logonService;
    protected RunTimeSingleton global;
    protected I_ServiceRunMode.DialectSpace dialectSpace = null;

    final String ME = "SessionManagedQuery";
    private static Logger log = LogManager.getLogger(SessionManagedQuery.class);

    public SessionManagedQuery(RunTimeSingleton global, LogonService logonService, I_ServiceRunMode.DialectSpace dialectSpace, Annotation[] contextAnnotations) {
        this.logonService = logonService;
        this.global = global;
        this.connectPath = ParameterAnnotationHelper.parameterName(dialectSpace, "connect_path", contextAnnotations);
        this.connectMethod = ParameterAnnotationHelper.parameterName(dialectSpace, "connect_method", contextAnnotations);
        this.connectAction = ParameterAnnotationHelper.parameterName(dialectSpace, "connect_action", contextAnnotations);
        this.disconnectPath = ParameterAnnotationHelper.parameterName(dialectSpace, "disconnect_path", contextAnnotations);
        this.disconnectMethod = ParameterAnnotationHelper.parameterName(dialectSpace, "disconnect_method", contextAnnotations);
        this.disconnectAction = ParameterAnnotationHelper.parameterName(dialectSpace, "disconnect_action", contextAnnotations);
        this.dialectSpace = dialectSpace;
    }

    @Override
    public Map connect(MethodName action, I_SessionClientProperties hdrprops, String path, MethodName method, Object... parameters) throws ServiceManagerException {

        ResponseHolder responseHolder = (ResponseHolder) serviceConnect(hdrprops, path, action, method, parameters);

        Map<String, Object> retMap = null;

        if (responseHolder != null) {
            retMap = new HashMap<>();
            retMap.put("action", "CREATE");
            ClientProperty sessionClientProperty = responseHolder.getSessionClientProperties().getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace));
            retMap.put("sessionId", sessionClientProperty.toString());
            Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + "sessionId=" + sessionClientProperty.toString());
            retMap.putAll(metaref);
            retMap.put("headers", responseHolder.getSessionClientProperties());
        }

        return retMap;
    }

    /**
     * manage queries to connect/disconnect a user
     *
     * @param hdrprops
     * @param path
     * @param method
     * @param parameters
     * @return
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    private Object serviceConnect(I_SessionClientProperties hdrprops, String path, MethodName action, MethodName method, Object... parameters)
            throws ServiceManagerException {
        // if the query is /vehr/connect then do a direct call to LogonService
        // it is assume that this is the only case where a null session-id is
        // accepted
        // this is hardcoded to avoid loopholes such logonservice using the services
        // without a valid
        // sessionid

        I_SessionClientProperties qryparms = (I_SessionClientProperties) parameters[0];

        ResponseHolder ret = null;
        try {
            //add header x- fields into the query parameters
            Map<String, String> headerMap = hdrprops.clientProps2StringMap();

            for (String parm : headerMap.keySet()) {
                if (parm.contains("x-")) {
                    qryparms.addClientProperty(parm, headerMap.get(parm));
                }
            }

            ret = (ResponseHolder) connect(qryparms, hdrprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), (String) null));
            if (ret != null) {

                I_SessionInfo sessioninfo = logonService.check(ret.getSessionClientProperties().getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), new String("")));
                I_Session sessionSecurityContext = sessioninfo.getSecuritySession();
                I_Authenticate subjectSecurityContext = sessionSecurityContext.getAuthenticate();

                // Log connect
                String userId = null;
                if (subjectSecurityContext != null) //null if bypass_credential == true
                    userId = subjectSecurityContext.getUserId();
                AccessLog.log(userId == null ? "BYPASS_CREDENTIAL" : userId, method.getMethodName(), path);
                return ret;
            }
        } catch (ServiceManagerException soae) {
            // Log connect
            AccessLog.log("unknown", method.getMethodName(), path);
            throw soae;
        }

        throw new ServiceManagerException(global,
                SysErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED, ME,
                "Could not connect");
    }

    @Override
    public I_QueryUnit query(I_QueryUnit qryunit, I_SessionClientProperties qryparms, I_SessionClientProperties hdrprops, String path, MethodName action, MethodName method) throws ServiceManagerException {
        String sessionid = hdrprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), (String) null);
        SessionInfo sessioninfo = logonService.check(sessionid);
        I_Session sessionSecurityContext = sessioninfo.getSecuritySession();
        I_Authenticate subjectSecurityContext = sessionSecurityContext.getAuthenticate();
        I_SessionHolder sessionholder = new SessionHolder(sessioninfo);

        //Set Authentication to SecurityContext for access this value from other object
        //by using SecurityContext.getAuthentication();
        SecurityContext.setAuthentication(new Authentication(sessionSecurityContext, subjectSecurityContext));

        //add the secret session id for internal purpose
        qryunit.getParameters().addClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, sessionid);

        I_ContextHolder contextholder = new ContextHolder(action, qryunit);

        AccessLog.info("userId=" + subjectSecurityContext.getUserId() + ",method=" + method.getMethodName() + ",path=" + path + ",qryparams=" + qryparms.toString());

        if (!sessionSecurityContext.isAuthorized(sessionholder, contextholder)) {
            String msg = "Subject:" + subjectSecurityContext.getUserId()
                    + " is not authorized for '" + method + "'" + " in path:'"
                    + path + "'";
            log.warn(msg);
            throw new ServiceManagerException(global,
                    SysErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED, ME,
                    msg);
        }

        if (isDisconnectAction(path, action, method)) { //add the secret session id into the parameters
            if (!qryunit.getParameters().getClientProperties().containsKey(I_SessionManager.SECRET_SESSION_ID(dialectSpace))) {
                qryunit.getParameters().addClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), sessionid);
            }
        }

        // Log query
        //AccessLog.log(subjectSecurityContext.gtName(), method.getMethodName(),path);
        //AccessLog.info(qryunit.toString());

        return qryunit;

    }

    /**
     * directly call LogonService for a connect
     *
     * @param props
     * @return
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    private Object connect(I_SessionClientProperties props, String ssid) throws ServiceManagerException {

        ResponseHolder responseHolder = logonService.connect(props, ssid);

        return responseHolder;
    }

    /**
     * directly call LogonService for a disconnect
     *
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    private void disconnect(String ssid) throws ServiceManagerException {
        logonService.disconnect(ssid, null);
    }

    /**
     * a connect action does not require credential checking
     *
     * @param path
     * @param method
     * @return
     */
    @Override
    public boolean isConnectAction(String path, MethodName action, MethodName method) {
        return (path.equals(connectPath) && action.getMethodName().equals(connectAction) && method.getMethodName().equals(connectMethod));
    }


    /**
     * a disconnect action does require credential checking !
     *
     * @param path
     * @param method
     * @return
     */
    @Override
    public boolean isDisconnectAction(String path, MethodName action, MethodName method) {
        return (path.equals(disconnectPath) && action.getMethodName().equals(disconnectAction) && method.getMethodName().equals(disconnectMethod));
    }

}

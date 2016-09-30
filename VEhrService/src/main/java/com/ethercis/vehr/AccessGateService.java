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
package com.ethercis.vehr;

import com.ethercis.logonservice.LogonService;
import com.ethercis.logonservice.access.ContextHolder;
import com.ethercis.logonservice.access.QueryUnit;
import com.ethercis.logonservice.access.SessionHolder;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.logonservice.session.ResponseHolder;
import com.ethercis.logonservice.session.SessionInfo;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.interfaces.services.I_PolicyManager;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.common.session.I_SessionHolder;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.SerializeHelper;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.ethercis.servicemanager.service.ServiceRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;


/**
 * Access gate to backend services
 * <p>
 * All queries to the backend transit through this point which:
 * <p>
 * <ul>
 * <li>Check the query via the security service
 * <li>Dispatch the query to the mapped service
 * <li>Perform the opt-respond depending on the policy
 * <li>Return the result to the caller
 * </ul>
 * <p>
 * This service requires the following services:
 * <p>
 * <ul>
 * <li>LogonService: to check the user session
 * <li>ServiceSecurityManager: to check the access rights and opt-respond
 * <li>RequestDispatcher: to forward the query to the corresponding service
 * <li>other services logonservice required depending on service map in dispatcher
 * </ul>
 * 
 * @author Christian Chevalley
 * 
 */

@com.ethercis.servicemanager.annotation.Service(id = "AccessGateService")
@RunLevelActions({
		@RunLevelAction(onStartupRunlevel = 9, sequence = 9, action = "LOAD"),
		@RunLevelAction(onShutdownRunlevel = 9, sequence = 1, action = "STOP") })

@ParameterSetting( identification = {
        @ParameterIdentification(id = "connect_path", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "vehr", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "rest/v1/session", type = String.class)
        }),
        @ParameterIdentification(id = "connect_method", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "connect", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "post", type = String.class)
        }),
        @ParameterIdentification(id = "connect_action", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "get", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "post", type = String.class)
        }),
        @ParameterIdentification(id = "disconnect_path", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "vehr", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "rest/v1/session", type = String.class)
        }),
        @ParameterIdentification(id = "disconnect_method", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "disconnect", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "delete", type = String.class)
        }),
        @ParameterIdentification(id = "disconnect_action", definition = {
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.STANDARD, name = "get", type = String.class),
                @ParameterDefinition(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, name = "delete", type = String.class)
        })

})

public class AccessGateService extends ClusterInfo implements AccessGateServiceMBean {

	final private String ME = "AccessGateService";
	final private String Version = "1.0";
	private static Logger log = LogManager.getLogger(AccessGateService.class);

	private RequestDispatcher requestDispatcher;
	private LogonService logonService;
	private I_PolicyManager policyManager;

    private String CONNECT_PATH;
    private String CONNECT_METHOD;
    private String CONNECT_ACTION;
    private String DISCONNECT_PATH;
    private String DISCONNECT_METHOD;
    private String DISCONNECT_ACTION;

	@Override
	public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)
			throws ServiceManagerException {

		this.global = global;
		// resolve the dispatcher (or throws an exception since the service
		// cannot be provided...)

        initCompatibilityMode();

        CONNECT_PATH = ParameterAnnotationHelper.parameterName(dialectSpace, "connect_path", this.getClass().getAnnotations());
        CONNECT_METHOD = ParameterAnnotationHelper.parameterName(dialectSpace, "connect_method", this.getClass().getAnnotations());
        CONNECT_ACTION = ParameterAnnotationHelper.parameterName(dialectSpace, "connect_action", this.getClass().getAnnotations());
        DISCONNECT_PATH = ParameterAnnotationHelper.parameterName(dialectSpace, "disconnect_path", this.getClass().getAnnotations());
        DISCONNECT_METHOD = ParameterAnnotationHelper.parameterName(dialectSpace, "disconnect_method", this.getClass().getAnnotations());
        DISCONNECT_ACTION = ParameterAnnotationHelper.parameterName(dialectSpace, "disconnect_action", this.getClass().getAnnotations());

        String dispatcherId = get("dispatcherService", "RequestDispatcher,1.0");
        String logonServiceId = get("logonService", "LogonService,1.0");

		ServiceRegistry registry = global.getServiceRegistry();

		this.requestDispatcher = (RequestDispatcher) registry.getService(dispatcherId);

		if (requestDispatcher == null) {
			log.error("RequestDispatcher service is not loaded, please make sure it is defined in your services.xml file");
			throw new ServiceManagerException(
					global,
					SysErrorCode.USER_CONFIGURATION,
					ME,
					"RequestDispatcher service is not loaded, please make sure it is defined in your services.xml file");
		}

		this.logonService = (LogonService) registry.getService(logonServiceId);

		if (logonService == null) {
			log.error("LogonService is not loaded, please make sure it is defined in your services.xml file");
			throw new ServiceManagerException(
					global,
					SysErrorCode.USER_CONFIGURATION,
					ME,
					"Dispatcher service is not loaded, please make sure it is defined in your services.xml file");
		}

//		String policyType = global.getProperty().get(Constants.POLICY_TYPE_TAG,
//				Constants.STR_POLICY_XML); // default mode is XML
//
//		this.policyManager = PolicyManagerFactory.getInstance(global,
//				policyType);

		log.info("Gate service started...");
	}

    /**
     * a connect action does not require credential checking
     * @param path
     * @param method
     * @return
     */
    private boolean isConnectAction(String path, MethodName action, MethodName method){

        return (path.equals(CONNECT_PATH) && action.getMethodName().equals(CONNECT_ACTION) && method.getMethodName().equals(CONNECT_METHOD));
    }


    /**
     * a disconnect action does require credential checking !
     * @param path
     * @param method
     * @return
     */
    private boolean isDisconnectAction(String path, MethodName action, MethodName method){

        return (path.equals(DISCONNECT_PATH) && action.getMethodName().equals(DISCONNECT_ACTION) && method.getMethodName().equals(DISCONNECT_METHOD));
    }


    /**
	 * Handle a client query
	 * 
	 * @param hdrprops
	 * @param path
	 * @param method
	 * @param parameters
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public Object queryHandler(MethodName action,
			I_SessionClientProperties hdrprops, String path, MethodName method,
			Object... parameters) throws ServiceManagerException {
		// TODO log access for any query
		// can deal only with parameters in a SessionClientProperties map...
		if (!(parameters.length == 1 && parameters[0] instanceof SessionClientProperties)) {
			log.warn("Internal error, can be called only for validating parameters in property map");
			throw new ServiceManagerException(global,
					SysErrorCode.INTERNAL_NOTIMPLEMENTED, ME,
					"Internal error, can be called only for validating parameters in property map");
		}

		// if the query is /vehr/connect then do a direct call to LogonService
		// it is assume that this is the only case where a null session-id is
		// accepted
		// this is hardcoded to avoid loopholes such logonservice using the services
		// without a valid
		// sessionid

		I_SessionClientProperties qryparms = (I_SessionClientProperties) parameters[0];

		if (isConnectAction(path, action, method)) {
			ResponseHolder responseHolder = (ResponseHolder)serviceConnect(hdrprops, path, action, method, parameters);
			if (responseHolder != null) {
				Map<String, Object> retMap = new HashMap<>();
				retMap.put("action", "CREATE");
				ClientProperty sessionClientProperty = responseHolder.getSessionClientProperties().getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace));
				retMap.put("sessionId", sessionClientProperty.toString());
				Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + "sessionId="+sessionClientProperty.toString());
				retMap.putAll(metaref);
				retMap.put("headers", responseHolder.getSessionClientProperties());
				return retMap;
			}
		}
		if (hdrprops == null) { // invalid query, session id cannot be found...
			throw new ServiceManagerException(global,
					SysErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED, ME,
					"Invalid format for query");
		}
		String sessionid = hdrprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), (String) null);

		SessionInfo sessioninfo = logonService.check(sessionid);
		I_Session sessionSecurityContext = sessioninfo.getSecuritySession();
		I_Authenticate subjectSecurityContext = sessionSecurityContext.getAuthenticate();
		I_SessionHolder sessionholder = new SessionHolder(sessioninfo);
		//Set Authentication to SecurityContext for acces this value from other object
		//by using SecurityContext.getAuthentication();

		SecurityContext.setAuthentication(new Authentication(sessionSecurityContext,subjectSecurityContext));
		// build a query unit for a Path type query
		I_QueryUnit qryunit = new QueryUnit(dialectSpace, action, Constants.PATH_TAG, hdrprops, method, path, qryparms);
		I_ContextHolder contextholder = new ContextHolder(action, qryunit);

		// Log query
		//AccessLog.log(subjectSecurityContext.gtName(), method.getMethodName(),path);
		//AccessLog.info(qryunit.toString());
		AccessLog.info("userId="+subjectSecurityContext.getUserId()+",method="+method.getMethodName()+",path="+path+",qryparams="+qryparms.toString());

		if (!sessionSecurityContext.isAuthorized(sessionholder, contextholder)) {
			String msg = "Subject:" + subjectSecurityContext.getUserId()
					+ " is not authorized for '" + method + "'" + " in path:'"
					+ path + "'";
			log.warn(msg);
			throw new ServiceManagerException(global,
					SysErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED, ME,
					msg);
		}

        if (isDisconnectAction(path, action, method)){ //add the secret session id into the parameters
            if (!qryunit.getParameters().getClientProperties().containsKey(I_SessionManager.SECRET_SESSION_ID(dialectSpace))){
                qryunit.getParameters().addClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), sessionid);
            }

        }

        //add the secret session id for internal purpose
        qryunit.getParameters().addClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, sessionid);

		// so far so good... dispatch the query
		return  requestDispatcher.dispatch(qryunit);
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
	private Object serviceConnect(I_SessionClientProperties hdrprops,
			String path, MethodName action, MethodName method, Object... parameters)
			throws ServiceManagerException {
		// if the query is /vehr/connect then do a direct call to LogonService
		// it is assume that this is the only case where a null session-id is
		// accepted
		// this is hardcoded to avoid loopholes such logonservice using the services
		// without a valid
		// sessionid

		I_SessionClientProperties qryparms = (I_SessionClientProperties) parameters[0];

		if (isConnectAction(path, action, method)) {
			ResponseHolder ret = null;
			try {
				//add header x- fields into the query parameters
				Map<String, String> headerMap = hdrprops.clientProps2StringMap();

				for (String parm: headerMap.keySet()){
					if (parm.contains("x-")){
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
					AccessLog.log(userId == null ? "BYPASS_CREDENTIAL" : userId,method.getMethodName(), path);
					return ret;
				}
			} catch (ServiceManagerException soae) {
				// Log connect
				AccessLog.log("unknown",method.getMethodName(), path);
				throw soae;
			}

		}

		I_SessionInfo sessioninfo = null;

		String sessionid = hdrprops.getClientProperty(
				I_SessionManager.SECRET_SESSION_ID(dialectSpace), (String) null);
		sessioninfo = logonService.check(sessionid);
		I_Session sessionSecurityContext = sessioninfo.getSecuritySession();
		I_Authenticate subjectSecurityContext = sessionSecurityContext.getAuthenticate();
		// second hard coded call, disconnect session
		if (isDisconnectAction(path, action, method)) {

			// Log disconnect
			AccessLog.log(subjectSecurityContext.getUserId(),
					method.getMethodName(), path);

			disconnect(sessionid);
			return null;
		}
		throw new ServiceManagerException(global,
				SysErrorCode.INTERNAL_NOTIMPLEMENTED, ME,
				"Internal error, method is unknown:" + method);
	}

	/**
	 * directly call LogonService for a connect
	 * 
	 * @param props
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	private Object connect(I_SessionClientProperties props, String ssid)
			throws ServiceManagerException {

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
	 * return the method type logonservice found in map
	 * 
	 * @param path
	 * @param method
	 * @return
	 */
	public int getMappedMethodReturnType(MethodName action, String path,
			MethodName method) {
		return requestDispatcher.getMappedMethodReturnType(action, path, method);
	}

	public boolean isMappedMethodAsync(MethodName action, String path,
			MethodName method) {
		return requestDispatcher.isMappedMethodAsync(action, path, method);
	}


	/**
	 * little helper to get a ClientProperty and deal with array of parameters
	 * (base64 encoded List<String>)
	 * 
	 * @param cp
	 * @return
	 * @throws java.io.IOException
	 */
	private List<String> createParameterList(ClientProperty cp) throws IOException {
		SerializeHelper helper = new SerializeHelper(global);
		List<String> ret = new ArrayList<String>();
		if (cp.getType() == Constants.TYPE_STRING)
			ret.add(cp.getStringValue());
		else {
			if (cp.getType() == Constants.TYPE_BLOB) {
				String[] arr = (String[]) helper.deserializeObject(cp
						.getBlobValue());
				ret = Arrays.asList(arr);
			}
		}
		return ret;
	}

}

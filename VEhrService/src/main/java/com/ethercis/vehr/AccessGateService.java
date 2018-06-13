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
import com.ethercis.logonservice.access.QueryUnit;
import com.ethercis.logonservice.security.I_SecurityManager;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.SerializeHelper;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.ethercis.servicemanager.service.ServiceRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import com.ethercis.sessionlogger.I_SessionLoggerService;
//import com.ethercis.sessionlogger.SessionLoggerService;


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
 */

@com.ethercis.servicemanager.annotation.Service(id = "AccessGateService")
@RunLevelActions({
        @RunLevelAction(onStartupRunlevel = 9, sequence = 9, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 1, action = "STOP")})

@ParameterSetting(identification = {
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
    final private String version = "1.0";
    private static Logger log = LogManager.getLogger(AccessGateService.class);

    private RequestDispatcher requestDispatcher;
    private LogonService logonService;
    private I_QueryWrapper queryWrapper = null;

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)
            throws ServiceManagerException {

        this.global = global;

        initCompatibilityMode();

        ServiceRegistry registry = global.getServiceRegistry();

        String dispatcherId = get("dispatcherService", "RequestDispatcher,1.0");
        this.requestDispatcher = (RequestDispatcher) registry.getService(dispatcherId);
        if (requestDispatcher == null) {
            log.error("RequestDispatcher service is not loaded, please make sure it exists in your classpath");
            throw new ServiceManagerException(
                    global,
                    SysErrorCode.USER_CONFIGURATION,
                    ME,
                    "RequestDispatcher service is not loaded, please make sure it exists in your classpath");
        }

        int securityMode = securityMode();

        if (securityMode == Constants.POLICY_SHIRO) {
            this.logonService = (LogonService) registry.getService(get("logonService", "LogonService,1.0"));
            if (logonService == null) {
                throw new ServiceManagerException(global,
                        SysErrorCode.RESOURCE_UNAVAILABLE, ME,
                        "Required LogonService is not loaded, please check your classpath");
            }
            queryWrapper = new SessionManagedQuery(global, logonService, dialectSpace, this.getClass().getAnnotations());
        } else if (securityMode == Constants.POLICY_JWT) {
            queryWrapper = new JwtQuery(global);
        }

        log.info(ME + "," + version + " service started...");
    }

    @Override
    public void shutdown() throws ServiceManagerException {
        super.shutdown();
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
    public Object queryHandler(MethodName action, I_SessionClientProperties hdrprops, String path, MethodName method, Object... parameters) throws ServiceManagerException {
        // TODO log access for any query
        // can deal only with parameters in a SessionClientProperties map...
        if (!(parameters.length == 1 && parameters[0] instanceof SessionClientProperties)) {
            log.warn("Internal error, can be called only for validating parameters in property map");
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_NOTIMPLEMENTED, ME, "Internal error, can be called only for validating parameters in property map");
        }

        //connect is only valid in full session context
        if (queryWrapper.isConnectAction(path, action, method)) {
            return queryWrapper.connect(action, hdrprops, path, method, parameters);
        }

        if (hdrprops == null) { // invalid query, no headers...
            throw new ServiceManagerException(global, SysErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED, ME, "Invalid format for query, no headers");
        }

        // build a query unit for a Path type query
        I_SessionClientProperties qryparms = (I_SessionClientProperties) parameters[0];
        I_QueryUnit qryunit = new QueryUnit(dialectSpace, action, Constants.PATH_TAG, hdrprops, method, path, qryparms);

        qryunit = queryWrapper.query(qryunit, qryparms, hdrprops, path, action, method);

        // so far so good... dispatch the query
        // set the session context
//        rls.setRole(qryunit);
        Object response = requestDispatcher.dispatch(qryunit);
//        rls.resetRole();
        return response;
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

    private int securityMode() throws ServiceManagerException {
        //check the policy set in the security manager
        I_SecurityManager securityManager = (I_SecurityManager) global.getServiceRegistry().getService(get("serviceSecurityManager", "ServiceSecurityManager,1.0"));
        return securityManager.getPolicyMode();
    }

}

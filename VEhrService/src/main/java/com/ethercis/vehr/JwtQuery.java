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

import com.ethercis.authenticate.Authenticate;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.eclipse.jetty.http.HttpHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by christian on 6/1/2018.
 */
public class JwtQuery implements I_QueryWrapper {

    RunTimeSingleton runTimeSingleton;
    I_Authenticate authenticate;

    public JwtQuery(RunTimeSingleton runTimeSingleton) throws ServiceManagerException {
        this.runTimeSingleton = runTimeSingleton;
        this.authenticate = Authenticate.newWrapper(runTimeSingleton, Constants.POLICY_JWT, null);
    }

    @Override
    public Map connect(MethodName action, I_SessionClientProperties hdrprops, String path, MethodName method, Object... parameters) throws ServiceManagerException {
        return null;
    }

    @Override
    public I_QueryUnit query(I_QueryUnit qryunit, I_SessionClientProperties qryparms, I_SessionClientProperties hdrprops, String path, MethodName action, MethodName method) throws ServiceManagerException {

        ClientProperty token = hdrprops.getClientProperties().get(HttpHeader.AUTHORIZATION.asString());

        if (token == null) {
            throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_ILLEGALARGUMENT, "NULL token in query");
        }

        if (!token.getStringValue().startsWith(Constants.TOKEN_TYPE_BEARER)) {
            throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_ILLEGALARGUMENT, "JWT must be prefixed with 'Bearer'");
        }

        String jwt = token.getStringValue().split(" ")[1];

        authenticate.checkCredential(jwt);

        String userId = authenticate.getUserId();
        List<I_Principal> principals = authenticate.getPrincipals();

        //pass these objects into the properties for dispatch
        qryunit.getParameters().addClientProperty(Constants.TOKEN_USER_SESSION, userId);

        String principalString = new Principal(principals).toString();
        if (principalString != null)
            qryunit.getParameters().addClientProperty(Constants.TOKEN_PRINCIPAL_SESSION, principalString);

        return qryunit;
    }

    @Override
    public boolean isConnectAction(String path, MethodName action, MethodName method) {
        return false;
    }

    @Override
    public boolean isDisconnectAction(String path, MethodName action, MethodName method) {
        return false;
    }
}

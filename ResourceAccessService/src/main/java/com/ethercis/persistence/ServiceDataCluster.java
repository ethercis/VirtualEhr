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
package com.ethercis.persistence;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
public abstract class ServiceDataCluster extends ClusterInfo {

    private I_ResourceService resourceService;
    protected I_AuditSetter auditSetter;
    private I_DomainAccess sessionDomainAccess;


    protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        this.global = global;

        initCompatibilityMode();

        resourceService = ClusterInfo.getRegisteredService(global, "ResourceService", "1.0", new Object[]{null});
        sessionDomainAccess = null;
    }

    protected I_DomainAccess getDataAccess() {
        if (resourceService == null || resourceService.getDomainAccess() == null)
            throw new IllegalArgumentException("ResourceService seems not properly configured... Aborting...");

        if (sessionDomainAccess != null)
            return sessionDomainAccess;
        else
            return resourceService.getDomainAccess();
    }

    protected void queryProlog(I_SessionClientProperties props) throws ServiceManagerException {
        //prepare for audit log
//
        String policyType = global.getProperty().get(Constants.POLICY_TYPE_TAG, Constants.STR_POLICY_DEBUG);

        if (policyType.equals(Constants.STR_POLICY_JWT)) {
            String subject = props.getClientProperty(Constants.TOKEN_USER_SESSION).getStringValue();

            String principal = null;
            if (props.getClientProperties().containsKey(Constants.TOKEN_PRINCIPAL_SESSION))
                principal = props.getClientProperty(Constants.TOKEN_PRINCIPAL_SESSION).getStringValue();

            if (global.getProperty().get(Constants.DB_SECURITY_ROLE, true))
                sessionDomainAccess = new RoleControlledSession(global, resourceService).setRole(subject, principal);

            auditSetter = new JwtAuditSetter(global, subject, principal).handleProperties(resourceService.getDomainAccess(), props);

        } else if (policyType.equals(Constants.STR_POLICY_SHIRO)) { //session based
            auditSetter = new AuditSetter(global).handleProperties(resourceService.getDomainAccess(), props);

            if (global.getProperty().get(Constants.DB_SECURITY_ROLE, false)) {
                String subjectName = auditSetter.getSessionSubjectName(auditSetter.getSessionId());
                sessionDomainAccess = new RoleControlledSession(global, resourceService).setRole(subjectName, null);
            }
        } else {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "ServiceDataCluster", "No security policy activated");
        }

    }

}

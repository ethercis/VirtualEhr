package com.ethercis.persistence;/*
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

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.persistence.I_ResourceService;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.jooq.Configuration;
import org.jooq.exception.DataAccessException;

import java.sql.Connection;

/**
 * Created by christian on 6/6/2018.
 */
public class RoleControlledSession {

    I_ResourceService resourceService;
    RunTimeSingleton runTimeSingleton;

    public RoleControlledSession(RunTimeSingleton runTimeSingleton, I_ResourceService resourceService) {
        this.resourceService = resourceService;
        this.runTimeSingleton = runTimeSingleton;
    }

    public I_DomainAccess setRole(String subject, String principal) throws ServiceManagerException {

        I_DomainAccess sessionDomainAccess = resourceService.getDomainAccess();

        if (runTimeSingleton.getProperty().get(Constants.DB_SECURITY_ROLE, true)) {

            Connection connection = resourceService.getDomainAccess().getConnection();
            Configuration configuration = resourceService.getDomainAccess().getDataAccess().getContext().configuration().derive();
            configuration.set(connection);
//
            try {
                sessionDomainAccess = I_DomainAccess.getInstance(resourceService.getDomainAccess().getDataAccess());
                sessionDomainAccess.getContext().configuration().set(connection);
            } catch (Exception e) {
                throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_ILLEGALARGUMENT, "ServiceDataCluster", "Could not set session derivative:" + e);
            }

            try {
                sessionDomainAccess.getContext().query("SET ROLE '" + new DbRole(runTimeSingleton).role(subject, principal) + "';").execute();
            } catch (DataAccessException e) {
                throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, "ServiceDataCluster", "Role/user denied access:" + new DataAccessExceptionMessage(e).error());
            }
        }
        return sessionDomainAccess;
    }
}

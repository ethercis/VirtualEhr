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
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
public abstract class ServiceDataCluster extends ClusterInfo {

    private I_ResourceService resourceService;

    protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)throws ServiceManagerException  {
        this.global = global;

        initCompatibilityMode();

        resourceService = ClusterInfo.getRegisteredService(global, "ResourceService", "1.0", new Object[] {null});
    }

    protected I_DomainAccess getDataAccess(){
        if (resourceService == null || resourceService.getDomainAccess() == null)
            throw new IllegalArgumentException("ResourceService seems not properly configured... Aborting...");

        return resourceService.getDomainAccess();
    }

}

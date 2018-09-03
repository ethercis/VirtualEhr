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
package com.ethercis.systemservice;

import com.ethercis.dao.access.interfaces.I_SystemAccess;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.AnnotatedMBean;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
@Service(id ="SystemService", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 3, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 3, action = "STOP") })

public class SystemService extends ServiceDataCluster implements I_SystemService, SystemServiceMBean {

    final private String ME = "SystemService";
    final private String Version = "1.0";
    private Logger log = LogManager.getLogger(SystemService.class);

    @Override
    protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(global, serviceInfo);
//        putObject(I_Info.JMX_PREFIX+ME, this);
        AnnotatedMBean.RegisterMBean(this.getClass().getCanonicalName(), SystemServiceMBean.class, this);
    }

    @Override
    public UUID create(String description, String settings) throws Exception {
        I_SystemAccess systemAccess = I_SystemAccess.getInstance(getDataAccess(), description, settings);
        return systemAccess.commit();
    }

    @Override
    public Integer delete(UUID id) throws Exception {
        I_SystemAccess systemAccess = I_SystemAccess.retrieveInstance(getDataAccess(), id);
        return systemAccess.delete();
    }

    @Override
    public UUID retrieve(String settings) throws Exception {
        return I_SystemAccess.retrieveInstanceId(getDataAccess(), settings);
    }

    @Override
    public UUID getOrCreateSystemId(String description, String systemAddress){
        UUID systemId = null;
        try {
            systemId = retrieve(systemAddress);
        } catch (Exception e){
            ;
        }

        if (systemId == null){
            try {
                systemId = create("REMOTE CLIENT", systemAddress);
            }
            catch (Exception e){
                throw new IllegalArgumentException("Could not create client with settings:"+systemAddress);
            }
        }

        return systemId;

    }

    @Override
    public String getBuildVersion() {
        return BuildVersion.versionNumber;
    }

    @Override
    public String getBuildId() {
        return BuildVersion.projectId;
    }

    @Override
    public String getBuildDate() {
        return BuildVersion.buildDate;
    }

    @Override
    public String getBuildUser() {
        return BuildVersion.buildUser;
    }

}

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
package com.ethercis.partyservice;

import com.ethercis.dao.access.interfaces.I_PartyIdentifiedAccess;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;
import org.openehr.rm.common.generic.PartyIdentified;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
@Service(id ="PartyIdentifiedService", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 3, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 3, action = "STOP") })
public class PartyIdentifiedService extends ServiceDataCluster implements I_PartyIdentifiedService {

    final private String ME = "PartyIdentifiedService";
    final private String Version = "1.0";
    private Logger log = Logger.getLogger(PartyIdentifiedService.class);

    @Override
    protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(global, serviceInfo);

    }

    @Override
    public UUID retrievePartyByIdentifier(String idCode, String issuer){
        return I_PartyIdentifiedAccess.retrievePartyByIdentifier(getDataAccess(), idCode, issuer);
    }

    @Override
    public UUID getOrCreateParty(PartyIdentified partyIdentified){
        return I_PartyIdentifiedAccess.getOrCreateParty(getDataAccess(), partyIdentified);
    }

    @Override
    public UUID getOrCreateParty(String name, String idCode, String issuer, String assigner, String typeName){
        return I_PartyIdentifiedAccess.getOrCreateParty(getDataAccess(), name, idCode, issuer, assigner, typeName);
    }

    @Override
    public Integer deleteParty(UUID id){
        return I_PartyIdentifiedAccess.deleteInstance(getDataAccess(), id);
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "delete", path = "vehr/party", responseType = ResponseType.String),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "DELETE", method = "delete", path = "rest/v1/party", responseType = ResponseType.String)
        }
    )
    public Integer delete(I_SessionClientProperties props) throws ServiceManagerException {
        UUID uuid = UUID.fromString(props.getClientProperty("id", ""));
        return I_PartyIdentifiedAccess.deleteInstance(getDataAccess(), uuid);
    }
}

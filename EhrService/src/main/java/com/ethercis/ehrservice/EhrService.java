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
package com.ethercis.ehrservice;

import com.ethercis.compositionservice.I_CompositionService;
import com.ethercis.dao.access.interfaces.I_ConceptAccess;
import com.ethercis.dao.access.interfaces.I_EhrAccess;
import com.ethercis.dao.access.interfaces.I_PartyIdentifiedAccess;
import com.ethercis.dao.access.interfaces.I_SystemAccess;
import com.ethercis.dao.access.jooq.EhrAccess;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.building.util.CompositionAttributesHelper;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.datatypes.basic.DvIdentifier;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 6/30/2015.
 */
@Service(id ="EhrService", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP") })

public class EhrService extends ServiceDataCluster implements I_EhrService, EhrServiceMBean {

    final private String ME = "EhrService";
    final private String Version = "1.0";

    private EhrAccess.PARTY_MODE subjectMode;

    private Logger log = Logger.getLogger(EhrService.class);

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)throws ServiceManagerException {
        super.doInit(global, serviceInfo);

        //get the resolution mode for subject
        subjectMode = EhrAccess.PARTY_MODE.valueOf(global.getProperty().get("ehr.subject.mode", EhrAccess.PARTY_MODE.EXTERNAL_REF.toString()));

        log.info("Subject identification mode is set to:"+subjectMode.name());

        //get a resource service instance
        putObject(I_Info.JMX_PREFIX + ME, this);
        log.info("EhrService service started...");
    }

    @Override
    public UUID create(UUID partyId, UUID systemId) throws Exception {
        //check if an Ehr already exists for this party
        if (I_EhrAccess.checkExist(getDataAccess(), partyId))
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Specified party has already an EHR set (partyId="+partyId+")");

        I_EhrAccess ehrAccess = I_EhrAccess.getInstance(getDataAccess(), partyId, systemId, null, null);
        return ehrAccess.commit(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
    }

    @Override
    public UUID create(UUID partyId, UUID systemId, String otherDetailsXml, String otherDetailsTemplateId) throws Exception {
        //check if an Ehr already exists for this party
        if (I_EhrAccess.checkExist(getDataAccess(), partyId))
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Specified party has already an EHR set (partyId="+partyId+")");

        I_EhrAccess ehrAccess = I_EhrAccess.getInstance(getDataAccess(), partyId, systemId, null, null);
        if (otherDetailsXml != null && otherDetailsTemplateId != null){
            //the xml string is given within a CDATA
            otherDetailsXml = otherDetailsXml.replace("<![CDATA[", "");
            otherDetailsXml = otherDetailsXml.replace("]]>", "");
            Locatable otherDetails = I_ContentBuilder.parseOtherDetailsXml(new ByteArrayInputStream(otherDetailsXml.getBytes()));
            ehrAccess.setOtherDetails(otherDetails, otherDetailsTemplateId);
        }
        return ehrAccess.commit(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
    }

    @Override
    public UUID retrieve(String subjectId, String nameSpace){

        UUID subjectUuid;
        switch (subjectMode) {
            case IDENTIFIER:
                subjectUuid = I_PartyIdentifiedAccess.retrievePartyByIdentifier(getDataAccess(), subjectId, nameSpace);
                return I_EhrAccess.retrieveInstanceBySubject(getDataAccess(), subjectUuid);
            case EXTERNAL_REF:
                subjectUuid = I_PartyIdentifiedAccess.findReferencedParty(getDataAccess(), subjectId, nameSpace, CompositionAttributesHelper.DEMOGRAPHIC, CompositionAttributesHelper.PARTY);
                return I_EhrAccess.retrieveInstanceBySubject(getDataAccess(), subjectUuid);
        }
        return  null;
    }

    @Override
    public Integer delete(UUID ehrId) throws Exception {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
        return ehrAccess.delete();
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "get", path = "vehr/ehr", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/ehr", responseType = ResponseType.Json)
    })
    public Object retrieve(I_SessionClientProperties props) throws Exception {
        auditSetter.handleProperties(getDataAccess(), props);
        String subjectId = props.getClientProperty(I_EhrService.SUBJECTID_PARAMETER, (String) null);
        String nameSpace = props.getClientProperty(I_EhrService.SUBJECTNAMESPACE_PARAMETER, (String)null);
        String sessionId = auditSetter.getSessionId();

        if (subjectId == null || nameSpace == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid user id or namespace in query");

        UUID ehrId = retrieve(subjectId, nameSpace);

        if (ehrId == null) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Invalid or null ehrId");
        }

        setSessionEhr(sessionId, ehrId);

        return buildEhrStatusMap(ehrId);
    }

    private Map<String, Object> buildEhrStatusMap(UUID ehrUuid) throws Exception {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);
        if (ehrAccess == null) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Invalid or null ehrId");
        }

        Map<String, String> subjectIds = I_EhrAccess.fetchSubjectIdentifiers(getDataAccess(), ehrUuid);
        I_SystemAccess systemAccess = I_SystemAccess.retrieveInstance(getDataAccess(), ehrAccess.getSystemId());

        Map<String, String> statusMap = new HashMap(){{
            put("subjectIds", subjectIds);
            put("queryable", ehrAccess.isQueryable());
            put("modifiable", ehrAccess.isModifiable());
            if (ehrAccess.isSetOtherDetails()){
                put("other_details", "<![CDATA["+ehrAccess.exportOtherDetailsXml()+"]]>");
            }
            put("systemSettings", systemAccess.getSettings());
            put("systemDescription", systemAccess.getDescription());
        }};

        Map<String, Object> retmap = new HashMap<>();

        retmap.put("action", "RETRIEVE");
        retmap.put("ehrStatus", statusMap);
        retmap.put(I_EhrService.EHRID_PARAMETER, ehrUuid.toString());
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID+"="+ehrUuid);
        retmap.putAll(metaref);

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "get", path = "vehr/ehr/status", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/ehr/status", responseType = ResponseType.Json)
    })
    public Object retrieveStatus(I_SessionClientProperties props) throws Exception {
        auditSetter.handleProperties(getDataAccess(), props);
        UUID ehrUuid;
        try {
            ehrUuid = UUID.fromString(props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null));
        }
        catch (Exception e){
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid ehrId:'"+props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null)+"'");
        }
        String sessionId = auditSetter.getSessionId();

        if (ehrUuid == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid or null ehrId");

        setSessionEhr(sessionId, ehrUuid);

        return buildEhrStatusMap(ehrUuid);

    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/ehr", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/ehr", responseType = ResponseType.Json)
    })
    public Object create(I_SessionClientProperties props) throws Exception {
        auditSetter.handleProperties(getDataAccess(), props);
        String subjectIdCode = props.getClientProperty(I_EhrService.SUBJECTID_PARAMETER, (String) null);
        String subjectNameSpace = props.getClientProperty(I_EhrService.SUBJECTNAMESPACE_PARAMETER, (String) null);
        String systemSettings = props.getClientProperty("systemSettings", (String) null);

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String)null);

        String otherDetailsXml = null;
        String otherDetailsTemplateId = null;

        if (content != null) {
            Gson json = new GsonBuilder().create();
            Map<String, Object> atributes = json.fromJson(content, Map.class);

            otherDetailsXml = (String)atributes.getOrDefault("otherDetails", null);
            otherDetailsTemplateId = (String)atributes.getOrDefault("otherDetailsTemplateId", null);
        }

        String sessionId = auditSetter.getSessionId();

        if (subjectIdCode == null || subjectNameSpace == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid or missing user id or namespace in query");

        UUID subjectUuid = null;
        try {
            switch (subjectMode) {
                case IDENTIFIER:
                    subjectUuid = I_PartyIdentifiedAccess.retrievePartyByIdentifier(getDataAccess(), subjectIdCode, subjectNameSpace);
                    break;
                case EXTERNAL_REF:
                    subjectUuid = I_PartyIdentifiedAccess.getOrCreatePartyByExternalRef(getDataAccess(), null, subjectIdCode, subjectNameSpace, CompositionAttributesHelper.DEMOGRAPHIC, CompositionAttributesHelper.PARTY);
                    break;
            }
        } catch (Exception e){
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Ehr cannot be created, there is no existing subject with this identifier:"+subjectIdCode+"::"+subjectNameSpace);
        }

        if (subjectUuid == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Subject is not existing for id code="+subjectIdCode+" ,issuer="+subjectNameSpace);

        UUID systemId = null;
        if (systemSettings != null){ //NB: a systemSettings == null is valid, it is defaulted to the local system
            try {
                systemId = I_SystemAccess.retrieveInstanceId(getDataAccess(), systemSettings);
            } catch (Exception e) {
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "System is not existing for settings="+systemSettings);
            }
        }

        UUID ehrId = create(subjectUuid, systemId, otherDetailsXml, otherDetailsTemplateId);

        Map<String, Object> retmap = new HashMap<>();
        retmap.put(I_EhrService.EHRID_PARAMETER, ehrId.toString());
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID+"="+ehrId);
        retmap.putAll(metaref);
        retmap.put("action", "CREATE");

        setSessionEhr(sessionId, ehrId);

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "POST", method = "update", path = "vehr/ehr/status", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/ehr/status", responseType = ResponseType.Json)
    })
    public Object updateStatus(I_SessionClientProperties props) throws Exception {
        auditSetter.handleProperties(getDataAccess(), props);
        String sessionId = auditSetter.getSessionId();
        UUID ehrId = UUID.fromString(props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null));

        if (ehrId == null )
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "No valid ehr Id parameter found in query");

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String)null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content cannot be empty for updating ehr status");

        //get the map structure from the passed content string
        Gson json = new GsonBuilder().create();
        Map<String, Object> atributes = json.fromJson(content, Map.class);

        //retrieve the ehr to update
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);

        if (ehrAccess == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Passed ehr Id does not match an existing EHR");

        if (atributes.containsKey("modifiable")) {
            ehrAccess.setModifiable((Boolean)atributes.get("modifiable"));
        }

        if (atributes.containsKey("queryable"))
            ehrAccess.setQueryable((Boolean)atributes.get("queryable"));

        if (atributes.containsKey(I_EhrService.SUBJECTID_PARAMETER) && atributes.containsKey(I_EhrService.SUBJECTNAMESPACE_PARAMETER)){
            String subjectId = (String)atributes.get(I_EhrService.SUBJECTID_PARAMETER);
            String subjectNameSpace = (String)atributes.get(I_EhrService.SUBJECTNAMESPACE_PARAMETER);
            UUID partyId;
            switch (subjectMode) {
                case IDENTIFIER:
                    List<DvIdentifier> identifiers = new ArrayList<>();
                    identifiers.add(new DvIdentifier(subjectNameSpace, "", subjectId, ""));
                    partyId = I_PartyIdentifiedAccess.findIdentifiedParty(getDataAccess(), identifiers);

                    if (partyId != null) {
                        ehrAccess.setParty(partyId);
                    }
                    break;
                case EXTERNAL_REF:
                    partyId = I_PartyIdentifiedAccess.getOrCreateParty(getDataAccess(), null, subjectId, subjectNameSpace, CompositionAttributesHelper.DEMOGRAPHIC, CompositionAttributesHelper.PARTY);

                    if (partyId != null) {
                        ehrAccess.setParty(partyId);
                    }
                    break;

            }
        }

        ehrAccess.update(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), null, I_ConceptAccess.ContributionChangeType.modification, auditSetter.getDescription());

        Map<String, Object> retmap = new HashMap<>();
        retmap.put(I_EhrService.EHRID_PARAMETER, ehrId.toString());
        retmap.put("action", "UPDATE");
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID+"="+ehrId);
        retmap.putAll(metaref);
        setSessionEhr(sessionId, ehrId);

        return retmap;

    }

    private void setSessionEhr(String sessionId, UUID ehrId) throws ServiceManagerException {
        I_SessionManager sessionManager = getRegisteredService(getGlobal(), "LogonService", "1.0", null);
        //retrieve the session manager
        sessionManager.getSessionUserMap(sessionId).put(I_CompositionService.EHR_ID, ehrId);
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "delete", path = "vehr/ehr", responseType = ResponseType.String),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "DELETE", method = "delete", path = "rest/v1/ehr", responseType = ResponseType.String)
    })
    public String delete(I_SessionClientProperties props) throws ServiceManagerException {
        auditSetter.handleProperties(getDataAccess(), props);
        String ehrId = props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null);

        if (ehrId == null || ehrId.length() == 0)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "No valid ehr Id parameter found in query");

        UUID ehrUuid = UUID.fromString(ehrId);

        try {
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);

            if (ehrAccess == null)
                throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Passed ehr Id does not match an existing EHR");

            Integer result = ehrAccess.delete(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());

            if (result > 0)
                return "Done";
            else
                return "Could not delete Ehr (id="+ehrId+")";
        }
        catch (Exception e){
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_UNAVAILABLE, ME, "Problem accessing DB"+e.getMessage());
        }


    }

}

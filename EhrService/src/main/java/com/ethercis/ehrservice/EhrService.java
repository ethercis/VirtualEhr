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
import com.ethercis.dao.access.interfaces.*;
import com.ethercis.dao.access.jooq.EhrAccess;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.building.util.CompositionAttributesHelper;
import com.ethercis.ehr.encode.CompositionSerializer;
import com.ethercis.ehr.encode.EncodeUtil;
import com.ethercis.ehr.keyvalues.EcisFlattener;
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
import com.ethercis.servicemanager.jmx.AnnotatedMBean;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.ethercis.transform.rawjson.RawJsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.datatypes.basic.DvIdentifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 6/30/2015.
 */
@Service(id = "EhrService", version = "1.0", system = true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP")})

public class EhrService extends ServiceDataCluster implements I_EhrService, EhrServiceMBean {

    final private String ME = "EhrService";
    final private String Version = "1.0";

    private EhrAccess.PARTY_MODE subjectMode;

    private Logger log = LogManager.getLogger(EhrService.class);

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(global, serviceInfo);

        //get the resolution mode for subject
        subjectMode = EhrAccess.PARTY_MODE.valueOf(global.getProperty().get("ehr.subject.mode", EhrAccess.PARTY_MODE.EXTERNAL_REF.toString()));

        log.info("Subject identification mode is set to:" + subjectMode.name());

        //get a resource service instance
//        putObject(I_Info.JMX_PREFIX + ME, this);
        AnnotatedMBean.RegisterMBean(this.getClass().getCanonicalName(), EhrServiceMBean.class, this);
        log.info("EhrService service started...");
    }

    @Override
    public UUID create(UUID partyId, UUID systemId) throws Exception {
        //check if an Ehr already exists for this party
        if (I_EhrAccess.checkExist(getDataAccess(), partyId))
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Specified party has already an EHR set (partyId=" + partyId + ")");

        I_EhrAccess ehrAccess = I_EhrAccess.getInstance(getDataAccess(), partyId, systemId, null, null);
        return ehrAccess.commit(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
    }

    @Override
    public UUID create(UUID partyId, UUID systemId, Object otherDetails, String otherDetailsTemplateId) throws Exception {
        //check if an Ehr already exists for this party
        if (I_EhrAccess.checkExist(getDataAccess(), partyId))
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Specified party has already an EHR set (partyId=" + partyId + ")");

        I_EhrAccess ehrAccess = I_EhrAccess.getInstance(getDataAccess(), partyId, systemId, null, null);
        if (otherDetails instanceof String && otherDetails != null && otherDetailsTemplateId != null) {
            String otherDetailsStr = (String) otherDetails;
            if (otherDetailsStr.startsWith("<")) { //assume XML
                //the xml string is given within a CDATA
                otherDetailsStr = otherDetailsStr.replace("<![CDATA[", "");
                otherDetailsStr = otherDetailsStr.replace("]]>", "");
                Locatable locatable = I_ContentBuilder.parseOtherDetailsXml(new ByteArrayInputStream(otherDetailsStr.getBytes()));
                ehrAccess.setOtherDetails(locatable, otherDetailsTemplateId);
            }
            if (otherDetailsStr.startsWith("{")) { //assume JSON
                ehrAccess.setOtherDetails(otherDetailsStr, otherDetailsTemplateId);
            } else
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Could not identify other_details format:" + otherDetailsStr);
        } else if (otherDetails instanceof Map) {
            Map<String, Object> other_details = new HashMap<>();
            other_details.put(CompositionSerializer.TAG_OTHER_DETAILS.substring(1), otherDetails);
            ehrAccess.setOtherDetails(other_details, null);
        }
        return ehrAccess.commit(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
    }

    @Override
    public UUID retrieve(String subjectId, String nameSpace) {

        UUID subjectUuid;
        switch (subjectMode) {
            case IDENTIFIER:
                subjectUuid = I_PartyIdentifiedAccess.retrievePartyByIdentifier(getDataAccess(), subjectId, nameSpace);
                return I_EhrAccess.retrieveInstanceBySubject(getDataAccess(), subjectUuid);
            case EXTERNAL_REF:
                subjectUuid = I_PartyIdentifiedAccess.findReferencedParty(getDataAccess(), subjectId, CompositionAttributesHelper.DEMOGRAPHIC, nameSpace, CompositionAttributesHelper.PARTY);
                return I_EhrAccess.retrieveInstanceBySubject(getDataAccess(), subjectUuid);
        }
        return null;
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
        queryProlog(props);
        String subjectId = props.getClientProperty(I_EhrService.SUBJECTID_PARAMETER, (String) null);
        String nameSpace = props.getClientProperty(I_EhrService.SUBJECTNAMESPACE_PARAMETER, (String) null);
        String sessionId = auditSetter.getSessionId();
        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, "XML"));


        if (subjectId == null || nameSpace == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid user id or namespace in query");

        UUID ehrId = retrieve(subjectId, nameSpace);

        if (ehrId == null) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Invalid or null ehrId");
        }

        setSessionEhr(sessionId, ehrId);

        return buildEhrStatusMap(ehrId, format);
    }

    private Map<String, Object> buildEhrStatusMap(UUID ehrUuid, I_CompositionService.CompositionFormat format) throws Exception {
        GsonBuilder gsonBuilder = EncodeUtil.getGsonBuilderInstance();
        Gson gson = gsonBuilder.setPrettyPrinting().create();

        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);
        if (ehrAccess == null) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Invalid or null ehrId");
        }

        Map<String, Object> subjectIds = I_EhrAccess.fetchSubjectIdentifiers(getDataAccess(), ehrUuid);
        I_SystemAccess systemAccess = I_SystemAccess.retrieveInstance(getDataAccess(), ehrAccess.getSystemId());

        HashMap statusMap = new HashMap() {{
            put(SUBJECT_IDS, subjectIds);
            put(QUERYABLE, ehrAccess.isQueryable());
            put(MODIFIABLE, ehrAccess.isModifiable());
            if (ehrAccess.isSetOtherDetails()) {
                put(OTHER_DETAILS, "<![CDATA[" + ehrAccess.exportOtherDetailsXml() + "]]>");
                put(OTHER_DETAILS_TEMPLATE_ID, ehrAccess.getOtherDetailsTemplateId());
            } else if (ehrAccess.isSetOtherDetailsSerialized()) {
                if (format.equals(I_CompositionService.CompositionFormat.ECISFLAT)) {
                    Map<String, Object> retmap = gson.fromJson(ehrAccess.getOtherDetailsSerialized(), TreeMap.class);
                    Map<String, String> flatten = new EcisFlattener().generateEcisFlat(retmap);
                    put(OTHER_DETAILS, flatten);
                } else {
                    Map<String, Object> retmap = gson.fromJson(ehrAccess.getOtherDetailsSerialized(), TreeMap.class);
                    put(OTHER_DETAILS, retmap);
                }
            }
            put(SYSTEM_SETTINGS, systemAccess.getSettings());
            put(SYSTEM_DESCRIPTION, systemAccess.getDescription());
        }};

        Map<String, Object> retmap = new HashMap<>();

        retmap.put("action", "RETRIEVE");
        retmap.put("ehrStatus", statusMap);
        retmap.put(I_EhrService.EHRID_PARAMETER, ehrUuid.toString());
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID + "=" + ehrUuid);
        retmap.putAll(metaref);

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "get", path = "vehr/ehr/status", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/ehr/status", responseType = ResponseType.Json)
    })
    public Object retrieveStatus(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        UUID ehrUuid;
//        try {
        String ehrId = props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null);
        if (ehrId != null) {
            ehrUuid = UUID.fromString(ehrId);
        } else {
            //assume retrieve ehr with subjectId and subjectNameSpace
            return retrieve(props);
        }

        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, "XML"));


        String sessionId = auditSetter.getSessionId();

        if (ehrUuid == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid or null ehrId");

        setSessionEhr(sessionId, ehrUuid);

        return buildEhrStatusMap(ehrUuid, format);

    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/ehr", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/ehr", responseType = ResponseType.Json)
    })
    public Object create(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        String subjectIdCode = props.getClientProperty(I_EhrService.SUBJECTID_PARAMETER, (String) null);
        String subjectNameSpace = props.getClientProperty(I_EhrService.SUBJECTNAMESPACE_PARAMETER, (String) null);
        String systemSettings = props.getClientProperty(SYSTEM_SETTINGS, (String) null);

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        Object otherDetails = null;
        String otherDetailsTemplateId = null;

        if (content != null) {
            Gson json = new GsonBuilder().create();
            Map<String, Object> atributes = json.fromJson(content, Map.class);

            otherDetails = atributes.getOrDefault(OTHER_DETAILS, null);
            otherDetailsTemplateId = (String) atributes.getOrDefault(OTHER_DETAILS_TEMPLATE_ID, null);
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
                    subjectUuid = I_PartyIdentifiedAccess.getOrCreatePartyByExternalRef(getDataAccess(), null, subjectIdCode, CompositionAttributesHelper.DEMOGRAPHIC, subjectNameSpace, CompositionAttributesHelper.PARTY);
                    break;
            }
        } catch (Exception e) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Ehr cannot be created, there is no existing subject with this identifier:" + subjectIdCode + "::" + subjectNameSpace);
        }

        if (subjectUuid == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Subject is not existing for id code=" + subjectIdCode + " ,issuer=" + subjectNameSpace);

        UUID systemId = null;
        if (systemSettings != null) { //NB: a systemSettings == null is valid, it is defaulted to the local system
            try {
                systemId = I_SystemAccess.retrieveInstanceId(getDataAccess(), systemSettings);
            } catch (Exception e) {
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "System is not existing for settings=" + systemSettings);
            }
        }

        UUID ehrId = create(subjectUuid, systemId, otherDetails, otherDetailsTemplateId);

        Map<String, Object> retmap = new HashMap<>();
        retmap.put(I_EhrService.EHRID_PARAMETER, ehrId.toString());
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID + "=" + ehrId);
        retmap.putAll(metaref);
        retmap.put("action", "CREATE");

        setSessionEhr(sessionId, ehrId);

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "POST", method = "update", path = "vehr/ehr/status", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "PUT", method = "put", path = "rest/v1/ehr/status", responseType = ResponseType.Json)
    })
    public Object updateStatus(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        String sessionId = auditSetter.getSessionId();
        UUID ehrId = UUID.fromString(props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null));

        if (ehrId == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "No valid ehr Id parameter found in query");

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content cannot be empty for updating ehr status");

        //retrieve the ehr to update
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);

        if (ehrAccess == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Passed ehr Id does not match an existing EHR");

        //check for other_details
        if (props.getClientProperties().containsKey("other_details")) {
            I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, I_CompositionService.CompositionFormat.ECISFLAT.toString()));
            if (format.equals(I_CompositionService.CompositionFormat.XML)) {
                Gson json = new GsonBuilder().create();
                Map<String, Object> atributes = json.fromJson(content, Map.class);
                String otherDetailsXml = (String) atributes.getOrDefault(OTHER_DETAILS, null);
                otherDetailsXml = otherDetailsXml.replace("<![CDATA[", "");
                otherDetailsXml = otherDetailsXml.replace("]]>", "");
                String otherDetailsTemplateId = (String) atributes.getOrDefault(OTHER_DETAILS_TEMPLATE_ID, null);
                //do other_details stuff
                Locatable itemStructure = I_ContentBuilder.parseOtherDetailsXml(new ByteArrayInputStream(otherDetailsXml.getBytes()));
                ehrAccess.setOtherDetails(itemStructure, otherDetailsTemplateId);
                ehrAccess.update(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), null, I_ConceptAccess.ContributionChangeType.modification, auditSetter.getDescription(), true);
            } else if (format.equals(I_CompositionService.CompositionFormat.RAW)) {
                Gson json = new GsonBuilder().create();
                Map<String, Object> attributes = json.fromJson(content, Map.class);
                String otherDetailsRaw = (String) attributes.getOrDefault(OTHER_DETAILS, null);
                RawJsonParser rawJsonParser = new RawJsonParser();
                String serialized = rawJsonParser.dbEncode(new StringReader(otherDetailsRaw));
                String otherDetailsTemplateId = (String) attributes.getOrDefault(OTHER_DETAILS_TEMPLATE_ID, null);
                //do other_details stuff
                ehrAccess.setOtherDetails(serialized, otherDetailsTemplateId);
                ehrAccess.update(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), null, I_ConceptAccess.ContributionChangeType.modification, auditSetter.getDescription(), true);

            } else {
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "format for other_details is not supported:" + format);
            }
        } else {

            //get the map structure from the passed content string
            Gson json = new GsonBuilder().create();
            Map<String, Object> atributes = json.fromJson(content, Map.class);

            if (atributes.containsKey(MODIFIABLE)) {
                ehrAccess.setModifiable((Boolean) atributes.get(MODIFIABLE));
            }

            if (atributes.containsKey(QUERYABLE))
                ehrAccess.setQueryable((Boolean) atributes.get(QUERYABLE));

            if (atributes.containsKey(I_EhrService.SUBJECTID_PARAMETER) && atributes.containsKey(I_EhrService.SUBJECTNAMESPACE_PARAMETER)) {
                String subjectId = (String) atributes.get(I_EhrService.SUBJECTID_PARAMETER);
                String subjectNameSpace = (String) atributes.get(I_EhrService.SUBJECTNAMESPACE_PARAMETER);
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
        }

        Map<String, Object> retmap = new HashMap<>();
        retmap.put(I_EhrService.EHRID_PARAMETER, ehrId.toString());
        retmap.put("action", "UPDATE");
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID + "=" + ehrId);
        retmap.putAll(metaref);
        setSessionEhr(sessionId, ehrId);

        return retmap;

    }

    private void setSessionEhr(String sessionId, UUID ehrId) throws ServiceManagerException {
        I_SessionManager sessionManager = getRegisteredService(getGlobal(), "LogonService", "1.0", null);
        //retrieve the session manager
        if (sessionManager != null)
            sessionManager.getSessionUserMap(sessionId).put(I_CompositionService.EHR_ID, ehrId);
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "delete", path = "vehr/ehr", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "DELETE", method = "delete", path = "rest/v1/ehr", responseType = ResponseType.Json)
    })
    public Object delete(I_SessionClientProperties props) throws ServiceManagerException {
        queryProlog(props);
        String ehrId = props.getClientProperty(I_EhrService.EHRID_PARAMETER, (String) null);

        if (ehrId == null || ehrId.length() == 0)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "No valid ehr Id parameter found in query");

        UUID ehrUuid = UUID.fromString(ehrId);
        I_EhrAccess ehrAccess;

        try {
            ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);
        }
        catch (Exception e){
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Passed ehr Id does not match an existing EHR");
        }

        if (ehrAccess == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Passed ehr Id does not match an existing EHR");

        Integer result = 0;

        try {
            result = ehrAccess.delete(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());

        } catch (Exception e) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_UNAVAILABLE, ME, "Problem accessing DB" + e.getMessage());
        }

        if (result > 0) {
            Map<String, Object> retmap = new HashMap<>();
            retmap.put(I_EhrService.EHRID_PARAMETER, ehrId.toString());
            retmap.put("action", "DELETE");
            Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + I_CompositionService.EHR_ID + "=" + ehrId);
            retmap.putAll(metaref);
            return retmap;
        }
        else {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_NO_CONTENT);
            //build the relative part of the link to the existing last version
            Map<String, Object> retMap = new HashMap<>();
            retMap.put("Reason", "Delete ehrId failed");
            return retMap;
        }

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

    @Override
    public String getSubjectMode() {
        return subjectMode.toString();
    }

    @Override
    public void setSubjectMode(String mode) {
        subjectMode = EhrAccess.PARTY_MODE.valueOf(mode);
    }
}

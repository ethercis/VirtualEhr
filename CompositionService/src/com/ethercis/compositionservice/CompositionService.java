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
package com.ethercis.compositionservice;

import com.ethercis.compositionservice.handler.CanonicalHandler;
import com.ethercis.dao.access.handler.PvCompoHandler;
import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.json.FlatJsonUtil;
import com.ethercis.ehr.keyvalues.EcisFlattener;
import com.ethercis.ehr.knowledge.I_CacheKnowledgeService;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openehr.rm.composition.Composition;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Replies to queries with the following format:
 * GET ../composition?uid=... &format=&lt;FLAT|STRUCTURED|RAW|CANONICAL&gt;
 * DELETE ../composition?uid=...[&committerName=....][&committerId=...]
 * POST ../composition?[templateId=...][&ehrId=...][&format=...][&committerName=...][&committerId=...] (body contains the serialized composition)
 * PUT ../composition?uid=...[templateId=...][&ehrId=...][&format=...][&committerName=...][&committerId=...] (body contains the serialized composition)
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/3/2015.
 */

@Service(id ="CompositionService", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP") })

public class CompositionService extends ServiceDataCluster implements I_CompositionService, CompositionServiceMBean {

    final private String ME = "CompositionService";
    final private String Version = "1.0";
    private Logger log = Logger.getLogger(CompositionService.class);
    private I_CacheKnowledgeService knowledgeCache;

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)throws ServiceManagerException {
        super.doInit(global, serviceInfo);
        //get a resource service instance
        //get the knowledge cache for composition handlers
        knowledgeCache = getRegisteredService(getGlobal(), "CacheKnowledgeService", "1.0");

        if (knowledgeCache == null)
            throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, "Cache knowledge service [CacheKnowledgeService,1.0] is not running, aborting");

        putObject(I_Info.JMX_PREFIX+ME, this);

        log.info("Composition service started...");
    }


    private UUID getSessionEhrId(String sessionId) throws ServiceManagerException {
        I_SessionManager sessionManager = getRegisteredService(getGlobal(), "LogonService", "1.0");
        //retrieve the session manager
        return (UUID) sessionManager.getSessionUserMap(sessionId).get(EHR_ID);
    }

    private String getSessionSubjectName(String sessionId) throws ServiceManagerException {
        I_SessionManager sessionManager = getRegisteredService(getGlobal(), "LogonService", "1.0");
        //retrieve the session manager
        return sessionManager.getSubjectName(sessionId);
    }


    private UUID retrieveEhrId(String sessionId, I_SessionClientProperties props) throws ServiceManagerException {
        String uuidEncoded = props.getClientProperty(I_CompositionService.EHR_ID, (String)null);
        if (uuidEncoded == null && getSessionEhrId(sessionId)!= null)
            uuidEncoded = getSessionEhrId(sessionId).toString();
        else
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "No Ehr Id found in context or in query");

        UUID ehrId = UUID.fromString(uuidEncoded);

        return ehrId;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/composition", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/composition", responseType = ResponseType.Json)
    })
    public Object create(I_SessionClientProperties props) throws Exception {
        String sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String)null);
        String committerId = props.getClientProperty(I_CompositionService.COMMITTER_ID, "LOCAL");
        String committerName = props.getClientProperty(I_CompositionService.COMMITTER_NAME, getSessionSubjectName(sessionId));
        String templateId = props.getClientProperty(I_CompositionService.TEMPLATE_ID, (String)null);

//        UUID ehrId = UUID.fromString(props.getClientProperty(I_CompositionService.EHR_ID, getSessionEhrId(sessionId).toString()));
        UUID ehrId = retrieveEhrId(sessionId, props);

        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, "CANONICAL"));

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String)null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Content cannot be empty for a new composition");

        Integer contentLength = (Integer)props.getClientProperty(Constants.REQUEST_CONTENT_LENGTH, (Integer)0);

        if (content.length() != contentLength)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Content may be altered found length ="+content.length()+" expected:"+contentLength);

//        String contentType = props.getClientProperty(Constants.REQUEST_CONTENT_TYPE, "");

        UUID compositionId;

        switch (format){
            case CANONICAL:
                CanonicalHandler canonicalHandler = new CanonicalHandler(knowledgeCache.getKnowledgeCache(), templateId, null);
                Composition composition = canonicalHandler.build(getGlobal(), content);
                I_CompositionAccess compositionAccess = I_CompositionAccess.getNewInstance(getDataAccess(), composition, DateTime.now(), ehrId);
                I_EntryAccess entryAccess = I_EntryAccess.getNewInstance(getDataAccess(), templateId, 0, compositionAccess.getId(), composition);
                compositionAccess.addContent(entryAccess);
                compositionId = compositionAccess.commit();
                break;
            case ECISFLAT:
                PvCompoHandler pvCompoHandler = new PvCompoHandler(this.getDataAccess(), templateId, null);
                Map<String, String> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(content.getBytes())));
                compositionId = pvCompoHandler.storeComposition(ehrId, kvPairs);
                break;
            default:
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "This format is not supported:"+format);
        }

        Map<String, String> retmap = new HashMap<>();
        retmap.put("action", "CREATE");
        retmap.put(COMPOSITION_UID, compositionId.toString());

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/composition", responseType = ResponseType.String),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/composition", responseType = ResponseType.String)
    })
    public Object retrieve(I_SessionClientProperties props) throws Exception {
        String sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String)null);

        UUID ehrId = retrieveEhrId(sessionId, props);

        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, "CANONICAL"));
        Integer version = -1;
        UUID uid = null;

        String compositionId = props.getClientProperty(I_CompositionService.UID, (String)null);
        if (compositionId == null){
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Null or not supplied composition id");
        }
        try {
            if (compositionId.contains("::")) {
                version = Integer.parseInt(compositionId.split("::")[1]);
                uid = UUID.fromString(compositionId.split("::")[0]);
            } else
                uid = UUID.fromString(compositionId);
        }
        catch (Exception e){
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Invalid composition id:"+compositionId);
        }

        //retrieve the composition
        I_CompositionAccess compositionAccess = null;

        if (version > 0)
            compositionAccess = I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), uid, version);
        else
            compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), uid);

        StringBuffer stringBuffer = new StringBuffer();

        if (compositionAccess == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Request did not give any result");

        for (I_EntryAccess entryAccess: compositionAccess.getContent()) {
            switch (format) {
                case CANONICAL:
                    stringBuffer.append(new String(I_ContentBuilder.exportCanonicalXML(entryAccess.getComposition())));
                    break;
                case ECISFLAT:
                    Map<String, String> testRetMap = EcisFlattener.renderFlat(entryAccess.getComposition());
                    GsonBuilder builder = new GsonBuilder();
                    Gson gson = builder.setPrettyPrinting().create();
                    stringBuffer.append(gson.toJson(testRetMap));
                    break;
                default:
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Unsupported format:"+format);
            }
        }
        return stringBuffer.toString();
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "update", path = "vehr/composition", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "PUT", method = "put", path = "rest/v1/composition", responseType = ResponseType.Json)
    })
    public Object update(I_SessionClientProperties props) throws Exception {
        String sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String)null);
        String committerId = props.getClientProperty(I_CompositionService.COMMITTER_ID, "LOCAL");
        String committerName = props.getClientProperty(I_CompositionService.COMMITTER_NAME, getSessionSubjectName(sessionId));
        String templateId = props.getClientProperty(I_CompositionService.TEMPLATE_ID, (String)null);
        UUID compositionId = UUID.fromString(props.getClientProperty(I_CompositionService.UID, (String) null));
        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, "CANONICAL"));

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String)null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Content cannot be empty for updating a composition");

        Integer contentLength = (Integer)props.getClientProperty(Constants.REQUEST_CONTENT_LENGTH, (Integer)0);

        if (content.length() != contentLength)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Content may be altered found length ="+content.length()+" expected:"+contentLength);

//        String contentType = props.getClientProperty(Constants.REQUEST_CONTENT_TYPE, "");

        Boolean result;

        switch (format){
            case CANONICAL:
                CanonicalHandler canonicalHandler = new CanonicalHandler(knowledgeCache.getKnowledgeCache(), templateId, null);
                result = canonicalHandler.update(getGlobal(), getDataAccess(), compositionId, content);
                break;
            case ECISFLAT:
                I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
                //TODO: template id is not required
                PvCompoHandler pvCompoHandler = new PvCompoHandler(this.getDataAccess(), compositionAccess, "*", null); //template id is not required
                Map<String, String> kvPairs;
                try {
                    kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(content.getBytes())));
                }
                catch (Exception e){
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Error interpreting JSON in content:"+e);
                }
                result = pvCompoHandler.updateComposition(kvPairs);
                break;
            default:
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "This format is not supported:"+format);
        }

        //TODO: set committer if passed

        if (!result)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Update failed on composition:"+compositionId);

        Map<String, String> retmap = new HashMap<>();
        retmap.put("action", result ? "UPDATED" : "FAILED");
        retmap.put(COMPOSITION_UID, compositionId.toString());

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "delete", path = "vehr/composition", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "DELETE", method = "delete", path = "rest/v1/composition", responseType = ResponseType.Json)
    })
    public Object delete(I_SessionClientProperties props) throws Exception {
        UUID compositionId = UUID.fromString(props.getClientProperty(I_CompositionService.UID, (String) null));

        I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
        Integer result = compositionAccess.delete();

        if (result <= 0)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Delete failed on composition:"+compositionAccess.getId());

        Map<String, String> retmap = new HashMap<>();
        retmap.put("action", result > 0 ? "DELETED" : "FAILED");
        retmap.put(COMPOSITION_UID, compositionId.toString());

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "POST", method = "post", path = "vehr/composition/query", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/composition/query", responseType = ResponseType.Json)
    })
    public Object query(I_SessionClientProperties props) throws Exception {
        String sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String)null);

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String)null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Content cannot be empty for updating a composition");

        Integer contentLength = (Integer)props.getClientProperty(Constants.REQUEST_CONTENT_LENGTH, (Integer)0);

        if (content.length() != contentLength)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, "Content may be altered found length ="+content.length()+" expected:"+contentLength);

        //perform the query
        Map<String, Object> result = I_EntryAccess.queryJSON(getDataAccess(), content);

        return result;

    }

}

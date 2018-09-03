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

import com.ethercis.dao.access.handler.*;
import com.ethercis.dao.access.interfaces.I_CompoXrefAccess;
import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.dao.access.jooq.CompoXRefAccess;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.encode.CompositionSerializer;
import com.ethercis.ehr.encode.EncodeUtil;
import com.ethercis.ehr.encode.I_CompositionSerializer;
import com.ethercis.ehr.json.FlatJsonUtil;
import com.ethercis.ehr.keyvalues.EcisFlattener;
import com.ethercis.ehr.util.FlatJsonCompositionConverter;
import com.ethercis.ehr.util.I_FlatJsonCompositionConverter;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.persistence.DataAccessExceptionMessage;
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
import com.ethercis.systemservice.I_SystemService;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jooq.exception.DataAccessException;
import org.openehr.rm.composition.Composition;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Replies to queries with the following format:
 * GET ../composition?uid=... &format=&lt;FLAT|STRUCTURED|RAW|XML&gt;
 * DELETE ../composition?uid=...[&committerName=....][&committerId=...]
 * POST ../composition?[templateId=...][&ehrId=...][&format=...][&committerName=...][&committerId=...] (body contains the serialized composition)
 * PUT ../composition?uid=...[templateId=...][&ehrId=...][&format=...][&committerName=...][&committerId=...] (body contains the serialized composition)
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/3/2015.
 */

@Service(id = "CompositionService", version = "1.0", system = true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP")})

public class CompositionService extends ServiceDataCluster implements I_CompositionService, CompositionServiceMBean {

    final private String ME = "CompositionService";
    final private String Version = "1.0";
    private Logger log = LogManager.getLogger(CompositionService.class);
//    private I_CacheKnowledgeService knowledgeCache;
    private I_SystemService systemService;
    private boolean useNamespaceInCompositionId = false;
    private boolean supportCompositionXRef = false; //if set to false, will not try to link compositions

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(global, serviceInfo);
        //get a resource service instance
        //get the knowledge cache for composition handlers
        useNamespaceInCompositionId = global.getProperty().get("composition.uid.namespace", true);
        supportCompositionXRef = global.getProperty().get("composition.xref", false);
//        knowledgeCache = getRegisteredService(getGlobal(), "CacheKnowledgeService", "1.0");

        if (supportCompositionXRef)
            log.info("Composition Service XREF support enabled");

//        if (knowledgeCache == null)
//            throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Cache knowledge service [CacheKnowledgeService,1.0] is not running, aborting");

//        putObject(I_Info.JMX_PREFIX + ME, this);
        AnnotatedMBean.RegisterMBean(this.getClass().getCanonicalName(), CompositionServiceMBean.class, this);

        log.info("Composition service started...");
    }


    private UUID getSessionEhrId(String sessionId) throws ServiceManagerException {
        I_SessionManager sessionManager = getRegisteredService(getGlobal(), "LogonService", "1.0");
        //retrieve the session manager
        if (sessionManager != null)
            return (UUID) sessionManager.getSessionUserMap(sessionId).get(EHR_ID);
        else
            return null;
    }

    private UUID retrieveEhrId(String sessionId, I_SessionClientProperties props) throws ServiceManagerException {
        String uuidEncoded = props.getClientProperty(I_CompositionService.EHR_ID, (String) null);
        if (uuidEncoded == null) {
            if (getSessionEhrId(sessionId) != null)
                uuidEncoded = getSessionEhrId(sessionId).toString();
        }

        if (uuidEncoded == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "No Ehr Id found in context nor in query");

        UUID ehrId = UUID.fromString(uuidEncoded);

        return ehrId;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/composition", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/composition", responseType = ResponseType.Json)
    })
    public Object create(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        String sessionId = auditSetter.getSessionId();
        String templateId = props.getClientProperty(I_CompositionService.TEMPLATE_ID, (String) null);
        String linkUidStr = props.getClientProperty(I_CompositionService.LINK_ID, (String) null);
        UUID linkUid = (linkUidStr != null) ? UUID.fromString(linkUidStr) : null;
        UUID ehrId = retrieveEhrId(sessionId, props);

        UUID committerUuid = auditSetter.getCommitterUuid();
        UUID systemUuid = auditSetter.getSystemUuid();

        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, "XML"));

        if ((format == CompositionFormat.FLAT || format == CompositionFormat.ECISFLAT) && (templateId == null || templateId.length() == 0))
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Template Id must be specified");

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content cannot be empty for a new composition");

        Integer contentLength = (Integer) props.getClientProperty(Constants.REQUEST_CONTENT_LENGTH, (Integer) 0);

//        if (content.length() != contentLength)
//            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content may be altered found length ="+content.length()+" expected:"+contentLength);

//        String contentType = props.getClientProperty(Constants.REQUEST_CONTENT_TYPE, "");

        UUID compositionId;

        switch (format) {
            case XML:
                I_CanonicalHandler canonicalHandler = new CanonicalHandler(getDataAccess(), templateId);
                compositionId = canonicalHandler.storeComposition(ehrId, content, committerUuid, systemUuid, auditSetter.getDescription());

                linkComposition(linkUid, compositionId);
                //create an XML response
                Document document = DocumentHelper.createDocument();
                Element root = document.addElement("compositionCreateRestResponseData");
                root.addElement("action").addText("CREATE");
                root.addElement("compositionUid").addText(encodeUuid(compositionId, 1));
                root.addElement("meta").addElement("href").addText(Constants.URI_TAG + "?" + encodeURI(null, compositionId, 1, null));
                global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_XML);
                return document;


            case ECISFLAT:
                PvCompoHandler pvCompoHandler = new PvCompoHandler(this.getDataAccess(), templateId, null);
                Map<String, Object> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(content.getBytes())));
                compositionId = pvCompoHandler.storeComposition(ehrId, kvPairs, committerUuid, systemUuid, auditSetter.getDescription());
                linkComposition(linkUid, compositionId);

                //create json response
                global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
                Map<String, Object> retmap = new HashMap<>();
                retmap.put("action", "CREATE");
                retmap.put(COMPOSITION_UID, encodeUuid(compositionId, 1));
                Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, compositionId, 1, null));
                retmap.putAll(metaref);
                return retmap;

            case FLAT:
                I_FlatJsonHandler flatJsonHandler = new FlatJsonHandler(getDataAccess(), templateId);
                compositionId = flatJsonHandler.store(ehrId, content, committerUuid, systemUuid, auditSetter.getDescription());
                linkComposition(linkUid, compositionId);

                //create json response
                global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
                retmap = new HashMap<>();
                retmap.put("action", "CREATE");
                retmap.put(COMPOSITION_UID, encodeUuid(compositionId, 1));
                metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, compositionId, 1, null));
                retmap.putAll(metaref);
                return retmap;


            default:
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "This format is not supported:" + format);
        }
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/composition", responseType = ResponseType.String),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/composition", responseType = ResponseType.String)
    })
    public Object retrieve(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, CompositionFormat.ECISFLAT.toString()));
        Integer version = -1;
        UUID uid = null;

        String compositionId = props.getClientProperty(I_CompositionService.UID, (String) null);
        if (compositionId == null) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Null or not supplied composition id");
        }
        try {
            //TODO: decode with/without namespace depending on configuration
            if (compositionId.contains("::")) {
                version = getCompositionVersion(compositionId); //version number is inorder: 1, 2, 3 etc.
                uid = getCompositionUid(compositionId);
            } else
                uid = getCompositionUid(compositionId);
        } catch (Exception e) {
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Invalid composition id:" + compositionId);
        }

        Object retObj = null;

        //retrieve the composition
        I_CompositionAccess compositionAccess = null;

        if (version > 0)
            compositionAccess = I_CompositionAccess.retrieveCompositionVersion(getDataAccess(), uid, version);
        else {
            try {
                compositionAccess = I_CompositionAccess.retrieveInstance2(getDataAccess(), uid);
                if (compositionAccess == null && I_CompositionAccess.hasPreviousVersion(getDataAccess(), uid)) { //try to identify a previous version
                    //TODO: add life_cycle state to versions and return the first non deleted version id... right now it's always 1
                    global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_NO_CONTENT);
                    //build the relative part of the link to the existing last version
                    Map<String, Object> retMap = new HashMap<>();
                    retMap.put("Link", Constants.URI_TAG + "?" + encodeURI(null, uid, 2, format));
                    return retMap;
                }
            } catch (DataAccessException e) {
                if (e.getMessage().contains("permission denied"))
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED, ME, "Access denied");
                else {
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_UNAVAILABLE, ME, "Could not complete query:" + new DataAccessExceptionMessage(e).error());
                }
            }
        }

        if (compositionAccess == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Request did not give any result");

        for (I_EntryAccess entryAccess : compositionAccess.getContent()) {
            switch (format) {
                case XML:
                    global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_XML);
                    retObj = new String(I_ContentBuilder.exportCanonicalXML(entryAccess.getComposition()));
                    break;
                case ECISFLAT:
                    global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
                    Map<String, Object> retmap = new HashMap<>();
                    retmap.put("format", CompositionFormat.ECISFLAT.toString());
                    retmap.put("templateId", entryAccess.getTemplateId());
                    retmap.put("composition", new EcisFlattener().render(entryAccess.getComposition()));
                    Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, entryAccess.getComposition().getUid().toString(), null));
                    retmap.putAll(metaref);
                    retObj = retmap;
                    break;

                case FLAT:
                    I_FlatJsonCompositionConverter flatJsonCompositionConverter = FlatJsonCompositionConverter.getInstance(getDataAccess().getKnowledgeManager());
                    global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
                    retmap = new HashMap<>();
                    retmap.put("format", CompositionFormat.FLAT.toString());
                    retmap.put("templateId", entryAccess.getTemplateId());
                    retmap.put("composition", flatJsonCompositionConverter.fromComposition(entryAccess.getTemplateId(), entryAccess.getComposition()));
                    metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, entryAccess.getComposition().getUid().toString(), null));
                    retmap.putAll(metaref);
                    retObj = retmap;
                    break;

                case RAW:
                    global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
                    Composition composition = entryAccess.getComposition();
                    I_CompositionSerializer compositionSerializer = I_CompositionSerializer.getInstance(CompositionSerializer.WalkerOutputMode.RAW);
                    retmap = new HashMap<>();
                    retmap.put("format", CompositionFormat.RAW.toString());
                    retmap.put("templateId", entryAccess.getTemplateId());
                    Gson gson = EncodeUtil.getGsonBuilderInstance().setPrettyPrinting().create();
                    Map rawEncoded = gson.fromJson(compositionSerializer.dbEncode(composition), Map.class);
                    retmap.put("composition", rawEncoded);
                    metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, entryAccess.getComposition().getUid().toString(), null));
                    retmap.putAll(metaref);
                    retObj = retmap;
                    break;

                default:
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Unsupported format:" + format);
            }
        }
        return retObj;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "update", path = "vehr/composition", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "PUT", method = "put", path = "rest/v1/composition", responseType = ResponseType.Json)
    })
    public Object update(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        String sessionId = auditSetter.getSessionId();
        String templateId = props.getClientProperty(I_CompositionService.TEMPLATE_ID, (String) null);
        String uidStr = props.getClientProperty(I_CompositionService.UID, (String) null);
        if (uidStr == null || uidStr.length() == 0)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "A valid composition id must be supplied");
        UUID compositionId = getCompositionUid(uidStr);

        I_CompositionService.CompositionFormat format = I_CompositionService.CompositionFormat.valueOf(props.getClientProperty(I_CompositionService.FORMAT, CompositionFormat.ECISFLAT.toString()));

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content cannot be empty for updating a composition");

//        Integer contentLength = (Integer)props.getClientProperty(Constants.REQUEST_CONTENT_LENGTH, (Integer)0);

//        if (content.length() != contentLength)
//            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Content may be altered found length ="+content.length()+" expected:"+contentLength);

//        String contentType = props.getClientProperty(Constants.REQUEST_CONTENT_TYPE, "");

        Boolean result;

        switch (format) {
            case XML:
                CanonicalHandler canonicalHandler = new CanonicalHandler(getDataAccess(), templateId);
                result = canonicalHandler.update(getDataAccess(), compositionId, content, auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
                break;

            case ECISFLAT:
                I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
                if (compositionAccess == null)
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Could not find composition:" + compositionId);

                //TODO: template id is not required
                PvCompoHandler pvCompoHandler = new PvCompoHandler(this.getDataAccess(), compositionAccess, "*", null); //template id is not required
                Map<String, Object> kvPairs;
                try {
                    kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(content.getBytes())));
                } catch (Exception e) {
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Error interpreting JSON in content:" + e);
                }
                result = pvCompoHandler.updateComposition(kvPairs, auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
                break;

            case FLAT:
                compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
                if (compositionAccess == null)
                    throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Could not find composition:" + compositionId);

                //get the template id
                I_FlatJsonHandler flatJsonHandler = new FlatJsonHandler(getDataAccess(), compositionAccess, null, null);
                result = flatJsonHandler.update(getDataAccess(), compositionId, new String(content.getBytes()), auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());
                break;

            default:
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "This format is not supported:" + format);
        }

        //TODO: set committer if passed

        if (!result)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Update failed on composition:" + compositionId);

        Map<String, Object> retmap = new HashMap<>();
        retmap.put("action", result ? "UPDATED" : "FAILED");
        //get the composition uid with the right version number
        retmap.put(COMPOSITION_UID, encodeUuid(compositionId, I_CompositionAccess.getLastVersionNumber(getDataAccess(), compositionId)));
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, uidStr, null));
        retmap.putAll(metaref);

        return retmap;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "delete", path = "vehr/composition", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "DELETE", method = "delete", path = "rest/v1/composition", responseType = ResponseType.Json)
    })
    public Object delete(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        String uidStr = props.getClientProperty(I_CompositionService.UID, (String) null);
        if (uidStr == null || uidStr.length() == 0)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "A valid composition id must be supplied");

        UUID compositionId = getCompositionUid(uidStr);
        auditSetter.handleProperties(getDataAccess(), props);
        String sessionId = auditSetter.getSessionId();

        I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(getDataAccess(), compositionId);
        if (compositionAccess == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.RESOURCE_NOT_FOUND, ME, "Could not find composition:" + compositionId);

        Integer result = compositionAccess.delete(auditSetter.getCommitterUuid(), auditSetter.getSystemUuid(), auditSetter.getDescription());

        if (result <= 0)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Delete failed on composition:" + compositionAccess.getId());

        Map<String, Object> retmap = new HashMap<>();
        retmap.put("action", result > 0 ? "DELETED" : "FAILED");
        retmap.put(COMPOSITION_UID, encodeUuid(compositionId, 1));
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG + "?" + encodeURI(null, compositionId, 1, null));
        retmap.putAll(metaref);
        return retmap;
    }

    private String encodeUuid(UUID uuid, int version) {
        if (useNamespaceInCompositionId)
            return uuid + "::" + getDataAccess().getServerNodeId() + "::" + version;
        else
            return uuid + "::" + version;
    }

    private UUID getCompositionUid(String fullcompositionUid) {
        if (!fullcompositionUid.contains("::"))
            return UUID.fromString(fullcompositionUid);
        return UUID.fromString(fullcompositionUid.substring(0, fullcompositionUid.indexOf("::")));
    }

    private int getCompositionVersion(String fullcompositionUid) {
        if (!fullcompositionUid.contains("::"))
            return 1; //current version
        return Integer.valueOf(fullcompositionUid.substring(fullcompositionUid.lastIndexOf("::") + 2));
    }

    private String encodeURI(UUID ehrId, UUID compositionId, int version, I_CompositionService.CompositionFormat format) {
        StringBuffer encoded = new StringBuffer();

        if (compositionId != null)
            encoded.append(I_CompositionService.UID + "=" + encodeUuid(compositionId, version));
        if (ehrId != null)
            encoded.append("&" + I_CompositionService.EHR_ID + "=" + ehrId);
        if (format != null)
            encoded.append("&" + I_CompositionService.FORMAT + "=" + format);

        return encoded.toString();
    }

    private String encodeURI(UUID ehrId, String compositionId, I_CompositionService.CompositionFormat format) {
        StringBuffer encoded = new StringBuffer();

        if (compositionId != null)
            encoded.append(I_CompositionService.UID + "=" + compositionId);
        if (ehrId != null)
            encoded.append("&" + I_CompositionService.EHR_ID + "=" + ehrId);
        if (format != null)
            encoded.append("&" + I_CompositionService.FORMAT + "=" + format);

        return encoded.toString();
    }

    private void linkComposition(UUID master, UUID child) {
        if (!supportCompositionXRef)
            return;
        if (master == null || child == null)
            return;
        I_CompoXrefAccess compoXrefAccess = new CompoXRefAccess(getDataAccess());
        compoXrefAccess.setLink(master, child);
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
    public boolean getUseNamespaceInCompositionId() {
        return useNamespaceInCompositionId;
    }

    @Override
    public boolean getSupportCompositionXRef() {
        return supportCompositionXRef;
    }

    @Override
    public void setUseNamespaceInCompositionId(boolean val) {
        useNamespaceInCompositionId = val;
    }

    @Override
    public void setSupportCompositionXRef(boolean val) {
        supportCompositionXRef = val;
    }


}

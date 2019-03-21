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

package com.ethercis.query;

import com.ethercis.compositionservice.I_CompositionService;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.persistence.ServiceDataCluster;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.AnnotatedMBean;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.ethercis.systemservice.I_SystemService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replies to queries with the following format:
 * GET ../composition?uid=... &format=&lt;FLAT|STRUCTURED|RAW|XML&gt;
 * DELETE ../composition?uid=...[&committerName=....][&committerId=...]
 * POST ../composition?[templateId=...][&ehrId=...][&format=...][&committerName=...][&committerId=...] (body contains the serialized composition)
 * PUT ../composition?uid=...[templateId=...][&ehrId=...][&format=...][&committerName=...][&committerId=...] (body contains the serialized composition)
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/3/2015.
 */

@Service(id = "QueryService", version = "1.0", system = true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 4, action = "STOP")})

public class QueryService extends ServiceDataCluster implements QueryServiceMBean {

    public static final String AQL = "aql";
    public static final String SQL = "sql";
    final private String ME = "QueryService";
    final private String Version = "1.0";
    private Logger log = LogManager.getLogger(QueryService.class);
//    private I_CacheKnowledgeService knowledgeCache;
    private I_SystemService systemService;
    private boolean useNamespaceInCompositionId = false;
    private boolean supportCompositionXRef = false; //if set to false, will not try to link compositions
    private boolean allowSQL = false; //if set, sql queries are allowed on this service. Parameter: 'server.query.sql_enabled'

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        super.doInit(global, serviceInfo);
        //get a resource service instance
        //get the knowledge cache for composition handlers
//        useNamespaceInCompositionId = global.getProperty().get("composition.uid.namespace", true);
//        supportCompositionXRef = global.getProperty().get("composition.xref", false);
//        knowledgeCache = getRegisteredService(getGlobal(), "CacheKnowledgeService", "1.0");

//        if (supportCompositionXRef)
//            log.info("Composition Service XREF support enabled");

//        if (knowledgeCache == null)
//            throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Cache knowledge service [CacheKnowledgeService,1.0] is not running, aborting");

//        putObject(I_Info.JMX_PREFIX + ME, this);
        AnnotatedMBean.RegisterMBean(this.getClass().getCanonicalName(), QueryServiceMBean.class, this);
        allowSQL = Boolean.parseBoolean(get(Constants.SQL_ENABLED, "false"));

        log.info("QueryService service started...");
    }


    private UUID getSessionEhrId(String sessionId) throws ServiceManagerException {
        I_SessionManager sessionManager = getRegisteredService(getGlobal(), "LogonService", "1.0");
        //retrieve the session manager
        if (sessionManager != null)
            return (UUID) sessionManager.getSessionUserMap(sessionId).get(I_CompositionService.EHR_ID);
        else
            return null;
    }

    private UUID retrieveEhrId(String sessionId, I_SessionClientProperties props) throws ServiceManagerException {
        String uuidEncoded = props.getClientProperty(I_CompositionService.EHR_ID, (String) null);
        if (uuidEncoded == null) {
            if (getSessionEhrId(sessionId) != null)
                uuidEncoded = getSessionEhrId(sessionId).toString();
            else
                throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "No Ehr Id found in context nor in query");
        }

        UUID ehrId = UUID.fromString(uuidEncoded);

        return ehrId;
    }

    @Override
    public boolean getAllowSQL() {
        return allowSQL;
    }

    private enum QueryMode {SQL, AQL, UNDEF}

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "POST", method = "post", path = "vehr/query", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/query", responseType = ResponseType.Json)
    })
    public Object query(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        Boolean explain = props.getClientProperty(I_QueryService.EXPLAIN, false);
        String sessionId = auditSetter.getSessionId();
        QueryMode queryMode = QueryMode.UNDEF;
        UUID committerUuid = auditSetter.getCommitterUuid();
        UUID systemUuid = auditSetter.getSystemUuid();
//
//        String sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String)null);

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        if (content == null)
            throw new ServiceManagerException(getGlobal(), SysErrorCode.USER_ILLEGALARGUMENT, ME, "Query is not specified (HTTP content is empty)");

//        Map<String, String> kvPairs = FlatJsonUtil.inputStream2Map(new StringReader(new String(content.getBytes())));
        Map<String, String> kvPairs = extractQuery(new String(content.getBytes()));

        String queryString;

        if (kvPairs.containsKey(AQL)) {
            queryMode = QueryMode.AQL;
//            queryString = URLDecoder.decode(kvPairs.get(AQL), "UTF-8");
            queryString = kvPairs.get(AQL);
        } else if (kvPairs.containsKey(SQL)) {
            queryMode = QueryMode.SQL;
//            queryString = URLDecoder.decode(kvPairs.get(SQL), "UTF-8");
            queryString = kvPairs.get(SQL);
        } else {
            throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "No query parameter supplied");
        }

        //perform the query
        Map<String, Object> result;

        switch (queryMode) {
            case SQL:
                if (allowSQL)
                    result = I_EntryAccess.queryJSON(getDataAccess(), queryString);
                else
                    throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "SQL expression is not allowed on this server instance");
                break;
            case AQL:
                if (explain)
                    result = I_EntryAccess.explainAqlJson(getDataAccess(), queryString);
                else
                    result = I_EntryAccess.queryAqlJson(getDataAccess(), queryString);
                break;

            default:
                throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "Unknown query expression, should be 'sql=' or 'aql='");
        }

        int resultsetSize = 0;
        if (result.get("resultSet") != null)
            resultsetSize = ((List) result.get("resultSet")).size();

        if (resultsetSize == 0) {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_NO_CONTENT);
            //build the relative part of the link to the existing last version
            Map<String, Object> retMap = new HashMap<>();
            retMap.put("Reason", "Query resultset is empty");
            return retMap;
        }

        return result;

    }


    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "get", path = "vehr/query", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/query", responseType = ResponseType.Json)
    })
    public Object queryInline(I_SessionClientProperties props) throws Exception {
        queryProlog(props);
        QueryMode queryMode = QueryMode.UNDEF;

        String sessionId = props.getClientProperty(I_SessionManager.SECRET_SESSION_ID_INTERNAL, (String) null);

        String queryString = props.getClientProperty(I_CompositionService.SQL_QUERY, (String) null);

        if (queryString == null) {
            queryString = props.getClientProperty(I_CompositionService.AQL_QUERY, (String) null);
            if (queryString != null) {
                queryMode = QueryMode.AQL;
            } else
                throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "No query parameter supplied");
        } else
            queryMode = QueryMode.SQL;


        //perform the query
        Map<String, Object> result;

        switch (queryMode) {
            case SQL:
                if (allowSQL)
                    result = I_EntryAccess.queryJSON(getDataAccess(), queryString);
                else
                    throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "SQL expression is not allowed on this server instance");
                break;
            case AQL:
                result = I_EntryAccess.queryAqlJson(getDataAccess(), queryString);
                break;

            default:
                throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "Unknown query expression, should be 'sql=' or 'aql='");
        }

        if (result.size() == 0) {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_NO_CONTENT);
            //build the relative part of the link to the existing last version
            Map<String, Object> retMap = new HashMap<>();
            retMap.put("Reason", "Query resultset is empty");
            return retMap;
        }

        return result;

    }

    /**
     * utility to get a query not necessarily encoded from a pseudo json construct
     * {sql:"SQL expression with colon and quotes"}
     *
     * @param content
     * @return
     */
    public static Map<String, String> extractQuery(String content) {
        Pattern patternKey = Pattern.compile("(?<=\\\")(.*?)(?=\")");
//        Pattern patternExpression = Pattern.compile("(?<=\\:)(.*?)(?!.*\\})");
        Matcher matcherKey = patternKey.matcher(content);
//        Matcher matcherExpression = patternExpression.matcher(content);
        if (matcherKey.find()) {
            String type = matcherKey.group(1);
            String query = content.substring(content.indexOf(":") + 1, content.lastIndexOf("\""));
            query = query.substring(query.indexOf("\"") + 1);
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(type.toLowerCase(), query);
            return queryMap;
        } else
            throw new IllegalArgumentException("Could not identified query type (sql or aql) in content:" + content);

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

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
import com.ethercis.ehr.knowledge.I_CacheKnowledgeService;
import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.exception.DataAccessException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistence common service to all service performing queries to the DB
 * This service initialize the DB communication and other access layer disposition (caching, distribution etc.)
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 6/30/2015.
 */

@Service(id ="ResourceService", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 2, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 2, action = "STOP") })

public class ResourceService extends ClusterInfo implements ResourceServiceMBean, I_ResourceService {

    final private String ME = "ResourceService";
    final private String Version = "1.0";
    private RunTimeSingleton global;
    private Logger log = LogManager.getLogger(ResourceService.class);

    private I_DomainAccess domainAccess;

    private enum ConnectionMode{JDBC_DRIVER, DBCP2_POOL, PG_CONNECTION_POOL}

    private ConnectionMode connectionMode;

    @Override
    public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)throws ServiceManagerException {

        this.global = global;

        initCompatibilityMode();

        //get the configuration parameters from services.properties
        String implementation  = get("server.persistence.implementation", null);

        if (implementation == null)
            throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Could not get 'server.persistence.implementation'");

        //retrieve instance of running KnowledgeService
        I_CacheKnowledgeService knowledgeService = ClusterInfo.getRegisteredService(global, "CacheKnowledgeService", "1.0", new Object[] {null});

        if (knowledgeService == null)
            throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "KnowledgeService is not running, please check your configuration");

        Map<String, Object> properties = new HashMap<>();
        properties.put(I_DomainAccess.KEY_KNOWLEDGE, knowledgeService.getKnowledgeCache());

        switch (implementation){
            case "jooq":
                properties.put(I_DomainAccess.KEY_DIALECT, get("server.persistence.jooq.dialect", "POSTGRES"));
                properties.put(I_DomainAccess.KEY_URL, get("server.persistence.jooq.url", null));
                properties.put(I_DomainAccess.KEY_LOGIN, get("server.persistence.jooq.login", "postgres"));
                properties.put(I_DomainAccess.KEY_PASSWORD, get("server.persistence.jooq.password", "postgres"));

                try {
                    domainAccess = I_DomainAccess.getInstance(properties);
                } catch (Exception e) {
                    throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Unable to setup DB layer access" + e);
                }
                connectionMode = ConnectionMode.JDBC_DRIVER;
                log.info("DB access set to JDBC DRIVER");
                break;
            case "jooq_pg_pool":
                properties.put(I_DomainAccess.KEY_CONNECTION_MODE, I_DomainAccess.PG_POOL);
                properties.put(I_DomainAccess.KEY_DIALECT, get("server.persistence.jooq.dialect", "POSTGRES"));
                properties.put(I_DomainAccess.KEY_DATABASE, get("server.persistence.jooq.database", "ethercis"));
//                properties.put(I_DomainAccess.KEY_SCHEMA, get("server.persistence.jooq.dialect", "ehr"));
                properties.put(I_DomainAccess.KEY_HOST, get("server.persistence.jooq.host", null));
                properties.put(I_DomainAccess.KEY_PORT, get("server.persistence.jooq.port", null));
                properties.put(I_DomainAccess.KEY_LOGIN, get("server.persistence.jooq.login", "postgres"));
                properties.put(I_DomainAccess.KEY_PASSWORD, get("server.persistence.jooq.password", "postgres"));
                properties.put(I_DomainAccess.KEY_MAX_CONNECTION, get("server.persistence.jooq.max_connections", "10"));

                try {
                    domainAccess = I_DomainAccess.getInstance(properties);
                } catch (Exception e) {
                    throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Unable to setup DB layer access" + e);
                }
                connectionMode = ConnectionMode.PG_CONNECTION_POOL;
                log.info("DB access set to PG CONNECTION POOLING");
                break;
            case "jooq_dbcp2":
                properties.put(I_DomainAccess.KEY_CONNECTION_MODE, I_DomainAccess.DBCP2_POOL);
                properties.put(I_DomainAccess.KEY_DIALECT, get("server.persistence.jooq.dialect", "POSTGRES"));
                properties.put(I_DomainAccess.KEY_URL, get("server.persistence.jooq.url", null));
                properties.put(I_DomainAccess.KEY_LOGIN, get("server.persistence.jooq.login", "postgres"));
                properties.put(I_DomainAccess.KEY_PASSWORD, get("server.persistence.jooq.password", "postgres"));

                properties.put(I_DomainAccess.KEY_MAX_IDLE, get("server.persistence.dbcp2.max_idle", null));
                properties.put(I_DomainAccess.KEY_MAX_ACTIVE, get("server.persistence.dbcp2.max_active", null));
                properties.put(I_DomainAccess.KEY_TEST_ON_BORROW, get("server.persistence.dbcp2.test_on_borrow", "false"));
                properties.put(I_DomainAccess.KEY_AUTO_RECONNECT, get("server.persistence.dbcp2.auto_reconnect", null));
                properties.put(I_DomainAccess.KEY_WAIT_MS, get("server.persistence.dbcp2.max_wait", null));
                properties.put(I_DomainAccess.KEY_SET_POOL_PREPARED_STATEMENTS, get("server.persistence.dbcp2.set_pool_prepared_statements", null));
                properties.put(I_DomainAccess.KEY_SET_MAX_PREPARED_STATEMENTS, get("server.persistence.dbcp2.set_max_prepared_statements", null));

                try {
                    domainAccess = I_DomainAccess.getInstance(properties);
                } catch (Exception e) {
                    throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Unable to setup DB layer access" + e);
                }
                connectionMode = ConnectionMode.DBCP2_POOL;
                log.info("DB access set to DBCP2 POOLING");
                break;
            default:
                throw new ServiceManagerException(global, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Unknown SQL resource dialect:"+implementation);

        }

        putObject(I_Info.JMX_PREFIX+ME, this);

        log.info("ResourceService started...");
    }

    @Override
    public I_DomainAccess getDomainAccess(){
        return domainAccess;
    }

    @Override
    public String settings() throws SQLException {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append("DB connection settings:\n");
        stringBuffer.append("=======================\n");
        switch (connectionMode) {
                case JDBC_DRIVER:
                    stringBuffer.append("\nJDBC_DRIVER");
                    stringBuffer.append("\nSQL dialect:" + domainAccess.getDialect());
                    stringBuffer.append("\nDB server node:" + domainAccess.getConnection().getMetaData().getURL().toString());
                    stringBuffer.append("\nDB engine name:" + domainAccess.getConnection().getMetaData().getDatabaseProductName());
                    stringBuffer.append("\nDB engine version:" + domainAccess.getConnection().getMetaData().getDatabaseProductVersion());
                    stringBuffer.append("\nDB driver name:" + domainAccess.getConnection().getMetaData().getDriverName());
                    stringBuffer.append("\nDB driver version:" + domainAccess.getConnection().getMetaData().getDriverVersion());
                break;
            case PG_CONNECTION_POOL:
                stringBuffer.append("\nPG_CONNECTION_POOL");
                stringBuffer.append("\nSQL dialect:" + domainAccess.getDialect());
            case DBCP2_POOL:
                stringBuffer.append("\nPG_DBCP2");
                stringBuffer.append("\nSQL dialect:" + domainAccess.getDialect());
            break;

        }
        return stringBuffer.toString();
    }

    @Override
    public String checkDBConnection(){
        try {
            int i = domainAccess.getContext().execute("SELECT 1;");
            return "Connection is alive";
        }
        catch (DataAccessException e){
            return "Fail to check connection:"+e.getMessage();
        }
    }

    @Override
    public String restartDBConnection(){
        return "Not implemented yet, restart ethercis to reconnect do DB server";
    }

}

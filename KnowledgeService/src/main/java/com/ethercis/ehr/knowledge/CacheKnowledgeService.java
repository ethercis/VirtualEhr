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
package com.ethercis.ehr.knowledge;

import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;

/**
 * Cache Knowledge Service class
 * <p>
 *     The Cache service scans ADL, OET and OPT resources in specified directories. It either reference
 *     or cache the parse resource according to its configuration. The service parameters are:<br>
 *         <ul>
 *             <li>knowledge.path.archetype : path where archetypes (ADL) are to be search</li>
 *              <li>knowledge.path.template : path for templates (OET)</li>
 *              <li>knowledge.path.opt : path for operational templates (OPT)</li>
 *              <li>knowledge.forcecache : "true|false" specifies if force caching is enabled</li>
 *         </ul>
 *         <br>
 *         The path search is recursive.
 *         forcecache tells the service to parse resources at load time.
 *         The service can be administered with JMX, the following commands are implemented:<br>
 *             <ul>
 *               <li>reload: reload the current caches </li>
 *               <li>statistics: get current statistics in cache</li>
 *               <li>showArchetypes: get the list of current archetypes</li>
 *               <li>showTemplates: get the list of templates</li>
 *               <li>showOPT: get the list of operational templates</li>
 *               <li>setForceCache (true|false): enable/disable force caching on reload</li>
 *               <li>settings: show the current service settings</li>
 *               <li>errors: display the faulty openEhr objects detected at load time</li>
 *             </ul>
 * </p>
 */
@Service(id ="CacheKnowledgeService", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 9, sequence = 1, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 1, action = "STOP") })

public class CacheKnowledgeService extends ClusterInfo implements I_CacheKnowledgeService, CacheKnowledgeServiceMBean {
	
	private String ME="CacheKnowledgeService";
	private String version="1.0";
	private Logger log = Logger.getLogger(ME);
	private RunTimeSingleton global;
	private I_KnowledgeCache cache;
    private ServiceInfo serviceInfo = null; //used for JMX reload()

	public CacheKnowledgeService() {

	}	
	
	/* (non-Javadoc)
	 * @see com.ethercis.ehr.cache.I_ResourceService#getKnowledgeCache()
	 */
	@Override
	public I_KnowledgeCache getKnowledgeCache(){
		return cache;
	}

	@Override
	protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)
			throws ServiceManagerException {
        log.info("Starting "+ME+" version="+version);
		this.global = global;
        this.serviceInfo = serviceInfo;
		
		//initialize the ArchetypeRepository controller
        try {
            this.cache = new KnowledgeCache(global.getProperty().getProperties(), serviceInfo.getParameters());
        } catch (Exception e){
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, "Severe error while loading cache:"+e);
        }

        putObject(I_Info.JMX_PREFIX+ME, this);

        log.info(ME + " successfully started");
        log.info("Statistics:\n"+statistics());
	}

    @Override
    public String usage() {
        StringBuffer sb = new StringBuffer();

        sb.append(ME+" version:"+version+"\n");
        sb.append("Available commands:\n");
        sb.append("reload: reload the current caches\n");
        sb.append("statistics: get current statistics in cache\n");
        sb.append("showArchetypes: get the list of current archetypes\n");
        sb.append("showTemplates: get the list of templates=\n");
        sb.append("showOPT: get the list of operational templates\n");
        sb.append("setForceCache (true|false): enable/disable force caching on reload\n");
        sb.append("settings: show the current service settings\n");
        sb.append("errors: display the faulty openEhr objects detected at load time\n");
        return sb.toString();
    }

    @Override
    public String reload() {
        try {
            if (serviceInfo != null && serviceInfo.getParameters().size() > 0)
                this.cache = new KnowledgeCache(global.getProperty().getProperties(), serviceInfo.getParameters());
            else
                this.cache = new KnowledgeCache(global.getProperty().getProperties(), null); //assume from environment
        } catch (Exception e){
            return "Could not reload cache with exception:"+e;
        }
        return "Reload successfully done\n";
    }

    @Override
    public String statistics(){
        return cache.statistics();
    }

    @Override
    public String showArcheypes(){
        return cache.archeypesList();
    }

    @Override
    public String showTemplates(){
        return cache.oetList();
    }

    @Override
    public String showOPT(){
        return cache.optList();
    }

    @Override
    public String setForceCache(boolean set){
        cache.setForceCache(set);
        return "Force Cache is now "+(set ? "enabled\n" : "disabled\n");
    }

    @Override
    public String settings(){
        return cache.settings();
    }

    @Override
    public String errors() { return cache.processingErrors(); }
}

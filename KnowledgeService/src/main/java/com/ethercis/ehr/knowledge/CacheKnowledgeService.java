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

import com.ethercis.ehr.json.FlatJsonUtil;
import com.ethercis.ehr.json.JsonUtil;
import com.ethercis.ehr.util.FlatJsonCompositionConverter;
import com.ethercis.ehr.util.I_FlatJsonCompositionConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openehr.rm.common.generic.PartyIdentified;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.building.util.CompositionAttributesHelper;
import com.ethercis.ehr.building.util.ContextHelper;
import com.ethercis.ehr.encode.CompositionSerializer;
import com.ethercis.ehr.keyvalues.EcisFlattener;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.composition.Composition;
import org.openehr.rm.composition.EventContext;
import org.openehr.rm.support.identification.ObjectVersionID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.ethercis.ehr.building.util.CompositionAttributesHelper.createComposer;

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
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Severe error while loading cache:"+e);
        }

        putObject(I_Info.JMX_PREFIX+ME, this);

        log.info(ME + " successfully started");
        log.info("Statistics:\n"+statistics());
	}

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "get", path = "vehr/template", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/template", responseType = ResponseType.Json)
    })
    public Object retrieve(I_SessionClientProperties props) throws ServiceManagerException {

        try {
            Map retmap = this.getKnowledgeCache().listOperationalTemplates();
            if (retmap.size() == 0){
                global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, ""+MethodName.RETURN_NO_CONTENT);
                //build the relative part of the link to the existing last version
                Map<String, Object> retMap = new HashMap<>();
                retMap.put("Reason", "No templates");
                return retMap;
            }
            Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG);
            retmap.putAll(metaref);
            return retmap;
        } catch (IOException e) {
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not generate templates list, reason:" + e);
        }
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "get", path = "vehr/template/example", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "get", path = "rest/v1/template/example", responseType = ResponseType.Json)
    })
    public Object example(I_SessionClientProperties props) throws ServiceManagerException {

        String templateId = props.getClientProperty(I_CacheKnowledgeService.TEMPLATE_ID, (String)null);
        String format = props.getClientProperty(I_CacheKnowledgeService.FORMAT, "XML");

        if (templateId == null)
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "No template Id give (templateId missing)");


        Object retObj = null;

        try {
            I_ContentBuilder contentBuilder = I_ContentBuilder.getInstance(null, I_ContentBuilder.OPT, this.getKnowledgeCache(), templateId);

            Object generated = contentBuilder.generate();

            if (generated == null)
                throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not generate an example for template Id:"+templateId);


            if (generated instanceof Composition) {
                EventContext context = ContextHelper.createDummyContext();
                PartyIdentified partyIdentified = CompositionAttributesHelper.createComposer("Composer", "ETHERCIS", "1234-5678");

                ((Composition) generated).setContext(context);
                ((Composition) generated).setComposer(partyIdentified);
                ((Composition) generated).setUid(new ObjectVersionID(UUID.randomUUID()+"::example.ethercis.com::1"));

                switch (format) {
                    case "XML":
                        global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, ""+MethodName.RETURN_XML);
                        byte[] exportXml = contentBuilder.exportCanonicalXML((Composition) generated, true, true);
                        if (exportXml == null)
                            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not export an example for template Id:" + templateId);
                        retObj = new String (exportXml);
                        break;
                    case "ECISFLAT":
                        global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, ""+MethodName.RETURN_STRING);
                        Map<String, String> testRetMap = EcisFlattener.renderFlat((Composition) generated, true, CompositionSerializer.WalkerOutputMode.PATH);
                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.setPrettyPrinting().disableHtmlEscaping().create();
                        String jsonString = gson.toJson(testRetMap);
                        retObj = jsonString;
                        break;
                    case "FLAT":
                        global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, ""+MethodName.RETURN_STRING);
                        I_FlatJsonCompositionConverter flatJsonCompositionConverter = FlatJsonCompositionConverter.getInstance(cache);
                        Map<String, Object> retMap = flatJsonCompositionConverter.fromComposition(templateId, (Composition)generated);
                        jsonString = JsonUtil.toJsonString(retMap);
                        retObj = jsonString;
                        break;
                }
            } else if (generated instanceof Locatable) {
                switch (format) {
                    case "XML":
                        global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_XML);
                        byte[] exportXml = contentBuilder.exportCanonicalXML((Locatable) generated, true, true);
                        if (exportXml == null)
                            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not export an example for template Id:" + templateId);
                        retObj = new String(exportXml);
                        break;
                    case "ECISFLAT":
                        global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_JSON);
                        Map<String, String> testRetMap = EcisFlattener.renderFlat((Locatable) generated, true, CompositionSerializer.WalkerOutputMode.PATH);

                        GsonBuilder builder = new GsonBuilder();
                        Gson gson = builder.setPrettyPrinting().disableHtmlEscaping().create();

                        String jsonString = gson.toJson(testRetMap);

                        retObj = jsonString;
                        break;

                }
            }
        } catch (Exception e) {
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not generate templates list, reason:" + e);
        }
        return retObj;
    }

    @QuerySetting(dialect = {
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "create", path = "vehr/template", responseType = ResponseType.Json),
            @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "POST", method = "post", path = "rest/v1/template", responseType = ResponseType.Json)
    })
    public Object create(I_SessionClientProperties props) throws Exception {

        //get body stuff
        String content = props.getClientProperty(Constants.REQUEST_CONTENT, (String) null);

        String templateId;
        try {
            templateId = this.getKnowledgeCache().addOperationalTemplate(content.getBytes());
        } catch (Exception e){
            throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, ME, "Could not add template, reason:" + e);
        }

        Map<String, Object> retmap = new HashMap<>();
        retmap.put("action", "CREATE");
        retmap.put("templateId", templateId);
        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", Constants.URI_TAG);
        retmap.putAll(metaref);
        return retmap;
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

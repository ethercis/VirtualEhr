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
package com.ethercis.authenticate.service;

import com.ethercis.authenticate.interfaces.I_LookupManager;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.interfaces.data.I_SubjectService;
import com.ethercis.servicemanager.common.interfaces.data.I_User;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Service(id = "AuthenticateService" ,version="1.0",system=true)
@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 7, sequence = 4, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 8, sequence = 5, action = "STOP") })

public class AuthenticateService extends ClusterInfo implements AuthenticateServiceMBean, I_SubjectService {
	private static Logger log = Logger.getLogger(AuthenticateService.class);
	private RunTimeSingleton global;
	final private String ME = "AuthenticateService";
	final private String Version = "1.0";
	private I_LookupManager lookupManager;
	
	@Override
	protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)
			throws ServiceManagerException {
		this.global=global;
		putObject(I_Info.JMX_PREFIX + "AuthenticateService", this);
		String policyType = global.getProperty().get(Constants.POLICY_TYPE_TAG,
				Constants.STR_POLICY_DEBUG);
		try{
			lookupManager=LookupManagerFactory.getInstance(global, policyType);
		}catch(Exception e){
			log.error("LookupManagerFactory.newWrapper",e);
		}
	}
	
	public String getME() {
		return ME;
	}

	public String getVersion() {
		return Version;
	}

	/**
	 * 
	 * @param props
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
    @QuerySetting( dialect = {
	    @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.STANDARD, httpMethod = "GET", method = "query", path = "vehr/subjects", responseType = ResponseType.Xml),
        @QuerySyntax(mode = I_ServiceRunMode.DialectSpace.EHRSCAPE, httpMethod = "GET", method = "query", path = "vehr/subjects", responseType = ResponseType.Xml)
    })
	public String findSubjectsByPrincipal(I_SessionClientProperties props)
			throws ServiceManagerException {
		String principal=props.getClientProperty("principal", "");
		if(principal==null || principal.isEmpty()){
			log.warn("principal is empty!");
			throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, ME, "No principal supplied");
		}
		List<I_Authenticate> subjects=new ArrayList<I_Authenticate>();
		
		List<I_Authenticate> subs = lookupManager.findSubjectsByPrincipal(principal);

		return "";

	}
	
	@Override
	public I_User getUser(String id) {
		// TODO Auto-generated method stub
		return null;
	}
}

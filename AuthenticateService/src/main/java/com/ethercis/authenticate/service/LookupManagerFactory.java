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

import com.ethercis.authenticate.dummy.DummyLookupManager;


import com.ethercis.authenticate.interfaces.I_LookupManager;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LookupManagerFactory {
	private static Logger log = LogManager.getLogger(LookupManagerFactory.class);
	final private static String ME = "LookupManagerFactory";
	
	
	public static I_LookupManager getInstance(RunTimeSingleton global, String policyType) throws ServiceManagerException {
		I_LookupManager lookupManager = null;
		//log.finest("policyType=" +policyType);
		if (policyType.compareToIgnoreCase(Constants.STR_POLICY_XML) == 0){
			throw  new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT,
					"Lookup for policy "+policyType + " is not yet implemented");
		}else if(policyType.compareToIgnoreCase(Constants.STR_POLICY_DEBUG) == 0){
			lookupManager = new DummyLookupManager(global);
		}
        else if (policyType.compareToIgnoreCase(Constants.STR_POLICY_SHIRO) == 0){
            lookupManager = new DummyLookupManager(global);
        }
		else {
			log.error("the policy is not defined, make sure variables 'server.security.policy' is set :" +policyType);
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION, ME, "the policy is not defined, make sure variable 'server.security.policy' is set");
		}
		return lookupManager;
	}
}

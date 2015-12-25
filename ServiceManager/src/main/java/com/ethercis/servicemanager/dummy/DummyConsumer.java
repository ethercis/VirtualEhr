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
package com.ethercis.servicemanager.dummy;

import org.apache.log4j.Logger;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;

/**
 * simple example to perform an operation on another runtime service by invoking a public method
 * @author christian
 *
 */

public class DummyConsumer extends ClusterInfo implements DummyConsumerMXBean {
	private RunTimeSingleton glob;
	private String ME = "DummyConsumer";
	private Logger log = Logger.getLogger(ME);
	I_DummyProvider provider;
	
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	public String getType() {
		return ME;
	}

	public String getVersion() {
		return "1.0";
	}

	@Override
	public String consume(String arg) {
		if (provider == null)
			return "Sorry provider could not be found...\n";
		
		String response = provider.responder(arg);
		return response;
	}

	@Override
	protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo)
			throws ServiceManagerException {
		this.glob = global;
        //add JMX details...
        putObject(I_Info.JMX_PREFIX+"DummyConsumer", this);
        
        //locate the provider service
        provider = (I_DummyProvider)this.getRuntimeService(glob,"DummyProvider","1.0");
        
        if (provider == null){
        	log.error("Could not locate provider, make sure service is loaded with priority higher than consumer");
        }
	}
}

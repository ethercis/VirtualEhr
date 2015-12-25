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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;

public class DummyProvider extends ClusterInfo implements I_DummyProvider, DummyProviderMBean {

	private RunTimeSingleton glob;
	private String ME = "DummyProvider";
	
	public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
		this.glob = global;
        //add JMX details...
        putObject(I_Info.JMX_PREFIX+"DummyProvider", this);
	}

	@Override
	public String usage() {
		return "I can be called by responder(arg)\n";
	}

	@Override
	public String responder(String arg) {
		String response = "Hi this is service:"+ME+" you have requested:"+arg+"\n";
		return response;
	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	public String getType() {
		return ME;
	}

	public String getVersion() {
		return "1.0";
	}
}

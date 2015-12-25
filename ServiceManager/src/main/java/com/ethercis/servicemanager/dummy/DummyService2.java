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

/**
 * This class is JMX instrumentalized<p>
 * @author Christian Chevalley
 *
 */
public class DummyService2 extends ClusterInfo implements DummyService2MBean{
	public static final String __REVISION_ID="$Revision$ $Date$";
	//private static Logger log = Logger.getLogger(Constants.LOGGER_SYSTEM);

	//private ContextNode contextNode;
	private RunTimeSingleton glob;
//	private int errors = 0;
	private String lasterror;	
	
	private String ME="DUMMY2";
	
	private boolean initiated = false;

	public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
		this.glob = global;
        this.initiated = true;
        //add JMX details...
        putObject(I_Info.JMX_PREFIX+"DummyService2", this);
	}
	
	public boolean initiate() {
		return initiated;
	}

	/**
	 * JMX - manual publish
	 */
	public String publish(String URL, String action, String message, int priority){
		return "Done\n";
	}

	public String usage(){
		StringBuffer sb = new StringBuffer();
		
		sb.append("publish(String URL, String action, String message, int priority) - publish an arbitrary message\n");
		sb.append("where:\n");
		sb.append("action: ADD or CANCEL\n");
		sb.append("message: text to display\n");
		sb.append("priority: 0..9\n");
		
		return sb.toString();			
	}	
	
	
	public String status(){
		StringBuffer sb = new StringBuffer();

		sb.append(ME+" version:"+__REVISION_ID+"\n");
		sb.append("Last error:"+((lasterror==null)?"*none*":lasterror)+"\n");

		return sb.toString();		
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

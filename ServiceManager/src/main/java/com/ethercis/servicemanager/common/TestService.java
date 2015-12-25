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
package com.ethercis.servicemanager.common;

import java.util.Properties;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.I_Service;
import com.ethercis.servicemanager.service.ServiceInfo;

public class TestService {

	public static void setnstart(I_Service service, 
								 RunTimeSingleton controller,
								 String type, String version, 
								 String[][] parms) throws ServiceManagerException {
			ServiceInfo pi = new ServiceInfo(controller,null,type,version);
			Properties props = pi.getParameters();
			for (String[] kv: parms){
				props.put(kv[0], kv[1]);
			}
		
			service.init(controller, pi);
			//register this service
			controller.getServiceRegistry().register(type+","+version, service);
	}
}

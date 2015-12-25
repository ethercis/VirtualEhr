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
package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.annotation.Attribute;
import com.ethercis.servicemanager.annotation.Attributes;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.RunLevelAction;
import com.ethercis.servicemanager.runlevel.ServiceConfig;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ServiceHolderAnnotationFactory implements ServiceHolderFactory {
	private String ME = "ServiceHolderAnnotationFactory";
	private final RunTimeSingleton glob;
	private static Logger log = Logger.getLogger(ServiceHolderAnnotationFactory.class
			.getName());
	private ServiceHolder serviceHolder;
	private String currentNode;
	private String serviceClasses = "com.ethercis";

	public ServiceHolderAnnotationFactory(RunTimeSingleton glob) {
		this.glob = glob;
		this.serviceHolder = new ServiceHolder(glob);
	}
	
	protected void loadAttributes(ServiceConfig config,Class<Service> clazz) {

		Attributes aas = clazz
				.getAnnotation(Attributes.class);

		if (aas != null) {
			Attribute[] aaa=aas.value();
			for(Attribute aa:aaa){
				config.addAttribute(aa.id(), aa.value());
			}
		}
	}

	protected List<RunLevelAction> loadServiceActions(Class<Service> clazz) {
		List<RunLevelAction> actions = new ArrayList<RunLevelAction>();
		RunLevelActions aas = clazz
				.getAnnotation(RunLevelActions.class);
		log.info("loadServiceActions " +aas);
		if (aas != null) {
			com.ethercis.servicemanager.annotation.RunLevelAction[] aaa=aas.value();
			for(com.ethercis.servicemanager.annotation.RunLevelAction aa:aaa){
				RunLevelAction action = new RunLevelAction(glob);
				action.setDo(aa.action());
				action.setOnFail(SysErrorCode.toErrorCode(aa.onFail()));
				action.setOnShutdownRunlevel(aa.onShutdownRunlevel());
				action.setOnStartupRunlevel(aa.onStartupRunlevel());
				action.setSequence(aa.sequence());
				actions.add(action);
			}
		}
		return actions;
	}

	protected List<ServiceConfig> loadServiceConfig() {
		List<ServiceConfig> configs = new ArrayList<ServiceConfig>();
		ServiceClassScanner scanner = new ServiceClassScanner();
		List<Class<Service>> classes = scanner
				.getServiceClasses(serviceClasses);
		
		
		Collections.sort(classes,new Comparator<Class<Service>>(){
			@Override
			public int compare(Class<Service> o1, Class<Service> o2) {
				Service service1 = o1.getAnnotation(Service.class);
				Service service2 = o2.getAnnotation(Service.class);
				if(service1.system() && service2.system()){
					return service1.order()-service2.order();
				}else if(service1.system()){
					return 1;
				}else if(service2.system()){
					return -1;
				}else{
					return service1.order()-service2.order();
				}
			}
			
		});
		
		for (Class<Service> clazz : classes) {
			log.info("Found Service " +clazz);
			Service service = clazz.getAnnotation(Service.class);
			if (service != null) {
				ServiceConfig config = new ServiceConfig(glob);
				config.setId(service.id());
				config.setCreateInternal(service.create());
				config.setClassName(clazz.getName());
				config.setJar(service.jarPath());
				List<RunLevelAction> actions = loadServiceActions(clazz);
				for (RunLevelAction action : actions) {
					config.addAction(action);
				}
				loadAttributes(config,clazz);
				configs.add(config);
			}
		}

		return configs;
	}

	@Override
	public ServiceHolder loadServiceHolder() throws ServiceManagerException {
		// loop load service config and add to ServiceHolder
		List<ServiceConfig> configs = loadServiceConfig();
		for (ServiceConfig config : configs) {
			// TODO implement node support
			if (currentNode != null) {
				serviceHolder.addServiceConfig(currentNode, config);
			} else {
				serviceHolder.addDefaultServiceConfig(config);
				log.info("Service " + config.getClassName());
			}

		}

		ServiceConfig[] arr = serviceHolder.getAllServiceConfig(this.glob
				.getNodeId().getId());
		for (int i = 0; i < arr.length; i++){
			//log.info("register service " + arr[i]);
			arr[i].registerMBean();
		}
		//log.dummy(".readConfigFile. The content: \n" + serviceHolder.toXml());
		return serviceHolder;
	}

}

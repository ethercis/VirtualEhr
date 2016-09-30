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
package com.ethercis.vehr;

import com.ethercis.servicemanager.annotation.QuerySetting;
import com.ethercis.servicemanager.annotation.QuerySyntax;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceClassScanner;
import com.ethercis.vehr.RequestDispatcher.ServiceAttribute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuerySyntaxMapper implements I_DispatchMapper {
    RunTimeSingleton global;
	Logger log= LogManager.getLogger(QuerySyntaxMapper.class);
    String serviceClassDefinition ; //if set, gives the list of prefix to scan for when loading services

    I_ServiceRunMode.DialectSpace dialectSpace;
    Boolean asyncQueryService = false;

    public QuerySyntaxMapper(RunTimeSingleton global){
        String compatibilityValue = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_PARAMETER, I_ServiceRunMode.DialectSpace.STANDARD.toString());
        dialectSpace = I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue);
        asyncQueryService = global.getProperty().get(I_ServiceRunMode.SERVER_ASYNC_MODE, false);
        serviceClassDefinition = global.getProperty().get(I_ServiceRunMode.SERVER_SERVICE_CLASS_DEF, "com.ethercis");
        this.global = global;
    }
	
	@Override
	public void loadConfiguration(RequestDispatcher requestDispatcher) throws ServiceManagerException {
		// Load Service
		ServiceClassScanner scanner = new ServiceClassScanner();
		List<Class<Service>> classes = scanner.getServiceClasses(global, serviceClassDefinition);
		requestDispatcher.configurationAuthor = "ethercis";
		requestDispatcher.configurationID = "EtherCISConfiguration";
		requestDispatcher.configurationOrganization = "ethercis";
		requestDispatcher.configurationVersion = "1.0";

		for (Class<Service> clazz : classes) {
			Service service = clazz.getAnnotation(Service.class);
			
			if (service != null) {
				for (Method m : clazz.getMethods()) {
					
					if (m.isAnnotationPresent(QuerySetting.class)) {
                        for (QuerySyntax querySyntax : m.getAnnotation(QuerySetting.class).dialect()) {
                            if (querySyntax.mode().compareTo(dialectSpace) == 0) { //to replace conditional compilation
                                MethodName actionname = MethodName.toMethodName(querySyntax.httpMethod());
                                Map<String, ServiceAttribute> servicemap = requestDispatcher.actionmap.get(actionname.getMethodName());
                                if (servicemap == null) {
                                    servicemap = new HashMap<>();
                                    requestDispatcher.actionmap.put(actionname.getMethodName(),
                                            servicemap);
                                }
                                ServiceAttribute sa = servicemap.get(querySyntax.path());
                                if (sa == null) {
                                    sa = new ServiceAttribute(requestDispatcher,
                                            service.id(), service.version(), "");
                                    servicemap.put(querySyntax.path(), sa);
                                }
                                log.debug("set method=" + actionname.getMethodName() + ":" + querySyntax.path() + " on " + clazz.getName() + ":" + m.getName());
                                sa.setMethod(true,
                                        MethodName.toMethodName(querySyntax.method()),
                                        m.getName(), querySyntax.responseType().toString(), asyncQueryService /*Modifier.isSynchronized(m.getModifiers())*/,
                                        m.getParameterTypes());
                            }
                        }
                    }
				}

			}

		}

	}

}

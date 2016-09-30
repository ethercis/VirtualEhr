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

import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceClassScanner extends ClassPathScanningCandidateComponentProvider {

	Logger logger = LogManager.getLogger(ServiceClassScanner.class);

	public ServiceClassScanner() {
		super(false);
		addIncludeFilter(new AnnotationTypeFilter(Service.class));
	}

	public final List<Class<Service>> getServiceClasses(RunTimeSingleton global, String basePackage) throws ServiceManagerException {
		if (basePackage == null)
			throw new ServiceManagerException(global,
					SysErrorCode.USER_CONFIGURATION,
					"ServiceClassScanner", "No class definition given in service loader, aborting");

		List<Class<Service>> classes = new ArrayList<Class<Service>>();
		for (String classDef: basePackage.split(",")) {
			logger.info("Resolving class definition:"+classDef);
			for (BeanDefinition candidate : findCandidateComponents(classDef)) {
				try {
					@SuppressWarnings("unchecked")
					Class<Service> clazz = (Class<Service>) ClassUtils.resolveClassName(candidate.getBeanClassName(),
							ClassUtils.getDefaultClassLoader());
					classes.add(clazz);
				} catch (Exception ex) {
					throw new ServiceManagerException(global,
							SysErrorCode.USER_CONFIGURATION,
							"ServiceClassScanner", "Could not resolve class definition:"+classDef+", error:"+ex.getMessage());
				}
			}
		}
		return classes;
	}

}

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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceClassScanner extends ClassPathScanningCandidateComponentProvider {

	public ServiceClassScanner() {
		super(false);
		addIncludeFilter(new AnnotationTypeFilter(Service.class));
	}

	public final List<Class<Service>> getServiceClasses(String basePackage) {
		basePackage = basePackage == null ? "" : basePackage;
		List<Class<Service>> classes = new ArrayList<Class<Service>>();
		for (BeanDefinition candidate : findCandidateComponents(basePackage)) {
			try {
				@SuppressWarnings("unchecked")
				Class<Service> cls = (Class<Service>) ClassUtils.resolveClassName(candidate.getBeanClassName(),
								            ClassUtils.getDefaultClassLoader());
				classes.add((Class<Service>) cls);
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}
		return classes;
	}

}

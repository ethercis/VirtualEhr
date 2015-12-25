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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.ehrserver.servicemap.Action;
import com.ethercis.ehrserver.servicemap.MapperDocument;
import com.ethercis.vehr.RequestDispatcher.ServiceAttribute;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileDispatchMapper implements I_DispatchMapper {
	private Logger log = Logger.getLogger(FileDispatchMapper.class);
	private String ME = "DispatchMapFactoryFile";
	private String mapfile;
	private RunTimeSingleton global;
	
	public FileDispatchMapper(RunTimeSingleton global, String mapfile) {
		super();
		this.global=global;
		this.mapfile = mapfile;
	}

	@Override
	public void loadConfiguration(RequestDispatcher requestDispatcher) throws ServiceManagerException {
		File f = new File(mapfile);

		if (!f.exists()) {
			log.warn("Could not find config file:" + mapfile);
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION,
					ME, "Could not find config file:" + mapfile);
		}

		try {
			MapperDocument mapper = MapperDocument.Factory.parse(f);

			log.info("Loading mapper configuration id:"
					+ mapper.getMapper().getId() + ", version:"
					+ mapper.getMapper().getVersion());

			requestDispatcher.configurationAuthor = mapper.getMapper().getAuthor();
			requestDispatcher.configurationID = mapper.getMapper().getId();
			requestDispatcher.configurationOrganization = mapper.getMapper()
					.getOrganization();
			requestDispatcher.configurationVersion = mapper.getMapper().getVersion();

			for (com.ethercis.ehrserver.servicemap.Action action : mapper.getMapper().getActionArray()) {
				// add new entry
				Map<String, ServiceAttribute> servicemap = new HashMap<String, ServiceAttribute>();
				// this looks a bit complicated, but it is required to ensure
				// that there is no upper-lower case issues there...
				MethodName actionname = MethodName.toMethodName(action.getCategory());
				requestDispatcher.actionmap.put(actionname.getMethodName(), servicemap);

				for (Action.Service s : action.getServiceArray()) {
					ServiceAttribute sa = new ServiceAttribute(requestDispatcher,
							s.getServiceid(), s.getServiceversion(),
							s.getResource());
					servicemap.put(s.getPath(), sa);
					// insert this entry into the actionmap
					// add methods
					for (Action.Service.Method m : s.getMethodArray()) {

						// add signature if any or default to
						// ClientSessionProperties

						List<Class<?>> clazzes = new ArrayList<Class<?>>();

						Action.Service.Method.Parameters parms = m.getParameters();
						if (parms != null) {
							for (String classname : parms.getClass1Array()) {
								try {
									Class<?> c = Class.forName(classname);
									clazzes.add(c);
								} catch (ClassNotFoundException e) {
									log.warn("Could not resolve class name:"
											+ classname);
									throw new ServiceManagerException(global,
											SysErrorCode.USER_CONFIGURATION,
											ME, "Could not resolve class name:"
													+ classname);
								}
							}
						}
						boolean resolve = !(s.getResource()
								.equals(Constants.INTERNAL_RESOURCE_MAP_ONLY));
						boolean async = m.isSetAsync() ? m.getAsync() : false;

						sa.setMethod(resolve,
								MethodName.toMethodName(m.getName()),
								m.getImplementation(), m.getReturn(), async,
								parms == null ? null : clazzes.toArray());

					}
				}
			}

		} catch (XmlException e) {
			log.warn("Could not parse config file:" + mapfile + ","
					+ e.getMessage());
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION,
					ME, "Could not parse config file:" + mapfile + ","
							+ e.getMessage());
		} catch (IOException e) {
			log.warn("Could not parse config file:" + mapfile + ","
					+ e.getMessage());
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION,
					ME, "Could not parse config file:" + mapfile + ","
							+ e.getMessage());
		}


	}

}

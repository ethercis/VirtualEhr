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

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public interface I_Service {
	   /**
	    * This method is called by the ServiceManager.
	    * <p/>
	    * Example how options are evaluated:
	    * <pre>
	    *   // An entry in services.properties (in one line):
	    *   MimeSubscribeService[ContentLenFilter][1.0]=\
	    *                 com.ethercis.mime.demo.ContentLenFilter,\
	    *                 DEFAULT_MAX_LEN=200,DEFAULT_MIN_LEN=20
	    *
	    *  // Access it like this:
	    *  java.common.Properties props = serviceInfo.getParameters();
	    *  String maxLen = (String)props.get("DEFAULT_MAX_LEN");
	    *  String throwLen = (String)props.get("THROW_EXCEPTION_FOR_LEN");
	    * </pre>
	    * @param glob   An ehrserver instance global object holding logging and property informations
	    * @param serviceInfo A container holding informations about the service, e.g. its parameters
	    */
	   public void init(RunTimeSingleton glob, ServiceInfo serviceInfo) throws ServiceManagerException;

	   public String getType();
	   public String getVersion();

	   /**
	    * Cleans up the resource.
	    * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException if an exception occurs. The exception is
	    * handled by the RunLevelManager depending on how the service has been
	    * configured with the action:
	    * <p/>
	    * &lt;action do='STOP' onShutdownRunlevel='2' sequence='5'
	    *     onFail='resource.configuration.serviceFailed'>
	    * 
	    * If onFail is defined to something, the RunLevelManager will stop.
	    */
	   public void shutdown() throws ServiceManagerException;
}

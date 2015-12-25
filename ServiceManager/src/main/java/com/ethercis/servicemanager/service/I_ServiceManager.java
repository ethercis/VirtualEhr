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

import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public interface I_ServiceManager {
	   /**
	    * Return a specific Service.
	    * @param String The type of the requested Service.
	    * @param String The version of the requested Service.
	    * @return I_Service The Service which is suitable to handle the request.
	    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown if no suitable Service has been found.
	    */
	   public I_Service getServiceObject(String type, String version) throws ServiceManagerException;
	   
	   /**
	   * @return The name of the property in services.property, e.g. "Security.Server.Service"
	   * for "Security.Server.Service[simple][1.0]"
	   */
	   // Renamed because of protected access
	   //public String getServicePropertyName();
	   public String getName();
	   
	   /**
	    * @return e.g. "Security.Server.Service[simple][1.0]"
	    */
	   public String createServicePropertyKey(String type, String version);
	   
	   /**
	    * @return The name of the property in services.property, e.g. "Security.Server.Service"
	    * for "Security.Server.Service[simple][1.0]"
	    */
	   public String getDefaultServiceName(String type, String version);
}

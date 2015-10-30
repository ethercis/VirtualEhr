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


package com.ethercis.servicemanager.jmx;

/**
 * Declares available methods to control an instance service.   
 */
public interface I_AdminService extends I_AdminUsage {
   /**
    * Activate this service
    */
   public void activate() throws Exception;

   /**
    * Deactivate the service to standby. 
    * A call to activate() fires the service up again
    */
   public void deActivate();
   
   /**
    * Access the current state
    * @return true if active
    */
   public boolean isActive();
   
   /**
    * The unique name of the service (together with the version). 
    * @return For example "IOR"
    */
   public java.lang.String getType();
   
   /**
    * The version of the service
    * @return For example "1.0"
    */
   public java.lang.String getVersion();

   /**
    * Shutdown the service, free knowledge.
    */
   public void shutdown() throws Exception;

   /**
    * Check status 
    * @return true if down
    */
   public boolean isShutdown();
}

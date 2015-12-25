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
 * Project: EtherCIS openEHR system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */

//Copyright
package com.ethercis.logonservice.access;

/**
 * Declares JMX available methods of a ConnectProperties instance. 
 */
public interface ConnectPropertiesMBean {
   /** How often the same client may login */
   public int getMaxSessions();
   /** The configured session live span in milli seconds */
   public long getSessionTimeout();
   /** The configured session live span in milli seconds */
   public void setSessionTimeout(long timeout);
   /** Does the client accept PtP messages? */
   public boolean isPtpAllowed();
   /** If this flag is set, the session will persist a server crash. */
   public boolean isPersistent();
}

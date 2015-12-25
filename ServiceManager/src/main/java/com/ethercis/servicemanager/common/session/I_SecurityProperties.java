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
package com.ethercis.servicemanager.common.session;

import com.ethercis.servicemanager.exceptions.ServiceManagerException;


/**
 * A client helper.
 *
 * The reason is for clients to access xmlBlaster
 * transparently from the authentication method
 * <p />
 * Here is a typical example for a password based QoS
 * <pre>
 *  &lt;qos>
 *     &lt;securityService type='htpasswd' version='1.0'>
 *        &lt;![CDATA[
 *           &lt;user>michele&lt;/user>
 *           &lt;passwd>secret&lt;/passwd>
 *        ]]>
 *     &lt;/securityService>
 *  &lt;/qos>
 * </pre>
 */
public interface I_SecurityProperties {

   /**
    * Parse the given xml string which contains the userId and credentials.
    * Should be able to parse with or without surrounding &lt;security> tag
    */
   public void parse(I_ConnectProperties props) throws ServiceManagerException;

   /**
    * Set the userId for the login.
    * <p/>
    * @param String userId
    */
   public void setUserId(String userId);

   /**
    * Get the userId, which is used for the login;
    */
   public String getUserId();

   public void setClientIp (String ip);

   /**
    * Access the remote IP of the socket - the clients IP.
    * <p />
    * Currently only passed by the SOCKET protocol plugin, other plugins return null
    * @return null if not known or something like "192.168.1.2"
    *
    */
   public String getClientIp();

   /**
    * Set the credential (password etc.).
    * <p/>
    * @param String credential
    */
   public void setCredential(String cred);

   /**
    * Access the credential (e.g. password)
    */
   public String getCredential();

   /**
    * Serialize the information.
    */
   public String toXml(String extraOffset);

}

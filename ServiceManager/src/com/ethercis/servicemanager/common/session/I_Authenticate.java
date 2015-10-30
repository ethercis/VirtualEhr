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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * All login/logout or connect/disconnect calls access
 * authentication services through these methods.
 */
public interface I_Authenticate
{
   public boolean sessionExists(String sessionId);

   public RunTimeSingleton getGlobal();

   /**
    */
   public I_SessionProperties connect(I_ConnectProperties props) throws ServiceManagerException;


   public I_SessionProperties connect(I_ConnectProperties props, String sessionId) throws ServiceManagerException;

   /**
    */
   public void disconnect(String sessionId) throws ServiceManagerException;

   /**
     */
   public String ping(String addressServer, String qos);

   /**
    * A protocol may inform the client is lost (currently only the SOCKET protocol plugin supports it)
    * @param state ConnectionStateEnum.DEAD
    */
   public void connectionState(String secretSessionId, I_ConnectionStateEnum state);

   public String toXml() throws ServiceManagerException;
}



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
package com.ethercis.servicemanager.common.security;

import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.I_Service;

/**
 * Used to hide session handling
 * @author C.Chevalley
 *
 */
public interface I_Manager extends I_Service
{
   // --- session handling ----------------------------------------------------

   // querySubjects(String query, String syntax)

   /**
    * The session handling.
    * <code>connect(...)</code>
    * and <code>login(...)</code> calls this method to get a new I_Session
    * and bind it to the session.
    * <p/>
    * @param String sessionId
    */
   public I_Session reserveSession(String sessionId) throws ServiceManagerException;

   /**
    * Releases a reserved I_Session.
    * <p/>
    * @param String The id of the session, which has to be released.
    * @param String This qos literal could contain a proof of authenticity, etc.
    */
   public void releaseSession(String sessionId, String qos_literal) throws ServiceManagerException;

   /**
    * Get the I_Session which corresponds to the given sessionId.
    * <p/>
    * @param String The sessionId
    * @return I_Session
    */
   public I_Session getSessionById(String id) throws ServiceManagerException;

}

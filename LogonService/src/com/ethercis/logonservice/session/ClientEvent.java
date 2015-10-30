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


//Copyright
package com.ethercis.logonservice.session;

import com.ethercis.servicemanager.common.security.I_SubjectInfo;
import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionProperties;

/**
 * An event which indicates that a client did a login or logout.
 * It carries the SessionInfo reference inside
 */
public class ClientEvent extends java.util.EventObject {
   private static final long serialVersionUID = -4613461343832833084L;
   public final I_SessionProperties previousConnectSessionProperties;
   
   /**
    * Constructs a ClientEvent object.
    *
    * @param the client which does the login or logout
    */
   public ClientEvent(I_SubjectInfo subjectInfo) {
       super(subjectInfo);
       this.previousConnectSessionProperties = null;
   }

   /**
    * Constructs a ClientEvent object.
    *
    * @param the client which does the login or logout
    */
   public ClientEvent(I_SessionInfo sessionInfo) {
       super(sessionInfo);
       this.previousConnectSessionProperties = null;
   }

   public ClientEvent(I_SessionProperties previousConnectProps, I_SessionInfo sessionInfo) {
      super(sessionInfo);
      this.previousConnectSessionProperties = previousConnectProps;
   }

   /**
    * Returns the connectQos or null of the event.
    * @return the connectQos (could be null if not passed in the constructor)
    */
   public I_ConnectProperties getConnectProperties() {   
	   I_SessionInfo info = getSessionInfo();
       return info.getConnectProperties();
   }
   
   /**
    * Returns the originator of the event.
    *
    * @return the client which does the login or logout
    */
   public I_SessionInfo getSessionInfo() {
       return (I_SessionInfo)source;
   }

   /**
    * Returns the originator of the event.
    *
    * @return the client which does the login or logout
    */
   public I_SubjectInfo getSubjectInfo() {
       return (I_SubjectInfo)source;
   }

   /**
    * Given for sessionUpdated() calls
    * @return can be null
    */
   public I_SessionProperties getPreviousConnectSessionProperties() {
      return previousConnectSessionProperties;
   }
}

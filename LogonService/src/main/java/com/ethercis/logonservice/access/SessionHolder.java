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
//Copyright
package com.ethercis.logonservice.access;

import com.ethercis.servicemanager.common.session.I_SessionHolder;
import com.ethercis.servicemanager.common.session.I_SessionInfo;

/**
 * Container to transport information to the isAuthorized() method. 
 */
public class SessionHolder implements I_SessionHolder {
   private I_SessionInfo sessionInfo;

   /**
    * @param sessionInfo
    * @param addressServer
    */
   public SessionHolder(I_SessionInfo sessionInfo) {
      super();
      this.sessionInfo = sessionInfo;

   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_SessionHolder#getSessionInfo()
 */
   @Override
public I_SessionInfo getSessionInfo() {
      return this.sessionInfo;
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_SessionHolder#setSessionInfo(com.ethercis.logonservice.session.SessionInfo)
 */
   @Override
public void setSessionInfo(I_SessionInfo sessionInfo) {
      this.sessionInfo = sessionInfo;
   } 
}

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
package com.ethercis.logonservice.session.protectors;

import com.ethercis.logonservice.session.SubjectInfo;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionInfoProtector;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * SubjectInfoProtector protects SubjectInfo.java from direct access by administrative tasks. 
 * <p>
 * See javadoc of SubjectInfo.java for a description
 * </p>
 * @author <a href="mailto:xmlBlaster@marcelruff.info">Marcel Ruff</a>
 * @param <I_AdminSession>
 */
public final class SubjectInfoProtector<I_AdminSession> implements SubjectInfoProtectorMBean
{
   private final SubjectInfo subjectInfo;

   public SubjectInfoProtector(SubjectInfo subjectInfo) {
      this.subjectInfo = subjectInfo;
   }

   public long getUptime() {
      return this.subjectInfo.getUptime();
   }

   public String getCreationDate() {
      return this.subjectInfo.getCreationDate();
   }

   public long getNumUpdate() {
      return this.subjectInfo.getNumUpdate();
   }

   public int getNumSessions() {
      return this.subjectInfo.getNumSessions();
   }

   public int getMaxSessions() {
      return this.subjectInfo.getMaxSessions();
   }

   public void setMaxSessions(int maxSessions) {
      this.subjectInfo.setMaxSessions(maxSessions);
   }

   public boolean isBlockClientLogin() {
      return this.subjectInfo.isBlockClientLogin();
   }

   public String setBlockClientLogin(boolean blockClient) {
      return this.subjectInfo.setBlockClientLogin(blockClient);
   }

   public String blockClientAndResetConnections() {
      return this.subjectInfo.blockClientAndResetConnections();
   }

   public String getSessionList() {
      return this.subjectInfo.getSessionList();
   }

   public String[] getSessions() {
      I_SessionInfo[] arr = this.subjectInfo.getSessions();
      String[] ret = new String[arr.length];
      for (int i=0; i<arr.length; i++) {
         ret[i] = ""+arr[i].getPublicSessionId();
      }
      return ret;
   }

   public I_SessionInfoProtector getSessionByPubSessionId(long pubSessionId) {
      return this.subjectInfo.getSessionByPubSessionId(pubSessionId);
   }

   public String killClient() throws ServiceManagerException {
      return this.subjectInfo.killClient();
   }


   /** JMX */
   public String usage() {
      return this.subjectInfo.usage();
   }
   /** JMX */
   public String getUsageUrl() {
      return this.subjectInfo.getUsageUrl();
   }
   /* JMX dummy to have a copy/paste functionality in jconsole */
   public void setUsageUrl(String url) {}
}

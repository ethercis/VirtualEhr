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
package com.ethercis.logonservice.session.protectors;

import java.util.Map;

import com.ethercis.logonservice.session.SessionInfo;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_SessionInfoProtector;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * SessionInfoProtector protects SessionInfo.java from direct access by administrative tasks.
 * <p>
 * See javadoc of SessionInfo.java
 * </p>
 * @author <a href="mailto:xmlBlaster@marcelruff.info">Marcel Ruff</a>
 */
public class SessionInfoProtector implements SessionInfoProtectorMBean /*I_AdminSession*/, I_SessionInfoProtector
{
   private final SessionInfo sessionInfo;

   public SessionInfoProtector(SessionInfo sessionInfo) {
      this.sessionInfo = sessionInfo;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getId()
 */
@Override
public final String getId() {
      return this.sessionInfo.getId();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getLoginName()
 */
@Override
public final String getLoginName() {
      return this.sessionInfo.getLoginName();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getQos()
 */
@Override
public final String getQos() {
      return this.sessionInfo.getQos();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getConnectionState()
 */
@Override
public final String getConnectionState() {
      return this.sessionInfo.getConnectionState();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getPublicSessionId()
 */
@Override
public final long getPublicSessionId() {
      return this.sessionInfo.getPublicSessionId();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getLoginDate()
 */
@Override
public final String getLoginDate() {
      return this.sessionInfo.getLoginDate();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getSessionTimeoutExpireDate()
 */
@Override
public final String getSessionTimeoutExpireDate() {
      return this.sessionInfo.getSessionTimeoutExpireDate();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getAliveSinceDate()
 */
@Override
public final String getAliveSinceDate() {
      return this.sessionInfo.getAliveSinceDate();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getPollingSinceDate()
 */
@Override
public final String getPollingSinceDate() {
      return this.sessionInfo.getPollingSinceDate();
   }


   public final void refreshSession() throws ServiceManagerException {
      this.sessionInfo.refreshSession();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getUptime()
 */
@Override
public final long getUptime() {
      return this.sessionInfo.getUptime();
   }


   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getUserObjectMap()
 */
@Override
public Map<String, Object> getUserObjectMap() {
	   return this.sessionInfo.getUserObjectMap();
   }


   public final String killSession() throws ServiceManagerException {
      return this.sessionInfo.killSession();
   }
   
   public String disconnectClientKeepSession() {
      return this.sessionInfo.disconnectClientKeepSession();
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#isBlockClientSessionLogin()
 */
@Override
public boolean isBlockClientSessionLogin() {
      return this.sessionInfo.isBlockClientSessionLogin();
   }

   public String setBlockClientSessionLogin(boolean blockClient) {
      return this.sessionInfo.setBlockClientSessionLogin(blockClient);
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getMaxSessions()
 */
   @Override
public final long getMaxSessions() {
      return this.sessionInfo.getConnectProperties().getSessionProperties().getMaxSessions();
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getSessionTimeout()
 */
   @Override
public final long getSessionTimeout() {
      return this.sessionInfo.getConnectProperties().getSessionProperties().getSessionTimeout();
   }
   /** Enforced by ConnectQosDataMBean interface. */
   public final void setSessionTimeout(long timeout) {
      this.sessionInfo.setSessionTimeout(timeout);
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#isPtpAllowed()
 */
   @Override
public final boolean isPtpAllowed() {
      return this.sessionInfo.getConnectProperties().isPtpAllowed();
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#isPersistent()
 */
   @Override
public final boolean isPersistent() {
      return this.sessionInfo.getConnectProperties().isPersistent();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getRemoteProperties()
 */
@Override
public String[] getRemoteProperties() {
      ClientProperty[] cp = this.sessionInfo.getRemotePropertyArr();
      String[] arr = new String[cp.length];
      for (int i=0; i<cp.length; i++)
         arr[i] = cp[i].toXml("", "remoteProperty", true).trim();
      return arr;
   }

   public String clearRemotePropertiesStartingWith(String prefix) {
      if (this.sessionInfo.getRemoteProperties() == null || prefix == null) return "No remote properties found, nothing to clear";
      int count = this.sessionInfo.clearRemoteProperties(prefix);
      return "Removed " + count + " remote properties which are starting with '"+prefix+"'";
   }

   public String clearRemoteProperties() {
      if (this.sessionInfo.getRemoteProperties() == null) return "No remote properties found, nothing to clear";
      int count = this.sessionInfo.clearRemoteProperties(null);
      return "Removed " + count + " remote properties";
   }

   public String addRemoteProperty(String key, String value) {
      ClientProperty old = this.sessionInfo.addRemoteProperty(key, value);
      if (old == null)
         return "Added client property '" + key + "'";
      else
         return "Replaced existing client property '" + old.toXml("", "remoteProperty").trim() + "'";
   }

   /** JMX */
   public String usage() {
      return this.sessionInfo.usage();
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getUsageUrl()
 */
   @Override
public String getUsageUrl() {
      return this.sessionInfo.getUsageUrl();
   }
   /* JMX dummy to have a copy/paste functionality in jconsole */
   public void setUsageUrl(String url) {}

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#getConnectProperties()
 */
@Override
public I_ConnectProperties getConnectProperties() {
      return this.sessionInfo.getConnectProperties();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#isStalled()
 */
@Override
public boolean isStalled() {
      return this.sessionInfo.isStalled();
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.protectors.I_SessionInfoProtector#isAcceptWrongSenderAddress()
 */
@Override
public boolean isAcceptWrongSenderAddress() {
      return this.sessionInfo.isAcceptWrongSenderAddress();
   }

   public void setAcceptWrongSenderAddress(boolean acceptWrongSenderAddress) {
      this.sessionInfo.setAcceptWrongSenderAddress(acceptWrongSenderAddress);
   }
}

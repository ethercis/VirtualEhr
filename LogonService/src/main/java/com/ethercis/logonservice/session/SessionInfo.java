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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import com.ethercis.logonservice.session.protectors.SessionInfoProtector;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.common.ClientPropertiesInfo;
import com.ethercis.servicemanager.common.ClientProperty;
import com.ethercis.servicemanager.common.I_Timeout;
import com.ethercis.servicemanager.common.IsoDateJoda;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.Timeout;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.security.I_SubjectInfo;
import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionName;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxMBeanHandle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * SessionInfo stores all known session data about a client.
 * <p />
 * One client (SubjectInfo) may have multiple login sessions.
 */
public final class SessionInfo implements I_Timeout, I_SessionInfo
{
   private String ME = "SessionInfo";
   private ContextNode contextNode;
   /** The cluster wide unique identifier of the session e.g. "/node/heron/client/joe/2" */
   private final I_SessionName sessionName;
   private I_SubjectInfo subjectInfo; // all client informations
   private I_Session securityCtx;
   private static long instanceCounter = 0L;
   private long instanceId = 0L;
   /** The current connection address from the protocol plugin */
   private I_ConnectProperties connectProperties;
   private Timeout expiryTimer;
   private TimeStamp timerKey;
   private RunTimeSingleton glob;
   private static Logger log = LogManager.getLogger(SessionInfo.class);
 
   private boolean isShutdown = false;
   /** Protects timerKey refresh */
   private final Object EXPIRY_TIMER_MONITOR = new Object();
   private SessionInfoProtector sessionInfoProtector;
   /** My JMX registration */
   private JmxMBeanHandle mbeanHandle;
   /** To prevent noisy warnings */
   private boolean transientWarn;
   /** Can be optionally used by authorization frameworks */
   private Object authorizationCache;
   private boolean blockClientSessionLogin;

   private ServiceManagerException transportConnectFail;

   /** Holding properties send by our remote client via the topic __sys__sessionProperties */
   private ClientPropertiesInfo remoteProperties;

   private boolean acceptWrongSenderAddress;

   // Enforced by I_AdminSubject
   /** Incarnation time of this object instance in millis */
   private long startupTime;

   private ReentrantLock lock = new ReentrantLock();

   private boolean initialized;
   
   /**
    * Map to store arbitrary info for this client, is cleaned up automatically when session dies
    * Useful for example for plugins
    */
   private Map<String, Object> userMap = Collections.synchronizedMap(new HashMap<String, Object>());


   /**
    * Create this instance when a client did a login.
    * <p />
    * You need to call initializeSession()!
    */
   SessionInfo(RunTimeSingleton glob, I_SessionName sessionName) {
      this.glob = glob;
      synchronized (SessionInfo.class) {
    	  instanceCounter--;
    	  instanceId = instanceCounter;

      }
      // client has specified its own publicSessionId (> 0)
      this.sessionName = (sessionName.isPubSessionIdUser()) ? sessionName :
         new SessionName(glob, sessionName, getInstanceId());
   }
   
   /**
    * @param subjectInfo the SubjectInfo with the login informations for this client
    */
   public void init(I_SubjectInfo subjectInfo, I_Session securityCtx, I_ConnectProperties connectProps)
          throws ServiceManagerException {

      if (securityCtx==null) {
         String tmp = "SessionInfo(securityCtx==null); A correct security manager must be set.";
         log.debug(tmp);
         throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_CONFIGURATION, ME, tmp);
      }
      this.sessionInfoProtector = new SessionInfoProtector(this);

      //this.id = ((prefix.length() < 1) ? "client/" : (prefix+"/client/")) + subjectInfo.getLoginName() + "/" + getPublicSessionId();

      this.contextNode = new ContextNode(ContextNode.SESSION_MARKER_TAG, ""+this.sessionName.getPublicSessionId(),
                                       subjectInfo.getContextNode());
      this.ME = this.instanceId + "-" + this.sessionName.getRelativeName();


       log.debug(ME+": Creating new SessionInfo " + instanceId + ": " + subjectInfo.toString());
      this.startupTime = System.currentTimeMillis();
      this.subjectInfo = subjectInfo;
      this.securityCtx = securityCtx;
      this.connectProperties = connectProps;


      this.expiryTimer = glob.getSessionTimer();
      if (connectProps.getSessionProperties().getSessionTimeout() > 0L) {
         log.debug(ME+": Setting expiry timer for " + getLoginName() + " to " + connectProps.getSessionProperties().getSessionTimeout() + " msec");
         this.timerKey = this.expiryTimer.addTimeoutListener(this, connectProps.getSessionProperties().getSessionTimeout(), null);
      }
      else {
         log.debug(ME+": Session lasts forever, requested expiry timer was 0");
      }

      // "__remoteProperties"
      if (this.connectProperties.getSessionProperties().getClientProperties().getClientProperty(Constants.CLIENTPROPERTY_REMOTEPROPERTIES, false)) {
          mergeRemoteProperties(this.connectProperties.getSessionProperties().getClientProperties().getClientProperties());
      }

      // TODO: Decide by authorizer
      // see Authenticate.java boolean may = glob.getProperty().get("xmlBlaster/acceptWrongSenderAddress", false);
      this.acceptWrongSenderAddress = glob.getProperty().get("ehrserver/acceptWrongSenderAddress/"+getSessionName().getLoginName(), false);

      // JMX register "client/joe/1"
      this.mbeanHandle = this.glob.registerMBean(this.contextNode, this.sessionInfoProtector);
      
      this.initialized = true;
   }
   
   public final boolean isInitialized() {
      return this.initialized;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#isAlive()
 */
@Override
public final boolean isAlive() {
      return !isShutdown();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getContextNode()
 */
   @Override
public final ContextNode getContextNode() {
      return this.contextNode;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#isAcceptWrongSenderAddress()
 */
   @Override
public boolean isAcceptWrongSenderAddress() {
      return this.acceptWrongSenderAddress;
   }

   /**
    * @param acceptWrongSenderAddress the acceptWrongSenderAddress to set
    */
   public void setAcceptWrongSenderAddress(boolean acceptWrongSenderAddress) {
      boolean old = this.acceptWrongSenderAddress;
      this.acceptWrongSenderAddress = acceptWrongSenderAddress;
      String tmp = ME + "Changed acceptWrongSenderAddress from " + old + " to " + this.acceptWrongSenderAddress + ".";
      //if (glob.getAuthenticate().iscceptWrongSenderAddress()
      if (this.acceptWrongSenderAddress == true)
         log.warn(tmp + " Caution: This client can now publish messages using anothers login name logonservice sender");
      else
         log.info(tmp + " Faking anothers publisher address is not possible");
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getSessionInfoProtector()
 */
   @Override
public final SessionInfoProtector getSessionInfoProtector() {
      return this.sessionInfoProtector;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getInstanceId()
 */
   @Override
public final long getInstanceId() {
      return this.instanceId;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getLock()
 */
   @Override
public ReentrantLock getLock() {
      return this.lock;
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#releaseLockAssertOne(java.lang.String)
 */
   @Override
public long releaseLockAssertOne(String errorInfo) {
      int holds = this.lock.getHoldCount();
      if (holds != 1) {
         log.debug("Topic=" + getId() + " receiverSession=" + getId() +". Not expected lock holds=" + holds + "\n" + RunTimeSingleton.getStackTraceAsString(null));
      }
      if (holds > 0) {
         for (int i = 0; i < holds; ++i) {
            try {
              this.lock.unlock();
            }
            catch (Throwable e) {
               log.debug("Free lock failed: " + e.toString() + " " + errorInfo + " receiverSession=" + getId() +". Not expected lock holds=" + holds + "\n" + RunTimeSingleton.getStackTraceAsString(null));
            }
         }
      }
      return holds;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getPublicSessionId()
 */
   @Override
public final long getPublicSessionId() {
      return this.sessionName.getPublicSessionId();
   }

   public void finalize() {
      try {
         removeExpiryTimer();
         log.debug(ME+": finalize - garbage collected " + getSecretSessionId());
      }
      catch (Throwable e) {
         e.printStackTrace();
      }
      try {
         super.finalize();
      }
      catch (Throwable e) {
         e.printStackTrace();
      }
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#isShutdown()
 */
@Override
public boolean isShutdown() {
      this.lock.lock();
      try {
         return this.isShutdown; // sync'd because of TimeoutListener?
      }
      finally {
         try {
            this.lock.unlock();
         }
         catch (Throwable e) {
        	 e.printStackTrace();
         }
      }
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#removeExpiryTimer()
 */
@Override
public void removeExpiryTimer() {
      synchronized (this.EXPIRY_TIMER_MONITOR) {
         if (this.timerKey != null) {
            this.expiryTimer.removeTimeoutListener(this.timerKey);
            this.timerKey = null;
         }
      }
   }

   public void shutdown() {
      log.debug(ME+": shutdown() of session");
      this.lock.lock();
      try {
         if (this.isShutdown)
            return;
         this.isShutdown = true;
      }
      finally {
    	 try {
            this.lock.unlock();
    	 }
    	 catch (Throwable e) {
    		 e.printStackTrace();
    	 }
      }
      this.glob.unregisterMBean(this.mbeanHandle);
      removeExpiryTimer();

      this.subjectInfo = null;
      // this.securityCtx = null; We need it in finalize() getSecretSessionId()
      // this.connectQos = null;
      this.expiryTimer = null;
   }


   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#refreshSession()
 */
   @Override
public final void refreshSession() throws ServiceManagerException {
      if (connectProperties.getSessionProperties().getSessionTimeout() > 0L) {
         synchronized (this.EXPIRY_TIMER_MONITOR) {
            Timeout expiryTimer = this.expiryTimer;
            if (expiryTimer != null) {
               this.timerKey = expiryTimer.addOrRefreshTimeoutListener(this, connectProperties.getSessionProperties().getSessionTimeout(), null, this.timerKey);
            }
         }
      }
      else {
         removeExpiryTimer();
      }
   }

   /**
    * We are notified when this session expires.
    * @param userData You get bounced back your userData which you passed
    *                 with Timeout.addTimeoutListener()
    */
   public final void timeout(Object userData) {
      // lock could cause deadlock with topicHandler.lock()
      // it is not needed here logonservice the disconnect from remote clients
      // also can come at any time and the core must be capable to handle this.
      //this.lock.lock();
      //try {
      synchronized (this.EXPIRY_TIMER_MONITOR) {
         this.timerKey = null;
      }
      log.warn(ME+": Session timeout for " + getLoginName() + " occurred, session '" + getSecretSessionId() + "' is expired, autologout");

      try {
    	  if (glob.getAuthenticate() != null) //it's possible the disconnect has been done on another thread...
    		  glob.getAuthenticate().disconnect(getSecretSessionId());
      } catch (ServiceManagerException e) {
         e.printStackTrace();
         log.debug(ME+": Internal problem with disconnect: " + e.toString());
      }
      //}
      //finally {
      //   this.lock.release();
      //}
   }


   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getConnectProperties()
 */
@Override
public final I_ConnectProperties getConnectProperties() {
      return this.connectProperties;
   }

   public final void updateConnectProperties(I_ConnectProperties newConnectProperties) {

      this.connectProperties = newConnectProperties; // Replaces SessionProperty settings like bypassCredentialCheck

      // "__remoteProperties"
      if (newConnectProperties.getSessionProperties().getClientProperties().getClientProperty(Constants.CLIENTPROPERTY_REMOTEPROPERTIES, false)) {
          mergeRemoteProperties(newConnectProperties.getSessionProperties().getClientProperties().getClientProperties());
      }

   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getLoginName()
 */
   @Override
public final String getLoginName() {
      I_SubjectInfo subjectInfo = this.subjectInfo;
      return (subjectInfo==null)?"--":subjectInfo.getLoginName();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getSubjectInfo()
 */
   @Override
public final I_SubjectInfo getSubjectInfo() {
      return this.subjectInfo;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getSecretSessionId()
 */
   @Override
public String getSecretSessionId() {
      return this.securityCtx.getSecretSessionId();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getSecuritySession()
 */
@Override
public I_Session getSecuritySession() {
      return this.securityCtx;
   }

   public void setSecuritySession(I_Session ctx) {
      this.securityCtx = ctx;
   }


   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getId()
 */
   @Override
public final String getId() {
      return this.sessionName.getAbsoluteName();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getSessionName()
 */
@Override
public final I_SessionName getSessionName() {
      return this.sessionName;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#isSameSession(com.ethercis.logonservice.session.I_SessionInfo)
 */
   @Override
public boolean isSameSession(I_SessionInfo sessionInfo) {
      return getId().equals(sessionInfo.getId());
   }


   /**
    * @see #getId
    */
   public final String toString() {
      return getId();
   }

   /**
    * Dump state of this object into a XML ASCII string.
    * <br>
    * @return internal state of SessionInfo logonservice a XML ASCII string
    */
   public final String toXml() {
      return toXml((String)null, (Properties)null);
   }

   /**
    * Dump state of this object into a XML ASCII string.
    * <br>
    * @param extraOffset indenting of tags for nice response
    * @return internal state of SessionInfo logonservice a XML ASCII string
    */
   public final String toXml(String extraOffset, Properties props) {
      StringBuffer sb = new StringBuffer(256);
      if (extraOffset == null) extraOffset = "";
      String offset = Constants.OFFSET + extraOffset;

      sb.append(offset).append("<SessionInfo id='").append(getId());

      Timeout expiryTimer = this.expiryTimer;
      long timeToLife = (expiryTimer != null) ? expiryTimer.spanToTimeout(timerKey) : 0;
      sb.append("' timeout='").append(timeToLife).append("'>");

      // Avoid dump of password
      if (props == null) props = new Properties();
      props.put(Constants.TOXML_NOSECURITY, ""+true);
//      sb.append(this.sessionProperty.toXml(extraOffset+Constants.INDENT, props));

      sb.append(offset).append("</SessionInfo>");

      return sb.toString();
   }

   //=========== Enforced by I_AdminSession ================
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getQos()
 */
@Override
public String getQos() {
      return (this.connectProperties == null) ? "" : this.connectProperties.getSessionProperties().toXml();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getUptime()
 */
@Override
public final long getUptime() {
      return (System.currentTimeMillis() - this.startupTime)/1000L;
   }


   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getLoginDate()
 */
@Override
public final String getLoginDate() {
      long ll = this.startupTime;
      java.sql.Timestamp tt = new java.sql.Timestamp(ll);
      return tt.toString();
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getSessionTimeoutExpireDate()
 */
@Override
public final String getSessionTimeoutExpireDate() {
      long timeToLife = this.expiryTimer.spanToTimeout(timerKey);
      if (timeToLife == -1) {
         return "unlimited";
      }
      long ll = System.currentTimeMillis() + timeToLife;
      java.sql.Timestamp tt = new java.sql.Timestamp(ll);
      return tt.toString();
   }

   // JMX
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getAliveSinceDate()
 */
@Override
public final String getAliveSinceDate() {
      long ll = 0;
      if (ll == 0) return "";
      java.sql.Timestamp tt = new java.sql.Timestamp(ll);
      return tt.toString();
   }

   // JMX
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getPollingSinceDate()
 */
@Override
public final String getPollingSinceDate() {
      long ll = 0;
      if (ll == 0) return "";
      java.sql.Timestamp tt = new java.sql.Timestamp(ll);
      return tt.toString();
   }


   /** JMX Enforced by ConnectQosDataMBean interface. */
   public final void setSessionTimeout(long timeout) {
      getConnectProperties().getSessionProperties().setSessionTimeout(timeout);
      try {
         refreshSession();
      } catch (ServiceManagerException e) {
         e.printStackTrace();
      }
   }


   /** JMX */
   public String usage() {
      return RunTimeSingleton.getJmxUsageLinkInfo(this.getClass().getName(), null);
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getUsageUrl()
 */
   @Override
public String getUsageUrl() {
      return RunTimeSingleton.getJavadocUrl(this.getClass().getName(), null);
   }
   /* JMX dummy to have a copy/paste functionality in jconsole */
   public void setUsageUrl(String url) {}

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getRemoteProperties()
 */
   @Override
public ClientPropertiesInfo getRemoteProperties() {
      return this.remoteProperties;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getRemotePropertyArr()
 */
   @Override
public ClientProperty[] getRemotePropertyArr() {
      ClientPropertiesInfo tmp = this.remoteProperties;
      if (tmp == null) return new ClientProperty[0];
      return tmp.getClientPropertyArr();
   }

   /**
    * Set properties send by our client.
    * @param remoteProperties The remoteProperties to set, pass null to reset.
    * The key is of type String and the value of type ClientProperty
    */
   public synchronized void setRemoteProperties(Map<String, ClientProperty> map) {
      if (map == null)
         this.remoteProperties = null;
      else
         this.remoteProperties = new ClientPropertiesInfo(map);
   }

   /**
    * Clear remote properties.
    * @param prefix if not null only keys starting with are removed
    * @return number of removed entries
    */
   public synchronized int clearRemoteProperties(String prefix) {
      if (prefix == null) {
         int size = 0;
         if (this.remoteProperties != null)
            size = this.remoteProperties.getClientPropertyMap().size();
         this.remoteProperties = null;
         return size;
      }
      
      ClientPropertiesInfo info = this.remoteProperties;
      if (info == null || prefix == null) return 0;
      ClientProperty[] arr = info.getClientPropertyArr();
      int count = 0;
      for (int i=0; i<arr.length; i++) {
         if (arr[i].getName().startsWith(prefix)) {
            info.getClientPropertyMap().remove(arr[i].getName());
            count++;
         }
      }
      return count;
   }

   /**
    * Update properties send by our client.
    * @param remoteProperties The remoteProperties to set,
    * if a property exists its value is overwritten, passing null does nothing
    * The key is of type String and the value of type ClientProperty
    */
   public synchronized void mergeRemoteProperties(Map<?, ?> map) {
      if (map == null || map.size() == 0) return;
      if (this.remoteProperties == null) {
          this.remoteProperties = new ClientPropertiesInfo(new HashMap<String, ClientProperty>());
          /*// Changed 2007-06-29 marcel: we now take a clone
         this.remoteProperties = new ClientPropertiesInfo(map);
         // remove, is only a hint:
         this.remoteProperties.put(Constants.CLIENTPROPERTY_REMOTEPROPERTIES, (ClientProperty)null);
         return;
         */
      }
      Iterator<?> it = map.keySet().iterator();
      while (it.hasNext()) {
         String key = (String)it.next();
         if (Constants.CLIENTPROPERTY_REMOTEPROPERTIES.equals(key))
             continue; // Remove, is only a flag
         if (Constants.CLIENTPROPERTY_UTC.equals(key)) {
            try {
                ClientProperty cpClientUtc = (ClientProperty)map.get(key);
                if (cpClientUtc != null) {
                   String timeOffset = IsoDateJoda.getDifferenceToNow(cpClientUtc.getStringValue());
                   this.remoteProperties.put("__timeOffset", timeOffset);
                }
             }
             catch (Throwable e) {
                e.printStackTrace();
             }
             continue; // Remove, we only want the offset time between client and server
         }
         Object value = map.get(key);
         this.remoteProperties.put(key, (ClientProperty)value);
      }
   }

   /**
    * Add a remote property.
    * Usually this is done by a publish of a client, but for
    * testing reasons we can to it here manually.
    * If the key exists, its value is overwritten
    * @param key The unique key (no multimap)
    * @param value The value, it is assumed to be of type "String"
    * @return The old ClientProperty if existed, else null
    * @see <a href="http://www.xmlBlaster.org/xmlBlaster/doc/requirements/admin.events.html">The admin.events requirement</a>
    */
   public synchronized ClientProperty addRemoteProperty(String key, String value) {
      if (this.remoteProperties == null)
         this.remoteProperties = new ClientPropertiesInfo(null);
      ClientProperty old = (ClientProperty)this.remoteProperties.getClientPropertyMap().get(key);
      this.remoteProperties.put(key, value);
      return old;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#isStalled()
 */
@Override
public boolean isStalled() {
      return false;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#lostClientConnection()
 */
   @Override
public void lostClientConnection() {
      log.debug(ME+": Protocol layer is notifying me about a lost connection");
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getTransportConnectFail()
 */
   @Override
public ServiceManagerException getTransportConnectFail() {
      return this.transportConnectFail;
   }

   /**
    * @param transportConnectFail the transportConnectFail to set
    */
   public void setTransportConnectFail(ServiceManagerException transportConnectFail) {
      this.transportConnectFail = transportConnectFail;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getAuthorizationCache()
 */
   @Override
public Object getAuthorizationCache() {
      return authorizationCache;
   }

   public void setAuthorizationCache(Object authorizationCache) {
      this.authorizationCache = authorizationCache;
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#isBlockClientSessionLogin()
 */
@Override
public boolean isBlockClientSessionLogin() {
      return blockClientSessionLogin;
   }

   public String setBlockClientSessionLogin(boolean blockClient) {
      if (this.blockClientSessionLogin == blockClient)
         return "Session " + getId() + " is alread in state blocking=" + blockClient;
      this.blockClientSessionLogin = blockClient;
      String text = blockClient ? "The ALIVE client remains logged in, reconnects are blocked" : "Blocking of "
            + getId() + " is switched off";
      log.info(text);
      return text;
   }
   
   public String disconnectClientKeepSession() {
      String text = "Client " + getId() + " is disconnected";
      log.info(text);
      return text;
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getUserObject(java.lang.String, java.lang.Object)
 */
   @Override
public Object getUserObject(String key, Object defaultValue) {
      Object obj = this.userMap.get(key);
      if (obj == null) {
         return defaultValue;
      }
      return obj;
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#hasUserObject(java.lang.String)
 */
@Override
public boolean hasUserObject(String key) {
      return this.userMap.containsKey(key);
   }
   /**
    * The key should use a prefix to not collide with other users / plugins. 
    * @param key
    * @param value
    * @return the previous or null
    */
   public Object setUserObject(String key, Object value) {
      Object obj = this.userMap.put(key, value);
      return obj;
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getUserObjectMap()
 */
   @Override
public Map<String, Object> getUserObjectMap() {
      return this.userMap;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#killSession()
 */
@Override
public final String killSession() throws ServiceManagerException {
	   glob.getAuthenticate().disconnect(securityCtx.getSecretSessionId());
	   return getId() + " killed";

   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.session.I_SessionInfo#getConnectionState()
 */
@Override
public String getConnectionState() {
	   return "UNDEF";
   }
}

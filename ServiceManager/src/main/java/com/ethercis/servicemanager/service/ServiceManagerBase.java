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
 * @author W. Kleinertz (wkl) H. Goetzger
 * @author <a href="mailto:Konrad.Krafft@doubleslash.de">Konrad Krafft</a>
 * @author <a href="mailto:xmlBlaster@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.ServiceConfig;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
/**
 * Base class to load Services.
 */
public class ServiceManagerBase implements I_ServiceManager {

   private static String ME = "ServiceManagerBase";
   private Hashtable ServiceCache; // currently loaded Services  (REMOVE???)
   protected final RunTimeSingleton glob;
   private static Logger log = Logger.getLogger(ServiceManagerBase.class);
   public final static String NO_Service_TYPE = "undef";

   public ServiceManagerBase(RunTimeSingleton glob) {
      this.glob = glob;

   }

   protected RunTimeSingleton getRunTimeSingleton() {
      return this.glob;
   }

   /**
    * @param typeVersion and version with comma separator e.g. "RMI,1.0"
    */
   public I_Service getServiceObject(String typeVersion) throws ServiceManagerException {
      if (typeVersion == null)
         return null;
      String type_;
      String version_;
      int i = typeVersion.indexOf(',');
      if (i==-1) {  // version is optional
         version_ = null;
         type_ = typeVersion;
      }
      else {
         version_ = typeVersion.substring(i+1);
         type_ = typeVersion.substring(0,i);
      }
      return getServiceObject(type_, version_);
   }

   /**
    * Return a specific Service, if one is loaded already it is taken from cache. 
    * <p/>
    * This code is thread save.
    * @param type The type of the requested Service.
    * @param version The version of the requested Service.
    * @return I_Service The Service which is suitable to handle the request or null if type=="undef"
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown if no suitable Service has been found.
    */
   public I_Service getServiceObject(String type, String version) throws ServiceManagerException {
      ServiceInfo ServiceInfo = new ServiceInfo(glob, this, type, version);
      return getServiceObject(ServiceInfo);
   }

   /**
    * Return a specific Service, if one is loaded already it is taken from cache. 
    * <p/>
    * This code is thread save.
    * @param serviceInfo the service parameters
    * @return I_Service The Service which is suitable to handle the request or null if type=="undef"
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown if no suitable Service has been found.
    */
   public I_Service getServiceObject(ServiceInfo serviceInfo) throws ServiceManagerException {
      log.debug("Loading Service " + serviceInfo.toString());
      I_Service plug = null;

      if (serviceInfo.ignoreService()) return null;

      synchronized (this) {
         // check in hash if Service is instantiated already
         plug = this.glob.getServiceRegistry().getService(serviceInfo.getId());
         if (plug!=null) return plug;
         // not in hash, instantiate Service
         plug = instantiateServiceFirstPhase(serviceInfo, true);
      }
      if (plug == null) return null;
      synchronized(plug) {
         return instantiateServiceSecondPhase(plug, serviceInfo);
      }
   }

   public I_Service getFromServiceCache(String id) {
      if (id == null) return null;
      return this.glob.getServiceRegistry().getService(id);
   }

   public I_Service removeFromServiceCache(String id) {
      if (id == null) return null;
      return this.glob.getServiceRegistry().unRegister(id);
   }

   /**
    * Is called after a Service in instantiated, allows the base class to do specific actions.
    * Is NOT called when Service got from cache.
    */
   protected void postInstantiate(I_Service Service, ServiceInfo ServiceInfo) throws ServiceManagerException {
   }

   /**
    * @param type can be null
    * @param version can be null
    * @return please return your default Service classname or null if not specified
    */
   public String getDefaultServiceName(String type, String version) {
      return null;
   }

   /**
    * Tries to return an instance of the default Service.
    */
   public I_Service getDummyService() throws ServiceManagerException {
      return getServiceObject(null, null);
   }

   /**
   * @return The name of the property in services.property, e.g. "Security.Server.Service"
   * for "Security.Server.Service[simple][1.0]"
   */
   protected String getServicePropertyName() {
      return "EhrSOABaseService";
   }

   public String getName() {
      return getServicePropertyName();
   }

   /**
    * @return e.g. "Security.Server.Service[simple][1.0]"
    */
   public final String createServicePropertyKey(String type, String version) {
      StringBuffer buf = new StringBuffer(80);
      buf.append(getServicePropertyName());
      if (type != null)
         buf.append("[").append(type).append("]");
      if (version != null)
         buf.append("[").append(version).append("]");
      return buf.toString();
   }

   /**
    * Create a Service instance <b>without</b> caching it. 
    *
    * @see #instantiateService(ServiceInfo, boolean false)
    */
   protected I_Service instantiateService(ServiceInfo ServiceInfo) throws ServiceManagerException {
      return instantiateService(ServiceInfo, false);
   }

   /**
    * Loads a Service.
    *
    * @param ServiceInfo Contains the Service information
    * @param useServiceCache If true the Service is remembered in our cache and e.g. retrievable with getServiceObject()
    *
    * @return I_Service or null if Service type is set to "undef"
    *
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown if loading or initializing failed.
    */
   protected I_Service instantiateService(ServiceInfo ServiceInfo, boolean useServiceCache) throws ServiceManagerException
   {
      I_Service Service = instantiateServiceFirstPhase(ServiceInfo, useServiceCache);
      if (Service != null) {
         return instantiateServiceSecondPhase(Service, ServiceInfo);
      }
      return null;
   }


   private I_Service instantiateServiceFirstPhase(ServiceInfo ServiceInfo, boolean useServiceCache) 
      throws ServiceManagerException {
      // separate parameter and Service name
      if (ServiceInfo.ignoreService()) return null;

      I_Service Service = null;
      String ServiceName = ServiceInfo.getClassName();
      if (ServiceName == null) {
         log.warn("The Service class name is null, please check the property setting of '" + ServiceInfo.toString() + "' " + RunTimeSingleton.getStackTraceAsString(null));
         throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION, ME,
               "The Service class name is null, please check the property setting of '" + ServiceInfo.toString() + "'");
      }
      try {

    	  Class cl = java.lang.Class.forName(ServiceName);
    	  Service = (I_Service)cl.newInstance();

    	  if (useServiceCache) {
    		  this.glob.getServiceRegistry().register(ServiceInfo.getId(), Service);
    	  }
    	  return Service;
      }

      catch (IllegalAccessException e) {
         log.error("The Service class '" + ServiceName + "' is not accessible\n -> check the Service name and/or the CLASSPATH to the Service: " + e.toString());
         throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION_SERVICEFAILED, ME+".NoClass", "The Service class '" + ServiceName + "' is not accessible\n -> check the Service name and/or the CLASSPATH to the Service", e);
      }
      catch (SecurityException e) {
         log.error("No right to access the Service class or initializer '" + ServiceName + "': " + e.toString());
         throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION_SERVICEFAILED, ME+".NoAccess", "No right to access the Service class or initializer '" + ServiceName + "'", e);
      }
      catch (InstantiationException e) {
         String text = "The Service class or initializer '" + ServiceName + "' is invalid, check if the Service has a default constructor";
         throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION_SERVICEFAILED, ME+".Invalid", text, e);
      }
      catch (Throwable e) {
         String text = "The Service class or initializer '" + ServiceName + "' could not be loaded, exception:"+e;
         log.error(text);
         //e.printStackTrace();
         throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION_SERVICEFAILED, ME, text);
      }
   }

   /**
    * TODO Clean this method since it uses knowledge of the server side
    * @param ServiceInfo
    * @return
    */
   private ServiceInfo checkServiceInfoInRunLevelInfos(ServiceInfo ServiceInfo) throws ServiceManagerException {
      if (this.glob.isServerSide()) {// && this.glob instanceof ServerScope) {
    	  ServiceConfig config = this.glob.getServiceHolder().getServiceConfig(this.glob.getStrippedId(), ServiceInfo.getId());
         if (config == null) // normally it is stored logonservice Type without the version information.
            config = this.glob.getServiceHolder().getServiceConfig(this.glob.getStrippedId(), ServiceInfo.getType());
         if (config == null)
            return ServiceInfo;
         ServiceInfo runLevelServiceInfo = config.getServiceInfo();
         if (runLevelServiceInfo == null || runLevelServiceInfo == ServiceInfo)
            return ServiceInfo;
         if (ServiceInfo.getParameters() == null || ServiceInfo.getParameters().size() < 1)
            return runLevelServiceInfo;
         if (ServiceInfo.getParameters() == runLevelServiceInfo.getParameters())
            return ServiceInfo;
         if (runLevelServiceInfo.getParameters().size() < 1)
            return ServiceInfo;
         ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
         PrintStream ps = new PrintStream(baos);
         ServiceInfo.getParameters().list(ps);
         log.warn("The Service " + ServiceInfo.getClassName() + " '" + ServiceInfo.getId() + "' is configured in the properties and in the services.xml file. I will take the Service attributes/parameters defined in the properties which are: " + Constants.toUtf8String(baos.toByteArray()));
      }
      return ServiceInfo;
   }

   private I_Service instantiateServiceSecondPhase(I_Service Service, ServiceInfo ServiceInfo) throws ServiceManagerException {
      // Initialize the Service
      try {
         ServiceInfo = checkServiceInfoInRunLevelInfos(ServiceInfo);
         Service.init(glob, ServiceInfo);
         postInstantiate(Service, ServiceInfo);
         log.debug("Service '" + ServiceInfo.getId() + " successfully initialized.");
         //log.info(ME, "Service " + ServiceInfo.toString() + "=" + ServiceName + " successfully initialized.");
      } catch (ServiceManagerException e) {
         //log.error(ME, "Initializing of Service " + Service.getType() + " failed:" + e.getMessage());
         e.printStackTrace();
         throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION_SERVICEFAILED, ME+".NoInit", "Initializing of Service " + Service.getType() + " failed:" + e.getMessage());
      }
      return Service;
   }

   /**
    * Service with type=="undef" are ignored
    */
   public final static boolean ignoreService(String typeVersion) {
      if (NO_Service_TYPE.equalsIgnoreCase(typeVersion.trim()) || "undef,1.0".equalsIgnoreCase(typeVersion.trim()))
         return true;
      return false;
   }

   public void shutdown() {
      if (this.ServiceCache != null)
         this.ServiceCache.clear();
   }

}

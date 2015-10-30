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
 * @author <a href="mailto:Konrad.Krafft@doubleslash.de">Konrad Krafft</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.property.PropString;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Holds data about a Service (immutable). 
 *
 */
public class ServiceInfo implements I_ServiceConfig {
   private RunTimeSingleton glob;
   private static Logger log = Logger.getLogger(ServiceInfo.class);
   private String ME;

   /** e.g. "ProtocolService" */ // can be removed ...
   private String propertyName;
   /** e.g. "ProtocolService[IOR][1.0]" */
   private String propertyKey;   // can be removed ...

   /** e.g. "IOR" */
   private String type;
   /** e.g. "1.0" */
   private String version;
   /** e.g. "com.ethercis.protocol.soap.SoapDriver" */
   private String className;
   
   /** The key into params for the classpath */
   public static String KEY_CLASSPATH = "classpath";

   /** key/values from "com.ethercis.protocol.soap.SoapDriver,classpath=xerces.jar:soap.jar,MAXSIZE=100" */
   private Properties params;
   private Object userData;

   public ServiceInfo(RunTimeSingleton glob, String type, String className, Properties params) {
      ME = "ServiceInfo-" + type;
      this.glob = glob;

      log.debug("constructor type='" + type + "' className='" + className + "'");
      this.type = type.trim();
      this.className = className;
      this.params = params;
      this.version = "1.0"; // for the moment. Later remove this
   }

   public String getId() {
      return getTypeVersion();
   }

   /**
    * @param manager can be null if you only want to parse typeVersion
    * @param typeVersion null: Choose default Service ServiceManager.getDefaultServiceName() <br />
    *             "undef": Don't load the Service
    *             else: Load the given Service or throw exception
    *        Example: "SOCKET,1.0" or "RAM,1.0"
    */
   public ServiceInfo(RunTimeSingleton glob, I_ServiceManager manager, String typeVersion) throws ServiceManagerException {
      this(glob, manager, typeVersion, (ContextNode)null);
   }

   /**
    * @param manager can be null if you only want to parse typeVersion
    * @param typeVersion null: Choose default Service ServiceManager.getDefaultServiceName() <br />
    *             "undef": Don't load the Service
    *             else: Load the given Service or throw exception
    *        Example: "SOCKET,1.0" or "RAM,1.0"
    */
   public ServiceInfo(RunTimeSingleton glob, I_ServiceManager manager, String typeVersion, ContextNode contextNode) throws ServiceManagerException {
      if (typeVersion == null) {
         init(glob, manager, (String)null, (String)null, (ContextNode)null);
         return;
      }
      int i = typeVersion.indexOf(',');
      String type_;
      String version_;
      if (i==-1) {  // version is optional
         version_ = null;
         type_ = typeVersion.trim();
      }
      else {
         version_ = typeVersion.substring(i+1);
         type_ = typeVersion.substring(0,i);
      }
      init(glob, manager, type_, version_, contextNode);
   }

   /**
    * From ServiceEnvClass and instanceId we build a string to lookup the key in the environment
    * e.g. "/ehrserver/node/heron/persistence/topicStore/PersistenceService[JDBC][1.0]"
    * @param manager can be null if you only wanted to parse typeVersion
    * @param type null: Choose default Service ServiceManager.getDefaultServiceName() <br />
    *             "undef": Don't load the Service
    *             else: Load the given Service or throw exception
    * @param ServiceEnvClass The classname for environment lookup e.g. "queue" or "persistence"
    * @param instanceId The instance name of the Service e.g. "history" or "topicStore"
    */
   public ServiceInfo(RunTimeSingleton glob, I_ServiceManager manager, String type, String version,
                     ContextNode contextNode) throws ServiceManagerException {
      init(glob, manager, type, version, contextNode);
   }

   /**
    * @param type null: Choose default Service ServiceManager.getDefaultServiceName() <br />
    *             "undef": Don't load the Service
    *             else: Load the given Service or throw exception
    */
   public ServiceInfo(RunTimeSingleton glob, I_ServiceManager manager, String type, String version) throws ServiceManagerException {
      this(glob, manager, type, version, (ContextNode)null);
   }

   /**
    * Use this setUserData() / getUserData() pair to transport some user specific data
    * to postInitialize() if needed
    */
   public void setUserData(Object userData) {
      this.userData = userData;
   }

   public Object getUserData() {
      return this.userData;
   }

   /**
    * see javadoc of constructor
    */
   private void init(RunTimeSingleton glob, I_ServiceManager manager, String type_, String version_,
                     ContextNode contextNode) throws ServiceManagerException {
      this.glob = glob;

      if (type_ == null) {
         log.debug("Service type is null, ignoring Service");
         return;
      }
      this.type = type_.trim();
      this.version = (version_ == null) ? "1.0" : version_.trim();

      if (manager == null) return;

      propertyName = manager.getName();
      ME = "ServiceInfo-"+propertyName;

      if (ignoreService()) {
         log.debug("Service type set to 'undef', ignoring Service");
         return;
      }

      // propertyKey="ProtocolService[IOR][1.0]"
      propertyKey = manager.createServicePropertyKey(type, version);
      
      // Search for e.g. "ProtocolService[IOR][1.0]" or "/ehrserver/node/heron/ProtocolService[IOR][1.0]"
      String defaultClass = "EhrSOABaseService";
      PropString prop = new PropString(defaultClass);
      /*String usedPropertyKey =*/prop.setFromEnv(glob, contextNode, propertyKey);
      
      log.debug("Trying contextNode=" + ((contextNode==null)?"null":contextNode.getRelativeName()) + " propertyKey=" + propertyKey);

      String rawString = prop.getValue();

      if (rawString==null) {
         if (this.type != null) {
            log.debug("Service '" + toString() + "' not found, giving up.");
            throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Service '" + toString() + "' not found, please check your configuration");
         }
         rawString = manager.getDefaultServiceName(this.type, this.version);
      }

      parsePropertyValue(rawString);
   }
   
   /**
    * @param rawString e.g. "com.ethercis.protocol.soap.SoapDriver,classpath=xerces.jar:soap.jar,MAXSIZE=100"
    */
   private void parsePropertyValue(String rawString) throws ServiceManagerException {
      if (rawString==null) throw new IllegalArgumentException(ME + ".parsePropertyValue(null)");

      this.params = new Properties();
      if(rawString!=null) {
         StringTokenizer st = new StringTokenizer(rawString, ",");
         boolean first=true;
         while(st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (first) { // The first is always the class name
               className = tok;
               first = false;
               continue;
            }
            int pos = tok.indexOf("=");
            if (pos < 0) {
               log.info("Accepting param '" + tok + "' without value (missing '=')");
               this.params.put(tok, "");
            }
            else
               this.params.put(tok.substring(0,pos), tok.substring(pos+1));
         }
      }
      else
         throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_CONFIGURATION, ME + ".parsePropertyValue", "Missing Service configuration for property " + propertyKey + ", please check your settings");
   }

   /**
    * Check if the Service is marked with "undef", such configurations are not loaded
    */
   public boolean ignoreService() {
      if ("undef".equalsIgnoreCase(type) || "undef,1.0".equalsIgnoreCase(type))
         return true;
      return false;
   }
   
   /**
    * @return classname
    */
   public String getClassName() {
      return className;
   }

   /**
    * @return The configuration, never null
    */
   public Properties getParameters() {
      if (this.params == null) {
         this.params = new Properties();
      }
      return this.params;
   }

   private String[] getParameterArr() {
      String[] arr = new String[getParameters().size()*2];
      Enumeration e = this.params.keys();
      int i = 0;
      while(e.hasMoreElements()) {
         String key = (String)e.nextElement();
         arr[i++] = key;
         arr[i++] = (String)this.params.get(key);
      }
      return arr;
   }

   public String getType() {
      return type;
   }

   public String getVersion() {
      return version;
   }

   public String getTypeVersion() {
      if (type == null) return null;
      if (version == null) return type;
      return type + "," + version;
   }

   /**
    * Dumps the parameters passed to the Service. So if you defined a property in
    * the property file
    */
   public String dumpServiceParameters() {
      String[] arr = this.getParameterArr();
      StringBuffer buf = new StringBuffer();
      buf.append(this.className);
      if (arr.length > 0) buf.append(',');

      char ch = ',';
      for (int i=0; i< arr.length; i++) {
         buf.append(arr[i]);
         if (ch == ',') ch ='='; else ch = ',';
         if (i !=arr.length-1) buf.append(ch);
      }
      return buf.toString();
   }

   /** @return for example "ProtocolService[IOR][1.0]" */
   public String toString() {
      return (this.propertyKey == null) ? this.propertyName+"["+getType()+"]["+getVersion()+"]" : this.propertyKey;
   }
   
   public String getPrefix() {
      return "Service/" + getType() + "/";
   }
   
}


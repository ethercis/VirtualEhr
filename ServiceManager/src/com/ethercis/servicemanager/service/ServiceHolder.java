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
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.service;


import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.runlevel.RunLevelAction;
import com.ethercis.servicemanager.runlevel.ServiceConfig;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;

/**
 * This class contains the information on how to configure a certain service and when a certain service is invoked by the run level manager
 */
public class ServiceHolder {
   private final RunTimeSingleton glob;
   private static Logger log = Logger.getLogger(ServiceHolder.class);

   private Hashtable<String, ServiceConfig> serviceConfigsDefault;

   /** 
    * This is a double Hashtable: an Hashtable containing one hashtable for every node found. Every one
    * of these node specific Hashtables contains all ServiceConfig objects defined in the corresponding node
    * section.
    */
   private Hashtable<String, Hashtable<String, ServiceConfig>> serviceConfigsNodes;

   /**
    * This constructor takes all parameters needed
    */
   public ServiceHolder(RunTimeSingleton glob, Hashtable<String, ServiceConfig> serviceConfigsDefault, Hashtable<String, Hashtable<String, ServiceConfig>> serviceConfigsNodes) {
      this.glob = glob;

      log.debug("constructor");
      if (serviceConfigsDefault != null )
         this.serviceConfigsDefault = serviceConfigsDefault;
      else this.serviceConfigsDefault = new Hashtable<String, ServiceConfig>();

      if (serviceConfigsNodes != null) this.serviceConfigsNodes = serviceConfigsNodes;
      else this.serviceConfigsNodes = new Hashtable<String, Hashtable<String, ServiceConfig>>();

   }

   /**
    * Minimal constructor
    */
   public ServiceHolder(RunTimeSingleton glob) {
      this(glob, ( Hashtable<String, ServiceConfig>)null, (Hashtable<String, Hashtable<String, ServiceConfig>>)null);
   }

   /**
    * @return The previous one or null
    */
   public ServiceConfig addDefaultServiceConfig(ServiceConfig serviceConfig) {
      return this.serviceConfigsDefault.put(serviceConfig.getId(), serviceConfig);
   }

   /**
    *  Adds a serviceConfig object to the specified node. 
    *  <p>
    *  If node is null it is put to default scope.
    * @param node the node to which to add the serviceConfig object
    * @param serviceConfig the object to add to the holder.
    * @return The previous one or null
    */
   public ServiceConfig addServiceConfig(String node, ServiceConfig serviceConfig) {
      if (node == null) {
         return addDefaultServiceConfig(serviceConfig);
      }
      // check first if the node already exists ...
      log.debug("addServiceConfig for node '" + node + "'");
      Hashtable<String, ServiceConfig> tmp = this.serviceConfigsNodes.get(node);
      if (tmp == null) { // then it does not exist (add one empty table)
         tmp = new Hashtable<String, ServiceConfig>();
         this.serviceConfigsNodes.put(node, tmp);
      }
      return tmp.put(serviceConfig.getId(), serviceConfig);
   }

   /**
    * returns the service specified with the given id and the given node. If a
    * service configuration is not found in the specified node, then it is
    * searched in the defaults. If none is found there either, then a null is
    * returned.
    * @param node the nodeId scope on which to do the request or null
    * @param id the unique string identifying the service
    */
   public ServiceConfig getServiceConfig(String node, String id) {
      log.debug("id '" + id + "', node '" + node + "'");
      Hashtable<String, ServiceConfig> nodeTable = (node==null) ? null : this.serviceConfigsNodes.get(node);
      if (nodeTable != null) {
         Object tmp = nodeTable.get(id);
         if (tmp != null) return (ServiceConfig)tmp;
      }
      return this.serviceConfigsDefault.get(id);
   }


   /**
    * Remove the given serviceConfig instance. 
    * @param node
    * @param id
    * @return Can be null if not found
    */
   public ServiceConfig removeServiceConfig(String node, String id) {
      log.debug("id '" + id + "', node '" + node + "'");
      Hashtable<String, ServiceConfig> nodeTable = (node==null) ? null : this.serviceConfigsNodes.get(node);
      if (nodeTable != null) {
         ServiceConfig oldConfig = (ServiceConfig)nodeTable.remove(id);
         if (oldConfig != null) {
            if (nodeTable.size() == 0)
               this.serviceConfigsDefault.remove(node);
            oldConfig.shutdown();
            return oldConfig;
         }
      }
      ServiceConfig oldConfig = this.serviceConfigsDefault.remove(id);
      if (oldConfig != null)
         oldConfig.shutdown();
      return oldConfig;
   }

   /**
    * returns all ServiceConfig found for the specified node (and the default)
    * @param node the node for which to search.
    */
   public ServiceConfig[] getAllServiceConfig(String node) {
      log.debug("getAllServiceConfig for node '" + node + "'");
      Hashtable<String, ServiceConfig> tmp = (Hashtable<String, ServiceConfig>)this.serviceConfigsDefault.clone();
      if (this.serviceConfigsNodes!=null && node!=null) {
         Hashtable<String, ServiceConfig> nodeTable = this.serviceConfigsNodes.get(node);
         if (nodeTable != null) {
            Enumeration<String> enumer = nodeTable.keys();
            while (enumer.hasMoreElements()) {
               String key = (String)enumer.nextElement();
               //log.dummy("-------------key " + key + ":" +nodeTable.get(key));
               tmp.put(key, nodeTable.get(key));
            }
         }
      }
      else {
         log.warn("No cluster node id given, checking only default services");
      }
      // prepare the return array ...
      int size = tmp.size();
      ServiceConfig[] ret = new ServiceConfig[size];
      int i = 0;
      Enumeration<String> enumer = tmp.keys();
      while (enumer.hasMoreElements()) {
         String key = enumer.nextElement();
         ret[i] = (ServiceConfig)tmp.get(key);
         i++;
      }
      return ret;
   }


   /**
    * returns an xml litteral string representing all entries found in the configuration file.
    */
   public String toXml(String extraOffset) {
      StringBuffer sb = new StringBuffer(512);
      if (extraOffset == null) extraOffset = "";
      String offset = Constants.OFFSET + extraOffset;

      sb.append(offset).append("<services>");
      // append all defaults ...
      Enumeration<String> enumer = this.serviceConfigsDefault.keys();
      while (enumer.hasMoreElements()) {
         String key = enumer.nextElement();
         ServiceConfig serviceConfig = this.serviceConfigsDefault.get(key);
         sb.append(serviceConfig.toXml(extraOffset + "   "));
      }

      enumer = this.serviceConfigsNodes.keys();
      while (enumer.hasMoreElements()) {
         String nodeId = enumer.nextElement();
         Hashtable<?, ?> nodeTable = this.serviceConfigsNodes.get(nodeId);
         sb.append(offset).append("   ").append("<node id='").append(nodeId).append("'>");
         Enumeration<?> enumNodes = nodeTable.keys();
         while (enumNodes.hasMoreElements()) {
            String key = (String)enumNodes.nextElement();
           ServiceConfig serviceConfig = (ServiceConfig)nodeTable.get(key);
            sb.append(serviceConfig.toXml(extraOffset + "      "));
         }
         sb.append(offset).append("   ").append("</node>");
      }

      sb.append(offset).append("</services>");
      return sb.toString();
   }

   public String toXml() {
      return toXml("");
   }


   /** 
    * Returns a hashset containing all services which have a startup level defined.
    * The returns are already in the right sequence.
    * @param nodeId the id of the node to retrieve
    * @param lowRunlevel the runlevel from which to start retreive (inclusive)
    * @param highRunlevel the runlevel to which to retrieve (inclusive)
    */
   public TreeSet<ServiceConfig> getStartupSequence(String nodeId, int lowRunlevel, int highRunlevel) {
      log.debug("getStartupSequence for node '" + nodeId + 
                         "' and runlevel '" + lowRunlevel + "' to '" + highRunlevel + "'");
      if (lowRunlevel > highRunlevel) {
         log.error(".getStartupSequence: the low run level '" + lowRunlevel + "' is higher than the high run level '" + highRunlevel + "'");
      }
      TreeSet<ServiceConfig> startupSet = new TreeSet<ServiceConfig>(new ServiceConfigComparator(this.glob, true));
      ServiceConfig[] services = getAllServiceConfig(nodeId);
      for (int i=0; i < services.length; i++) {
         RunLevelAction action = services[i].getUpAction();
         if (action != null) {
            int runlevel = action.getOnStartupRunlevel();
            if (runlevel >= lowRunlevel && runlevel <= highRunlevel)
               startupSet.add(services[i]);
         }
      }
      return startupSet;
   }
   
   /** 
    * Returns a hashset containing all services which have a shutdown level defined.
    * The returns are already in the right sequence.
    * @param nodeId the id of the node to retrieve
    * @param lowRunlevel the runlevel from which to start retreive (inclusive)
    * @param highRunlevel the runlevel to which to retrieve (inclusive)
    */
   public TreeSet<ServiceConfig> getShutdownSequence(String nodeId, int lowRunlevel, int highRunlevel) {
      log.debug("getShutdownSequence for node '" + nodeId + 
                        "' and runlevel '" + lowRunlevel + "' to '" + highRunlevel + "'");
      if (lowRunlevel > highRunlevel) {
         log.error(".getShutdownSequence: the low run level '" + lowRunlevel + "' is higher than the high run level '" + highRunlevel + "'");
      }
      TreeSet<ServiceConfig> shutdownSet = new TreeSet<ServiceConfig>(new ServiceConfigComparator(this.glob, false));
      ServiceConfig[] services = getAllServiceConfig(nodeId);
      for (int i=0; i < services.length; i++) {
         RunLevelAction action = services[i].getDownAction();
         if (action != null) {
            int runlevel = action.getOnShutdownRunlevel();
            if (runlevel >= lowRunlevel && runlevel <= highRunlevel)
               shutdownSet.add(services[i]);
         }
      }
      return shutdownSet;
   }

}

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


package com.ethercis.servicemanager.cluster;

import java.util.Set;

/**
 * Hides configuration parameters and passes common objects.
 */
public interface I_Info {
   public final static String JMX_PREFIX = "__JMX__";
   /** 
    * This is the key used to identify this instance of the info object. It can be set
    * explicitly or it can be set by the application. For example Implementations such logonservice
    * the ClusterInfo set it per default to be the id of the Service (which is the one
    * returned by the ServiceInfo.getType()
    */
   public final static String ID = "id";
   
   /**
    * Returns the value associated to this key.
    * @param key
    * @return
    */
   String getRaw(String key);
   
   /**
    * Access a string environment setting. 
    * @param key The parameter key
    * @param def The default used if key is not found
    * @return The configured value of the parameter
    */
   String get(String key, String def);
   
   /**
    * Put key/value to environment. This put does not modify (replace) the key, nor the value.  
    * @param key The parameter key
    * @param value The parameter value, if null the parameter is removed.
    * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>.
    */
   void putRaw(String key, String value);
        
   /**
    * Put key/value to environment.  
    * @param key The parameter key
    * @param value The parameter value, if null the parameter is removed.
    * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>.
    */
   void put(String key, String value);
        
   /**
    * Access an environment setting of type long. 
    * @param key The parameter key
    * @param def The default used if key is not found
    * @return The configured value of the parameter
    */
   long getLong(String key, long def);
        
   /**
    * Access an environment setting of type int. 
    * @param key The parameter key
    * @param def The default used if key is not found
    * @return The configured value of the parameter
    */
   int getInt(String key, int def);

   /**
    * Access an environment setting of type boolean. 
    * @param key The parameter key
    * @param def The default used if key is not found
    * @return The configured value of the parameter
    */
   boolean getBoolean(String key, boolean def);

   /**
    * Store an object.  
    * @param key The object key
    * @param o The object to remember or null to remove it
    * @return The old object or null
    */
   Object putObject(String key, Object o);

   /**
    * Access the remembered object.  
    * @param key The object key
    * @return The found object or null
    */
   Object getObject(String key);
   
   /**
    * Gets the keys of the entries stored. Note that this does not return the
    * key of the entries stored logonservice objects. To retrieve these use getObjectKeys().
    * @return
    */
   Set getKeys();
   
   /**
    * Gets the keys of the objects registered. Note that this does not return the
    * key of the normal entries. To retrieve these use getKeys().
    * @return
    */
   Set getObjectKeys();
   
}

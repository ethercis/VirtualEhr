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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * 
 * PropertiesInfo This is the I_Info implementation making use of Properties.
 */
public class PropertiesInfo implements I_Info {
        
   Properties props;
   Map objects;
   
   private InfoHelper helper;
   
   
   /**
    * Creates a simple implementation based on java's Properties.
    * This implementation uses the reference to the properties passed. If you want a snapshot of these properties, you 
    * need to take a clone and pass the clone to the constructor.
    * 
    * @param props The configuration store
    */
   public PropertiesInfo(Properties props) {
      this.props = props;
      this.objects = new HashMap();
      this.helper = new InfoHelper(this);
      this.helper.replaceAllEntries(this, null);
   }
   
   public String getRaw(String key) {
      return this.props.getProperty(key);
   }
   
   /**
   * get a property value with variable substitution
   */
   public String get(String key, String def) {
      if (def != null)
         def = this.helper.replace(def);
      if (key == null)
         return def;
      key = this.helper.replace(key);
      String ret = getRaw(key);
      if (ret != null) {
         return this.helper.replace(ret);
      }
      return def;
   }

   /**
    * put a property without substitution
    */
    public void putRaw(String key, String value) {
       if (value == null)
         this.props.remove(key);
       else
          this.props.put(key, value);
    }

    /**
    * put a property with substitution
     */
     public void put(String key, String value) {
        if (key != null)
           key = this.helper.replace(key);
        if (value != null)
           value = this.helper.replace(value);
        if (value == null)
          this.props.remove(key);
        else
           this.props.put(key, value);
     }

   /**
   * get a property logonservice long
   */
   public long getLong(String key, long def) {
      if (key == null)
         return def;
      String ret = get(key, null);
      if (ret != null) {
         try {
            return Long.parseLong(ret);
         }
         catch (NumberFormatException ex) {
            ex.printStackTrace();
            return def;
         }
      }
      return def;
   }

   /**
   * get a property logonservice int
   */
   public int getInt(String key, int def) {
      if (key == null)
         return def;
      String ret = get(key, null);
      if (ret != null) {
         try {
            return Integer.parseInt(ret);
         }
         catch (NumberFormatException ex) {
            ex.printStackTrace();
            return def;
         }
      }
      return def;
   }

   /**
    * get a property logonservice boolean
    */
    public boolean getBoolean(String key, boolean def) {
       if (key == null)
          return def;
       String ret = get(key, null);
       if (ret != null) {
          try {
             Boolean bool = new Boolean(ret);
             return bool.booleanValue();
          }
          catch (NumberFormatException ex) {
             ex.printStackTrace();
             return def;
          }
       }
       return def;
    }

   /**
   * get an object
   */
   public Object getObject(String key) {
      return this.objects.get(key);
   }

   /**
   * put an object
   */
   public Object putObject(String key, Object o) {
      if (o == null)
         return this.objects.remove(key);
      return this.objects.put(key, o);
   }
   
   /**
    * get the key set of properties
    */
   public Set getKeys() {
      return this.props.keySet();
   }

   /**
    * get the key set of objects
    */
   public Set getObjectKeys() {
      return this.objects.keySet();
   }
   
   /**
    * add a set to a set...
    * @param dest
    * @param source
    */
   public static void addSet(Set dest, Set source) {
      if (dest == null || source == null)
         return;
      Iterator iter = source.iterator();
      while (iter.hasNext()) {
         String key = (String)iter.next();
         dest.add(key);
      }
      
   }
   
}

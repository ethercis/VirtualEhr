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


package com.ethercis.servicemanager.common;

import com.ethercis.servicemanager.cluster.I_Info;
import com.ethercis.servicemanager.cluster.InfoHelper;

import java.util.*;
import java.util.Map.Entry;

/**
 * ClientPropertiesInfo This is the I_Info implementation making use of Properties.
 * Creates a simple implementation based on our ClientProperty maps.
 * This implementation uses the reference to the properties passed. If you want a snapshot of these properties, you 
 * need to take a clone and pass the clone to the constructor.
 * Therefore this class can be seen logonservice a decorator to the map passed
 * into the constructor. If you change a value with this class it will
 * update the clientPropertyMap. If entries in the map are found which
 * are not of the type ClientProperty, they are ignored.
 * 
 * This class is thread safe.
 * 
 */
public class ClientPropertiesInfo implements I_Info {
        
   Map<String, ClientProperty> clientPropertyMap;
   Map<String, Object> objects;
   private InfoHelper helper;
   
   /**
    * @param clientPropertyMap Can be null
    */
   public ClientPropertiesInfo(Map<String, ClientProperty> clientPropertyMap) {
      this(clientPropertyMap, null);
   }   
   
   /**
    * @param clientPropertyMap Can be null
    * @param extraInfo Can be null
    */
   public ClientPropertiesInfo(Map<String, ClientProperty> clientPropertyMap, I_Info extraInfo) {
      this.helper = new InfoHelper(this);
      this.clientPropertyMap = clientPropertyMap;
      if (this.clientPropertyMap == null)
         this.clientPropertyMap = new HashMap<String, ClientProperty>();
      this.objects = new HashMap<String, Object>();
      
      if (extraInfo != null) {
         synchronized (extraInfo) {
            Iterator<?> iter = extraInfo.getKeys().iterator();
            while (iter.hasNext()) {
               String key = (String)iter.next();
               String obj = extraInfo.get(key, null);
               if (obj != null)
                  put(key, obj);
            }
         }
      }
      this.helper.replaceAllEntries(this, null);
   }
   
   /**
    *
    * @return
    */
   public String getRaw(String key) {
      Object obj = this.clientPropertyMap.get(key);
      if (obj == null)
         return null;
      if (!(obj instanceof ClientProperty))
         return null;
      
      ClientProperty prop = (ClientProperty)obj;
      return prop.getStringValue();
   }
   
   
   /**
    *
    * @return
    */
   protected String getPropAsString(String key) {
      Object obj = this.clientPropertyMap.get(key);
      if (obj == null)
         return null;
      if (!(obj instanceof ClientProperty))
         return null;
      
      ClientProperty prop = (ClientProperty)obj;
      String ret = prop.getStringValue();
      if (ret != null) {
         return this.helper.replace(ret);
      }
      return null;
   }
   
   /**
    * @param key
    * @return null if not of type ClientProperty or of not found
    */
   protected ClientProperty getClientProperty(String key) {
      Object obj = this.clientPropertyMap.get(key);
      if (obj == null)
         return null;
      if (!(obj instanceof ClientProperty))
         return null;
      
      return (ClientProperty)obj;
   }

   /**
    */
   public synchronized String get(String key, String def) {
      if (def != null)
         def = this.helper.replace(def);
      if (key == null)
         return def;
      key = this.helper.replace(key);
      String ret = getPropAsString(key);
      if (ret != null) {
         return this.helper.replace(ret);
      }
      return def;
   }

   /**
    */
    public synchronized void put(String key, String value) {
       if (key != null)
          key = this.helper.replace(key);
       if (value != null)
          value = this.helper.replace(value);
       if (value == null)
         this.clientPropertyMap.remove(key);
       else {
          ClientProperty prop = new ClientProperty(key, null, null, value);
          this.clientPropertyMap.put(key, prop);
       }
    }

    /**
     */
     public synchronized void putRaw(String key, String value) {
        if (value == null)
          this.clientPropertyMap.remove(key);
        else {
           ClientProperty prop = new ClientProperty(key, null, null, value);
           this.clientPropertyMap.put(key, prop);
        }
     }

    /**
     */
    public synchronized void put(String key, ClientProperty value) {
       if (value == null)
         this.clientPropertyMap.remove(key);
       else {
          this.clientPropertyMap.put(value.getName(), value);
       }
    }

   /**
   */
   public synchronized long getLong(String key, long def) {
      if (key == null)
         return def;
      String ret = this.getPropAsString(key);
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
   */
   public synchronized int getInt(String key, int def) {
      if (key == null)
         return def;
      String ret = this.getPropAsString(key);
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
    */
    public synchronized boolean getBoolean(String key, boolean def) {
       if (key == null)
          return def;
       String ret = this.getPropAsString(key);
       if (ret != null) {
          try {
             Boolean bool = Boolean.valueOf(ret);
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
   */
   public synchronized Object getObject(String key) {
      return this.objects.get(key);
   }

   /**
   */
   public synchronized Object putObject(String key, Object o) {
      if (o == null)
         return this.objects.remove(key);
      return this.objects.put(key, o);
   }

   /**
    */
   public synchronized Set<String> getKeys() {
      Set<String> set = new HashSet<String>();
      Iterator<Entry<String, ClientProperty>> iter = this.clientPropertyMap.entrySet().iterator();
      while (iter.hasNext()) {
         Map.Entry<String, ClientProperty> entry = (Map.Entry<String, ClientProperty>)iter.next();
         if (entry.getValue() instanceof ClientProperty)
            set.add(entry.getKey());
      }
      return set;
   }

   /**
    */
   public synchronized Set<String> getObjectKeys() {
      return this.objects.keySet();
   }

   /**
    * @return Never null
    */
   public Map<String, ClientProperty> getClientPropertyMap() {
      return clientPropertyMap;
   }
   
   
   public Map<String, String> clientProps2StringMap(ClientPropertiesInfo clientprops){
	   Map<String, String> retmap = new HashMap<String, String>();
	   
	   Map<String, ClientProperty>navmap = clientprops.getClientPropertyMap();
	   for (String k: navmap.keySet()){
		   if (navmap.get(k)==null)
			   retmap.put(k, "");
		   else
			   retmap.put(k, navmap.get(k).getStringValue());
	   }
	   return retmap;
   }
   /**
    * @return Never null
    */
   public ClientProperty[] getClientPropertyArr() {
      if (this.clientPropertyMap.size() == 0) return new ClientProperty[0]; 
      return (ClientProperty[])this.clientPropertyMap.values().toArray(new ClientProperty[this.clientPropertyMap.size()]);
   }
}

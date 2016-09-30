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


package com.ethercis.servicemanager.jmx;


import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Definition of a dynamic MBean which exports the ClusterController properties. 
 *
 * The "JmxProperties" dynamic MBean shows how to expose for management
 * attributes and operations, at runtime,  by implementing the  
 * "javax.management.DynamicMBean" interface.
 *
 * This MBean exposes for management all glob.getProperty() key/values.
 *      - the read/write attribute,
 *      - the read only attribute,
 *      - the "set()" operation.
 * It does so by putting this information in an MBeanInfo object that
 * is returned by the getMBeanInfo() method of the DynamicMBean interface.
 *
 * It implements the access to its attributes through the getAttribute(),
 * getAttributes(), setAttribute(), and setAttributes() methods of the
 * DynamicMBean interface.
 *
 * It implements the invocation of its set() operation through the
 * invoke() method of the DynamicMBean interface.
 * 
 * Note that logonservice "JmxProperties" explicitly defines one constructor,
 * this constructor must be public and exposed for management through
 * the MBeanInfo object.
 * @since 1.0.4
 */
public class JmxProperties implements DynamicMBean {
   private RunTimeSingleton glob;
   private static Logger log = LogManager.getLogger(JmxProperties.class);
   private final String ME = "JmxProperties";
   private String dClassName = this.getClass().getName();
   private MBeanAttributeInfo[] dAttributes;
   private MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[1];
   private MBeanInfo dMBeanInfo = null;
   private int numProperties;

   /**
    * Export all properties from glob. 
    */
   public JmxProperties() {
      this(RunTimeSingleton.instance());
      log.debug("Default constructor");
   }

   /**
    * Export all properties from glob. 
    */
   public JmxProperties(RunTimeSingleton glob) {
      this.glob = glob;

      buildDynamicMBeanInfo();
      log.debug("Constructor created");
   }

   /**
    * Allows the value of the specified attribute of the Dynamic MBean to be obtained.
    */
   public Object getAttribute(String attribute_name) 
                             throws AttributeNotFoundException,
                                    MBeanException,
                                    ReflectionException {
      if (attribute_name == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null"), 
                                                "Cannot invoke a getter of " + dClassName + " with null attribute name");
      }
      attribute_name = RunTimeSingleton.decode(attribute_name, "US-ASCII"); // HtmlAdapter made from info/admin -> info%2Fadmin
      String value = this.glob.getProperty().get(attribute_name, (String)null);
      if (value != null) {
         return value;
      }
      throw(new AttributeNotFoundException("Cannot find " + attribute_name + " attribute in " + dClassName));
   }

   /**
    * Sets the value of the specified attribute of the Dynamic MBean.
    */
   public void setAttribute(Attribute attribute) 
                         throws AttributeNotFoundException,
                                InvalidAttributeValueException,
                                MBeanException, 
                                ReflectionException {
      if (attribute == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("Attribute cannot be null"), 
                                             "Cannot invoke a setter of " + dClassName + " with null attribute");
      }
      String name = attribute.getName();
      if (name == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null"), 
                                             "Cannot invoke the setter of " + dClassName + " with null attribute name");
      }
      name = RunTimeSingleton.decode(name, "US-ASCII"); // HtmlAdapter made from info/admin -> info%2Fadmin

      if (isReadOnly(name)) {
         throw(new AttributeNotFoundException("Cannot set attribute "+ name +" because it is read-only"));
      }

      Object value = attribute.getValue();
      try {
         if (value == null) {
            this.glob.getProperty().set(name, null);
         }
         else {
            this.glob.getProperty().set(name, ""+value);
         }
      }
      catch (Exception e) {
         throw(new AttributeNotFoundException("Cannot set attribute "+ name +":" + e.toString()));
      }
   }

   private boolean isReadOnly(String name) {
      return System.getProperty(name) != null;
      /*
      return name.startsWith("java.") ||
             name.startsWith("com.sun") ||
             name.startsWith("user.") ||
             name.startsWith("file.");
      */
   }

   /**
    * Enables the to get the values of several attributes of the Dynamic MBean.
    */
   public AttributeList getAttributes(String[] attributeNames) {
      if (attributeNames == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("attributeNames[] cannot be null"),
                                             "Cannot invoke a getter of " + dClassName);
      }
      AttributeList resultList = new AttributeList();

      if (attributeNames.length == 0)
         return resultList;
      
      for (int i=0 ; i<attributeNames.length ; i++){
         try {        
            Object value = getAttribute(attributeNames[i]);     
            resultList.add(new Attribute(attributeNames[i],value));
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return resultList;
   }

   /**
    * Sets the values of several attributes of the Dynamic MBean, and returns the
    * list of attributes that have been set.
    */
   public AttributeList setAttributes(AttributeList attributes) {
      if (attributes == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("AttributeList attributes cannot be null"),
                                             "Cannot invoke a setter of " + dClassName);
      }
      AttributeList resultList = new AttributeList();

      if (attributes.isEmpty())
         return resultList;

      for (Iterator i = attributes.iterator(); i.hasNext();) {
         Attribute attr = (Attribute) i.next();
         try {
            setAttribute(attr);
            String name = attr.getName();
            Object value = getAttribute(name); 
            resultList.add(new Attribute(name,value));
         } catch(Exception e) {
            e.printStackTrace();
         }
      }
      return resultList;
   }

   /**
    * Allows an operation to be invoked on the Dynamic MBean.
    */
   public Object invoke(String operationName, Object params[], String signature[])
      throws MBeanException,
            ReflectionException {
      if (operationName == null) {
         throw new RuntimeOperationsException(new IllegalArgumentException("Operation name cannot be null"), 
                                             "Cannot invoke a null operation in " + dClassName);
      }
      // Check for a recognized operation name and call the corresponding operation
      if (operationName.equals("set")){
         if (params.length == 2) {
            String key = (String)params[0];
            String value = (String)params[1];
            try {
               String retVal = this.glob.getProperty().set(key, value);
               String ret = "Operation set(key="+key+",value="+value+") returned '" + retVal + "'";
               log.info(ret);
               return ret;
            }
            catch (Exception e) {
               throw new RuntimeOperationsException(new IllegalArgumentException("Operation set(key="+key+",value="+value+") failed: " + e.toString()), 
                                             "Cannot invoke a set(key,value) operation in " + dClassName);
            }
         }
         else {
            throw new RuntimeOperationsException(new IllegalArgumentException("Operation set(key,value) expects two parameters"), 
                                             "Cannot invoke a set(key,value) operation in " + dClassName);
         }
      } else { 
         // unrecognized operation name:
         throw new ReflectionException(new NoSuchMethodException(operationName), 
                                       "Cannot find the operation " + operationName + " in " + dClassName);
      }
   }

   /**
    * This method provides the exposed attributes and operations of the Dynamic MBean.
    * It provides this information using an MBeanInfo object.
    */
   public MBeanInfo getMBeanInfo() {
       // return the information we want to expose for management:
       // the dMBeanInfo private field has been built at instanciation time,
       log.debug("Access MBeanInfo");
       buildDynamicMBeanInfo();
       return dMBeanInfo;
   }

   /**
    * Build the private dMBeanInfo field,
    * which represents the management interface exposed by the MBean;
    * that is, the set of attributes, constructors, operations and notifications
    * which are available for management. 
    *
    * A reference to the dMBeanInfo object is returned by the getMBeanInfo() method
    * of the DynamicMBean interface. Note that, once constructed, an MBeanInfo object is immutable.
    */
   private void buildDynamicMBeanInfo() {
      java.util.Properties props = glob.getProperty().getProperties();
      if (this.numProperties == props.size()) {
         return; // no change -> no need to refresh meta informations
      }
      this.numProperties = props.size();

      boolean isReadable = true;
      boolean isIs = false; // true if we use "is" getter

      ArrayList tmp = new ArrayList(props.size());

      for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
         String name = (String) e.nextElement();
         boolean isWritable = !isReadOnly(name);
         tmp.add(new MBeanAttributeInfo(name,
                                    "java.lang.String",
                                    name+" string.",
                                    isReadable,
                                    isWritable,
                                    isIs));
      }

      dAttributes = (MBeanAttributeInfo[])tmp.toArray(new MBeanAttributeInfo[tmp.size()]);

      Constructor[] constructors = this.getClass().getConstructors();
      dConstructors[0] = new MBeanConstructorInfo("JmxProperties(): Constructs a JmxProperties object",
                                                    constructors[0]);

      MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];

      MBeanParameterInfo[] params = 
               new MBeanParameterInfo[] { (new MBeanParameterInfo("key",
                                           "java.lang.String",
                                           "property key") ),
                                          (new MBeanParameterInfo("value",
                                           "java.lang.String",
                                           "new property value") )
                                         } ;
      dOperations[0] = new MBeanOperationInfo("set",
                                             "set(): set a new key/value property, returns the previous value if any",
                                             params , 
                                             "java.lang.String", 
                                             MBeanOperationInfo.ACTION);

      dMBeanInfo = new MBeanInfo(dClassName,
                                 "Exposing the ClusterController properties environment.",
                                 dAttributes,
                                 dConstructors,
                                 dOperations,
                                 new MBeanNotificationInfo[0]);
      log.debug("Created MBeanInfo with " + tmp.size() + " attributes");
   }
}

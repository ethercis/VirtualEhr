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
package com.ethercis.servicemanager.common.property;

/**
 * Base class for the various property data type implementations. 
 * @author xmlBlaster@marcelruff.info
 */
public final class PropString extends PropEntry implements java.io.Serializable, Cloneable
{
   private static final long serialVersionUID = 1L;
   private String valueDefault; // Remember default setting for usage response etc.
   private String value;

   /**
    * Constructor for the default value
    */
   public PropString(String value) {
      super(null);
      this.valueDefault = value;
      this.value = value;
   }

   /**
    * Constructor for the default value
    */
   public PropString(String propName, String value) {
      super(propName);
      this.value = value;
   }

   /**
    * @return "String"
    */
   public final String getTypeString() {
      return "String";
   }

   /**
    * @return The value in String form, null is supported
    */
   public final String getValueString() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
      super.creationOrigin = CREATED_BY_SETTER;
   }

   /**
    * @param creationOrigin e.g. PropEntry.CREATED_BY_JVMENV
    */
   public void setValue(String value, int creationOrigin) {
      this.value = value;
      super.creationOrigin = creationOrigin;
   }

   public String getValue() {
      return this.value;
   }

   /**
    * Overwrite the default value given to the constructor. 
    */
   public void setDefaultValue(String value) {
      this.valueDefault = value;
      if (CREATED_BY_DEFAULT == super.creationOrigin) {
         this.value = value; // overwrite the default setting
      }
   }

   public String getDefaultValue() {
      return this.valueDefault;
   }

   /**
    * Returns a shallow clone, you can change savely all basic or immutable types
    * like boolean, String, int.
    * Currently RouteInfo is not cloned (so don't change it)
    */
   public Object clone() {
      return super.clone();
   }

}

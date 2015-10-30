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

import java.io.NotSerializableException;

/**
 * The immutable event object when a property was created or has changed. 
 * @author Marcel Ruff
 * @see testsuite.TestProperty
 */
public class PropertyChangeEvent extends java.util.EventObject
{
   private static final long serialVersionUID = 1L;
   private String key;
   private String oldValue;
   private String newValue;

   /**
    * Constructs a new <code>PropertyChangeEvent</code> instance.
    *
    * @param key     The property key
    * @param oldValue The old value
    * @param newValue The new or changed value
    */
   public PropertyChangeEvent(String key, String oldValue, String newValue) {
       super(key);
       this.key = key;
       this.oldValue = oldValue;
       this.newValue = newValue;
   }

   /**
    * The unique key of the property
    */
   public String getKey() {
      return this.key;
   }

   /**
    * The previous value of this property
    */
   public String getOldValue() {
      return this.oldValue;
   }

   /**
    * The new value of this property
    */
   public String getNewValue() {
      return this.newValue;
   }

   public String toXml() {
      StringBuffer buf = new StringBuffer();
      buf.append("<property key='").append(key).append("'>");
      buf.append("  <old>").append(oldValue).append("</old>");
      buf.append("  <new>").append(newValue).append("</new>");
      buf.append("</property>");
      return buf.toString();
   }

   public String toString() {
      return key + "=" + newValue + " [old=" + oldValue + "]";
   }

   /**
    * Throws NotSerializableException, since PropertyChangeEvent objects are not
    * intended to be serializable.
    */
    private void writeObject(java.io.ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }

   /**
    * Throws NotSerializableException, since PropertyChangeEvent objects are not
    * intended to be serializable.
    */
    private void readObject(java.io.ObjectInputStream in) throws NotSerializableException {
        throw new NotSerializableException("Not serializable.");
    }
}

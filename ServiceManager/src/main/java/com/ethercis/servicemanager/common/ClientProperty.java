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


package com.ethercis.servicemanager.common;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;

/**
 * This class encapsulates one client property, for example query parameters
 * The parameters are passed logonservice key value pair with an optional encoding id and a type
 * If the attribute <code>type</code> is missing we assume a 'String' property
 */
public final class ClientProperty extends EncodableData
{
   private static final long serialVersionUID = 6415499809321164696L;
   /** Typically used tag name for plugin attributes */
   public static final String ATTRIBUTE_TAG = "attribute";
   /** Typicall used tag name for subscribeQos and other Qos */
   public static final String CLIENTPROPERTY_TAG = "clientProperty";

   /**
    * @param name  The unique property key
    * @param type The data type of the value
    * @param encoding null or Constants.ENCODING_BASE64="base64"
    * @deprecated you should use the constructors with no global
    */
   public ClientProperty(RunTimeSingleton glob, String name, String type, String encoding) {
      super("clientProperty", name, type, encoding);
      ME = "ClientProperty";
   }

   /**
    * @param name  The unique property key
    * @param type The data type of the value
    * @param encoding null or Constants.ENCODING_BASE64="base64"
    */
   public ClientProperty(String name, String type, String encoding) {
      super("clientProperty", name, type, encoding);
      ME = "ClientProperty";
   }

   /**
    * @param name  The unique property key
    * @param type The data type of the value
    * @param encoding null or Constants.ENCODING_BASE64="base64"
    * @deprecated you should use the alternative with no global.
    */
   public ClientProperty(RunTimeSingleton glob, String name, String type, String encoding, String value) {
      super("clientProperty", name, type, encoding, value);
      ME = "ClientProperty";
   }

   /**
    * @param name  The unique property key
    * @param type The data type of the value
    * @param encoding null or Constants.ENCODING_BASE64="base64"
    * @param value The original value (not yet encoded!)
    */
   public ClientProperty(String name, String type, String encoding, String value) {
      super("clientProperty", name, type, encoding, value);
      ME = "ClientProperty";
   }

   /**
    * Set binary data, will be of type "byte[]" and base64 encoded
    * @param name  The unique property key
    * @param value The binary data
    */
   public ClientProperty(String name, byte[] value) {
      super("clientProperty", name, value);
      ME = "ClientProperty";
   }

   public ClientProperty(String name, boolean value) {
      this(name, Constants.TYPE_BOOLEAN, Constants.ENCODING_NONE, ""+value);
      ME = "ClientProperty";
   }

   public String toString() {
      return getStringValue();
   }
}


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

import java.io.*;

/**
 * Used for Serialization and deserialization for Objects
 */

public class SerializeHelper {

  private final RunTimeSingleton glob;
   private static Logger log = LogManager.getLogger(SerializeHelper.class);
  private final String ME;

  public SerializeHelper(RunTimeSingleton glob) {
    this.glob = glob;

    this.ME = "SerializeHelper" + this.glob.getLogPrefixDashed();
  }


  /**
   * Serializes object to byteArray
   */
  public byte[] serializeObject(Object obj) throws IOException {
    log.info("Serializing object " + obj);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(obj);
    }
    catch (IOException ex) {
      ex.printStackTrace();
      throw new IOException("Unable to serializeObject " + ex.toString());
    }
    return bos.toByteArray();
  }


/**
 * Deserializes byteArray to Java-Object
 */
  public Object deserializeObject(byte[] mybyte) throws IOException {
    log.info("Deserializing object ");
    Object obj = new Object();
    ByteArrayInputStream bas = new ByteArrayInputStream(mybyte);
    try {
      ObjectInputStream ois = new ObjectInputStream(bas);
      obj = ois.readObject();
    }
    catch (ClassNotFoundException ex) {
      throw new IOException("Unable to rebuild Object  "+ ex.toString());
    }
    catch (StreamCorruptedException ex){
        throw new IOException("Unable to rebuild Object  "+ ex.toString());    	
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }

    return obj;
  }


}
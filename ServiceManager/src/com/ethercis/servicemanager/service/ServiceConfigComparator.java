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
import com.ethercis.servicemanager.runlevel.RunLevelAction;
import com.ethercis.servicemanager.runlevel.ServiceConfig;
import org.apache.log4j.Logger;

import java.util.Comparator;

/**
 * This class is used to compare ServiceConfig objects with each other.
 */
public class ServiceConfigComparator implements Comparator<Object>
{
   private String ME = "ServiceConfigComparator";
   private final RunTimeSingleton glob;
   private static Logger log = Logger.getLogger(ServiceConfigComparator.class);
   private boolean isAscending = true;

   /**
    * @param glob the global object. A ClusterController is sufficient here.
    * @param isAscending 'true' if you want to use this comparator for increasing
    *        (ascending) runlevels, i.e. for startup sequences. 'false' if you want
    *        to use it for descending (shutdown) sequences.
    */
   public ServiceConfigComparator(RunTimeSingleton glob, boolean isAscending) {
      this.isAscending = isAscending;
      this.glob = glob;

      this.log.debug("constructor");
   }


   /**
    * Compares its two arguments for order. Returns a negative integer, zero, or a 
    * positive integer logonservice the first argument is less than, equal to, or greater
    * than the second. Null objects are considered of different type and therefore 
    * will throw a runtime exception (ClassCastException).
    */
   public final int compare(Object o1, Object o2) {
      if (!(o1 instanceof ServiceConfig) || !(o2 instanceof ServiceConfig)) {
         String o1Txt = "null", o2Txt = "null";
         if (o1 != null) o1Txt = o1.toString();
         if (o2 != null) o2Txt = o2.toString();
         throw new ClassCastException(ME + " comparison between '" + o1Txt + "' and '" + o2Txt + "' is not possible because wrong types");
      }
      ServiceConfig p1 = (ServiceConfig)o1;
      ServiceConfig p2 = (ServiceConfig)o2;
      RunLevelAction action1 = null, action2 = null;
      if (this.isAscending) {
         action1 = p1.getUpAction();
         action2 = p2.getUpAction();
         int diff = action1.getOnStartupRunlevel() - action2.getOnStartupRunlevel();
         if (diff != 0) return diff;
         diff = action1.getSequence() - action2.getSequence();
         if (diff != 0) return diff;
         return p1.uniqueTimestamp.compareTo(p2.uniqueTimestamp);
      }
      else {
         action1 = p1.getDownAction();
         action2 = p2.getDownAction();
         int diff = action2.getOnShutdownRunlevel() - action1.getOnShutdownRunlevel();
         if (diff != 0) return diff;
         diff = action1.getSequence() - action2.getSequence();
         if (diff != 0) return diff;
         return p2.uniqueTimestamp.compareTo(p1.uniqueTimestamp);
      }
   }

   /**
    * Indicates whether some other object is "equal to" this Comparator.
    */
   public boolean equals(Object obj) {
      return this == obj;
   }

   
}

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


package com.ethercis.servicemanager.common.log;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Dispatch logging response to registered listeners.
 * As java.common.logging framework supports max. one Filter per Logger/Handler
 * we dispatch it further here.
 * <p/>
 * Note:
 * You may not directly add a Filter to java.common.logging.Logger/Handler logonservice this
 * would destroy our registration.
 * <p/>
 * Setup is done in logging.properties:
 *   handlers= java.common.logging.ConsoleHandler, com.ethercis.servicemanager.common.log.SBNotifyHandler
 * <p/>
 */
public class SoaNotifyHandler extends Handler {
   private static SoaNotifyHandler theSBNotifyHandler;
   private final String id;
   private static long instanceCounter;
   private Set errorListenerSet = new HashSet();
   private I_LogListener[] errorCache;
   private Set warnListenerSet = new HashSet();
   private I_LogListener[] warnCache;
   private Set allListenerSet = new HashSet();
   private I_LogListener[] allCache;
   private final I_LogListener[] emptyArr = new I_LogListener[0];
   private boolean hasAllListener;
   
   public SoaNotifyHandler() {
      this("default-"+instanceCounter++);
   }
   
   public SoaNotifyHandler(String id) {
      synchronized (SoaNotifyHandler.class) {
         theSBNotifyHandler = this;
      }
      this.id = id;
   }
   
   public static SoaNotifyHandler instance() {
      if (theSBNotifyHandler == null) {
         synchronized (SoaNotifyHandler.class) {
            if (theSBNotifyHandler == null) {
               theSBNotifyHandler = new SoaNotifyHandler();
            }
         }
      }
      return theSBNotifyHandler;  
   }

   public void close() throws SecurityException {
   }

   public void flush() {
   }

   /* Redirect logging to our listeners.  (non-Javadoc)
    * @see java.common.logging.Handler#publish(java.common.logging.LogRecord)
    */
   public void publish(LogRecord record) {
      //System.out.println("[SBNotifyHandler-"+this.id+"] " + record.getLevel() + " " + record.getMessage());
      int level = record.getLevel().intValue();
      if (Level.WARNING.intValue() == level) {
         I_LogListener[] arr = getWarnListeners();
         for (int i=0; i<arr.length; i++) {
            arr[i].log(record);
         }
      }
      else if (Level.SEVERE.intValue() == level) {
         I_LogListener[] arr = getErrorListeners();
         for (int i=0; i<arr.length; i++) {
            arr[i].log(record);
         }
      }
      if (this.hasAllListener) {
         I_LogListener[] arr = getAllListeners();
         for (int i=0; i<arr.length; i++) {
            arr[i].log(record);
         }
      }
   }

   /**
    * Register a listener. 
    * This listener may NOT use logging himself to avoid recursion. <br />
    * If the given <code>level/logNotification</code> combination is already registered,
    * the call leaves everything unchanged and returns false.
    * @param level to add, Level.SEVERE.intValue() | Level.WARNING.intValue() | Level.ALL.intValue()
    * @param logNotification The interface to send the logging
    * @return true if the given logNotification is added
    */
   public synchronized boolean register(int level, I_LogListener logNotification) {
      boolean ret = false;
      if (Level.WARNING.intValue() == level) {
         ret = this.warnListenerSet.add(logNotification);
         this.warnCache = null;
      }
      else if (Level.SEVERE.intValue() == level) {
         ret = this.errorListenerSet.add(logNotification);
         this.errorCache = null;
      }
      else if (Level.ALL.intValue() == level) {
         ret = this.allListenerSet.add(logNotification);
         this.allCache = null;
         this.hasAllListener = true;
      }
      return ret;
   }

   /**
    * Remove the listener. 
    * @param level Which levels you want to remove. Level.SEVERE.intValue() | Level.WARNING.intValue() | Level.ALL.intValue()
    * @return true if the set contained the specified element.
    */
   public synchronized boolean unregister(int level, I_LogListener logNotification) {
      boolean ret = false;
      if (Level.WARNING.intValue() == level) {
         ret = this.warnListenerSet.remove(logNotification);
         this.warnCache = null;
      }
      else if (Level.SEVERE.intValue() == level) {
         ret = this.errorListenerSet.remove(logNotification);
         this.errorCache = null;
      }
      else if (Level.ALL.intValue() == level) {
         ret = this.allListenerSet.remove(logNotification);
         this.allCache = null;
         if (this.allListenerSet.size() == 0)
            this.hasAllListener = false;
      }
      return ret;
   }

   /**
    * Get a snapshot of warn listeners. 
    */
   public I_LogListener[] getWarnListeners() {
      if (this.warnCache == null) {
         synchronized (this) {
            if (this.warnCache == null) {
               this.warnCache = (I_LogListener[])this.warnListenerSet.toArray(new I_LogListener[this.warnListenerSet.size()]);
            }
         }
      }
      return this.warnCache;
   }

   /**
    * @return Returns the id.
    */
   public String getId() {
      return this.id;
   }

   /**
    * Get a snapshot of error listeners. 
    */
   public I_LogListener[] getErrorListeners() {
      if (this.errorCache == null) {
         synchronized (this) {
            if (this.errorCache == null) {
               this.errorCache = (I_LogListener[])this.errorListenerSet.toArray(new I_LogListener[this.errorListenerSet.size()]);
            }
         }
      }
      return this.errorCache;
   }

   /**
    * Get a snapshot of all listeners. 
    */
   public I_LogListener[] getAllListeners() {
      if (!this.hasAllListener) return emptyArr;
      if (this.allCache == null) {
         synchronized (this) {
            if (this.allCache == null) {
               this.allCache = (I_LogListener[])this.allListenerSet.toArray(new I_LogListener[this.allListenerSet.size()]);
            }
         }
      }
      return this.allCache;
   }

}

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


package com.ethercis.servicemanager.runlevel;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class contains the information on how and when a certain service is invoked by the run level manager
 * <p>
 * <pre>
 *  &lt;action do='LOAD'
 *             onStartupRunlevel='3'
 *             sequence='5'
 *             onFail='RESOURCE_CONFIGURATION_SERVICEFAILED'/>
 * </pre>
 */
public class RunLevelAction
{
   public final static String LOAD = "LOAD";
   public final static String STOP = "STOP";

   private final RunTimeSingleton glob;
   private static Logger log = LogManager.getLogger(RunLevelAction.class);

   /* the action to trigger (either LOAD or STOP) */
   private String action;

   /* the run level when going up */
   private int upLevel = -1;

   /* the run level when going down */
   private int downLevel = -1;

   /* the error code to return in case of an error. If null, no error will be returned */
   private SysErrorCode errorCode;

   /* the runlevel internal sequence number at which this action will be invoked */
   private int sequence = 0;


   /**
    * This constructor takes all parameters needed
    */
   public RunLevelAction(RunTimeSingleton glob, String action, int upLevel, int downLevel,
      SysErrorCode errorCode, int sequence) {

      log.debug("constructor");
      this.glob = glob;
      this.action = action;
      this.upLevel  = upLevel;
      this.downLevel = downLevel;
      this.errorCode = errorCode;
      this.sequence = sequence;
   }

   /**
    * This constructor is the minimal constructor.
    */
   public RunLevelAction(RunTimeSingleton glob) {
      this(glob, LOAD, -1, -1, null, 0);
   }

   /**
    * returns a clone of this object.
    */
   public Object clone() {
      return new RunLevelAction(this.glob, this.action, this.upLevel,
                                this.downLevel, this.errorCode, this.sequence);
   }

   public String getDo() {
      return this.action;
   }

   public void setDo(String action) {
      this.action = action;
   }

   public int getOnStartupRunlevel() {
      return this.upLevel;
   }

   public void setOnStartupRunlevel(int upLevel) {
      this.upLevel = upLevel;
   }

   public boolean isOnStartupRunlevel() {
     return this.upLevel > 0;
   }

   public int getOnShutdownRunlevel() {
      return this.downLevel;
   }

   public void setOnShutdownRunlevel(int downLevel) {
      this.downLevel = downLevel;
   }

   public boolean isOnShutdownRunlevel() {
     return this.downLevel > 0;
   }

   public SysErrorCode getOnFail() {
      return this.errorCode;
   }

   public void setOnFail(SysErrorCode errorCode) {
      this.errorCode = errorCode;
   }

   public boolean hasOnFail() {
      return this.errorCode != null;
   }

   public int getSequence() {
      return this.sequence;
   }

   public void setSequence(int sequence) {
      this.sequence = sequence;
   }

   public String toXml(String extraOffset) {
      StringBuffer sb = new StringBuffer(512);
      if (extraOffset == null) extraOffset = "";
      String offset = Constants.OFFSET + extraOffset;

      sb.append(offset).append("<action ");
      sb.append("do='").append(this.action).append("' ");
      if (this.upLevel > -1)
         sb.append("onStartupRunlevel='").append(this.upLevel).append("' ");
      if (this.downLevel > -1)
         sb.append("onShutdownRunlevel='").append(this.downLevel).append("' ");
      if (this.errorCode != null ) {
         sb.append("onFail='").append(this.errorCode.getErrorCode()).append("' ");
      }
      if (this.sequence > 0) { // zero is default and therefore not written ...
         sb.append("sequence='").append(this.sequence).append("' ");
      }
      sb.append("/>");
      return sb.toString();
   }

   public String toXml() {
      return toXml("");
   }
}

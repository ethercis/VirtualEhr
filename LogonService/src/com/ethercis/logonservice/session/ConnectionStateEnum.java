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
 * Project: EtherCIS openEHR system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */

//Copyright
package com.ethercis.logonservice.session;

import com.ethercis.servicemanager.common.session.I_ConnectionStateEnum;


/**
 * This class simulates an enumeration for the connection states. 
 * <p>
 * Note that this implementation has a fixed number of four states.
 */
public final class ConnectionStateEnum implements java.io.Serializable, I_ConnectionStateEnum
{
   private static final long serialVersionUID = -8057592644593066934L;

   private final int connectionState;

   private ConnectionStateEnum(int connectionState) {
      this.connectionState = connectionState;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_ConnectionStateEnum#toString()
 */
   @Override
public String toString() {
      switch(this.connectionState) {
         case -1: return "UNDEF";
         case 0:  return "ALIVE";
         case 1:  return "POLLING";
         case 2:  return "DEAD";
         default: return ""+this.connectionState; // error
      }
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_ConnectionStateEnum#equals(com.ethercis.servicemanager.session.I_ConnectionStateEnum)
 */
@Override
public boolean equals(I_ConnectionStateEnum other) {
      return this.connectionState == other.getInt();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_ConnectionStateEnum#getInt()
 */
   @Override
public final int getInt() {
      return connectionState;
   }

   /**
    * Checks the given int and returns the corresponding ConnectionStateEnum instance. 
    * @param connectionState For example 7
    * @return The enumeration object for this connectionState
    * @exception IllegalArgumentException if the given connectionState is invalid
    */
   public static final I_ConnectionStateEnum toConnectionStateEnum(int connectionState) throws IllegalArgumentException {
      if (connectionState < -1 || connectionState > 2) {
         throw new IllegalArgumentException("ConnectionStateEnum: The given connectionState=" + connectionState + " is illegal");
      }
      return connectionStateEnumArr[connectionState];
   }

   /**
    * Parses given string to extract the connectionState of a message. 
    * We are case insensitive (e.g. "POLLING" or "poLLING" are OK). 
    * @param state For example "POLLING"
    * @return The ConnectionStateEnum instance for the message connectionState
    * @exception IllegalArgumentException if the given connectionState is invalid
    */
   public static final I_ConnectionStateEnum parseConnectionState(String state) throws IllegalArgumentException {
      if (state == null) {
         throw new IllegalArgumentException("ConnectionStateEnum: Given connectionState is null");
      }
      state = state.trim();
      try {
         int connectionState = new Integer(state).intValue();
         return toConnectionStateEnum(connectionState); // may throw IllegalArgumentException
      } catch (NumberFormatException e) {
         state = state.toUpperCase();
         if (state.startsWith("UNDEF"))
            return UNDEF;
         else if (state.startsWith("ALIVE"))
            return ALIVE;
         else if (state.startsWith("POLLING"))
            return POLLING;
         else if (state.startsWith("DEAD"))
            return DEAD;
      }
      throw new IllegalArgumentException("ConnectionStateEnum:  Wrong format of <connectionState>" + state +
                    "</connectionState>, expected one of UNDEF|ALIVE|POLLING|DEAD");
   }

   /**
    * Parses given string to extract the connectionState of a message
    * @param prio For example "polling" or "alive"
    * @param defaultConnectionState Value to use if not parsable
    * @return The ConnectionStateEnum instance for the message connectionState
    */
   public static final I_ConnectionStateEnum parseConnectionState(String connectionState, I_ConnectionStateEnum defaultConnectionState) {
      try {
         return parseConnectionState(connectionState);
      }
      catch (IllegalArgumentException e) {
         System.out.println("ConnectionStateEnum: " + e.toString() + ": Setting connectionState to " + defaultConnectionState);
         return defaultConnectionState;
      }
   }

   /**
    * The connection state is not known (-1).
    */
   public static final I_ConnectionStateEnum UNDEF = new ConnectionStateEnum(-1);

   /**
    * We have a connection (0).
    */
   public static final I_ConnectionStateEnum ALIVE = new ConnectionStateEnum(0);

   /**
    * We have lost the connection and are polling for it (1).
    */
   public static final I_ConnectionStateEnum POLLING = new ConnectionStateEnum(1);

   /**
    * The connection is dead an no recovery is possible (2).
    */
   public static final I_ConnectionStateEnum DEAD = new ConnectionStateEnum(2);

   /**
    * For good performance have a static array of all priorities
    */
   private static final I_ConnectionStateEnum[] connectionStateEnumArr = {
      UNDEF,
      ALIVE,
      POLLING,
      DEAD
   };
}


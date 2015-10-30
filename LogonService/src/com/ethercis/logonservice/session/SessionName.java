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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.cluster.NodeId;
import com.ethercis.servicemanager.common.ReplaceVariable;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.session.I_SessionName;

/**
 * Handles unified naming convention of login names and user sessions.
 * 
 * Instances are immutable.
 * 
 */
public final class SessionName implements java.io.Serializable, I_SessionName {
   private static final long serialVersionUID = 2684742715895586788L;
   /** Name for logging response */
   private static String ME = "SessionName";
   private transient final RunTimeSingleton glob;
   public final static String ROOT_MARKER_TAG = "/" + ContextNode.CLUSTER_MARKER_TAG; // "/node";
   public final static String SUBJECT_MARKER_TAG = ContextNode.SUBJECT_MARKER_TAG; // "client";
   /** The absolute name */
   private String absoluteName;
   private NodeId nodeId;
   private String relativeName;
   private final String subjectId; // == loginName
   private long pubSessionId;
   private boolean nodeIdExplicitlyGiven = false;

   // TODO:
   // On release 2.0 we should change the default to the new
   // "client/joe/session/1" notation.
   // Note that C++ clients compiled before V1.4 and java clients before V1.3
   // can't handle
   // the new notation
   private static boolean useSessionMarker = false;

   static {
      // To switch back to old "client/joe/1" markup, default is now
      // "client/joe/session/1"
      useSessionMarker = RunTimeSingleton.instance().getProperty().get("ehrserver/useSessionMarker", useSessionMarker);
   }

   public static boolean useSessionMarker() {
      return useSessionMarker;
   }

   /**
    * Create and parse a unified name.
    * <p>
    * 
    * @param name
    *           Examples:
    * 
    *           <pre>
    *  /node/heron/client/joe/2
    *  client/joe/2
    *  joe/2
    *  joe
    *  /node/heron/client/joe/session/2
    *  client/joe/session/2
    *  joe/session/2
    * </pre>
    * @exception IllegalArgumentException
    *               if your name can't be parsed
    */
   public SessionName(RunTimeSingleton glob, String name) {
      this(glob, (NodeId) null, name);
   }

   public SessionName(RunTimeSingleton glob, NodeId nodeId, String subjectId, long pubSessionId) {
      this.glob = glob;
      this.nodeId = nodeId;
      if (nodeId != null) {
         this.nodeIdExplicitlyGiven = true;
      }
      this.subjectId = subjectId;
      this.pubSessionId = pubSessionId;
   }

   /**
    * @param nodeId
    *           if not null it has precedence to the nodeId which is probably
    *           found in name
    * @param name
    *           Examples:
    * 
    *           <pre>
    *  /node/heron/client/joe/2
    *  client/joe/2
    *  joe/2
    *  joe
    * </pre>
    * 
    *           The EventPlugin supports wildcard '*' logonservice sessionNumber, to be
    *           able to use this parser we map it to Long.MIN_VALUE
    * @exception IllegalArgumentException
    *               if your name can't be parsed
    */
   public SessionName(RunTimeSingleton glob, NodeId nodeId, String name) {
      this.glob = (glob == null) ? RunTimeSingleton.instance() : glob;

      if (name == null) {
         throw new IllegalArgumentException(ME + ": Your given name is null");
      }

      // missing clusterNodeId is similar to relative addressing
      if (name.startsWith("/node//")) // ROOT_MARKER_TAG
         name = name.substring(7);
      String relative = name;

      // parse absolute part
      if (name.startsWith("/")) {
         String[] arr = ReplaceVariable.toArray(name, "/");
         if (arr.length == 0) {
            throw new IllegalArgumentException(ME + ": '" + name + "': The root tag must be '/node'.");
         }
         if (arr.length > 0) {
            if (!"node".equals(arr[0]))
               throw new IllegalArgumentException(ME + ": '" + name + "': The root tag must be '/node'.");
         }
         if (arr.length > 1) {
            if (nodeId != null) {
               this.nodeId = nodeId; // given nodeId is strongest
               this.nodeIdExplicitlyGiven = true;
            }
            // else if (glob.isServer()) {
            // this.nodeId = glob.getNodeId(); // always respect the given name
            // }
            else {
               this.nodeId = new NodeId(arr[1]); // the parsed nodeId
               this.nodeIdExplicitlyGiven = true;
               if ("unknown".equals(this.nodeId.getId()))
                  this.nodeId = null;
            }
         }
         if (arr.length > 2) {
            if (!SUBJECT_MARKER_TAG.equals(arr[2]))
               throw new IllegalArgumentException(ME + ": '" + name + "': 'client' tag is missing.");
         }

         relative = "";
         for (int i = 3; i < arr.length; i++) {
            relative += arr[i];
            if (i < (arr.length - 1))
               relative += "/";
         }
      }

      if (this.nodeId == null) {
         if (nodeId != null) {
            this.nodeId = nodeId; // given nodeId is strongest
            this.nodeIdExplicitlyGiven = true;
         } else if (this.glob.isServerSide()) { // if nodeId still not known we
            // set it to the servers nodeId
            this.nodeId = this.glob.getNodeId();
            this.nodeIdExplicitlyGiven = false;
         }
         // else {
         // this.nodeId = nodeId;
         // }
      }

      // parse relative part
      if (relative.length() < 1) {
         throw new IllegalArgumentException(ME + ": '" + name + "': No relative information found.");
      }

      int ii = 0;
      String[] arr = ReplaceVariable.toArray(relative, ContextNode.SEP); // "/"
      if (arr.length > ii) {
         String tmp = arr[ii++];
         if (SUBJECT_MARKER_TAG.equals(tmp)) { // "client"
            if (arr.length > ii) {
               this.subjectId = arr[ii++];
            } else {
               throw new IllegalArgumentException(ME + ": '" + name + "': No relative information found.");
            }
         } else {
            this.subjectId = tmp;
         }
      } else {
         throw new IllegalArgumentException(ME + ": '" + name + "': No relative information found.");
      }
      if (arr.length > ii) {
         String tmp = arr[ii++];
         if (ContextNode.SESSION_MARKER_TAG.equals(tmp)) {
            if (arr.length > ii) {
               tmp = arr[ii++];
            }
         }
         if ("*".equals(tmp)) { // The eventplugin supports wildcard, to use
            // this parser we map it to Long.MIN_VALUE)
            this.pubSessionId = Long.MIN_VALUE;
         } else {
            this.pubSessionId = Long.parseLong(tmp);
         }
      }
   }

   /**
    * Create a new instance based on the given sessionName but with
    * added/changed pubSessionId
    */
   public SessionName(RunTimeSingleton glob, I_SessionName sessionName, long pubSessionId) {
      this(glob, sessionName.getAbsoluteName());
      this.pubSessionId = pubSessionId;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getAbsoluteName()
 */
@Override
public String getAbsoluteName() {
      return getAbsoluteName(false);
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getAbsoluteName(boolean)
 */
   @Override
public String getAbsoluteName(boolean forceSessionMarker) {
      if (this.absoluteName == null) {
         StringBuffer buf = new StringBuffer(256);
         // buf.append("/node/").append((this.nodeId==null)?"unknown":this.nodeId.getId()).append("/");
         if (this.nodeId != null) {
            buf.append("/node/").append(this.nodeId.getId()).append("/");
         }
         buf.append(getRelativeName(forceSessionMarker));
         this.absoluteName = buf.toString();
      }
      return this.absoluteName;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getAbsoluteSubjectName()
 */
   @Override
public String getAbsoluteSubjectName() {
      StringBuffer buf = new StringBuffer(256);
      if (this.nodeId != null) {
         buf.append("/node/").append(this.nodeId.getId()).append("/");
      }
      buf.append(ContextNode.SUBJECT_MARKER_TAG).append("/").append(subjectId);
      return buf.toString();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#toString()
 */
   @Override
public String toString() {
      return getAbsoluteName();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getRelativeName()
 */
   @Override
public String getRelativeName() {
      return getRelativeName(false);
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getRelativeName(boolean)
 */
   @Override
public String getRelativeName(boolean forceSessionMarker) {
      // Conflict with existing "client/joe/session/1" and reconnecting
      // "client/joe/1"
      // swtich cache of
      /* if (this.relativeName == null || forceSessionMarker) { */
      StringBuffer buf = new StringBuffer(126);
      // For example "client/joe/session/-1"
      buf.append(ContextNode.SUBJECT_MARKER_TAG).append("/").append(subjectId);
      if (isSession()) {
         buf.append("/");
         if (useSessionMarker || forceSessionMarker)
            buf.append(ContextNode.SESSION_MARKER_TAG).append("/");
         buf.append("" + this.pubSessionId);
      }
      this.relativeName = buf.toString();
      /* } */
      return this.relativeName;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getRelativeNameWithoutSessionMarker()
 */
   @Override
public String getRelativeNameWithoutSessionMarker() {
      StringBuffer buf = new StringBuffer(126);
      buf.append(ContextNode.SUBJECT_MARKER_TAG).append("/").append(subjectId);
      if (isSession()) {
         buf.append("/").append("" + this.pubSessionId);
      }
      return buf.toString();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isNodeIdExplicitlyGiven()
 */
   @Override
public boolean isNodeIdExplicitlyGiven() {
      return this.nodeIdExplicitlyGiven;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getNodeId()
 */
   @Override
public NodeId getNodeId() {
      return this.nodeId;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getNodeIdStr()
 */
   @Override
public String getNodeIdStr() {
      return (this.nodeId == null) ? null : this.nodeId.getId();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getLoginName()
 */
   @Override
public String getLoginName() {
      return this.subjectId;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isInternalLoginName()
 */
@Override
public final boolean isInternalLoginName() {
      // assumes that services use "_" and core use "__" (same start logonservice services!)
      return this.subjectId.startsWith(Constants.INTERNAL_LOGINNAME_PREFIX_FOR_SERVICES);
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isPluginInternalLoginName()
 */
@Override
public final boolean isPluginInternalLoginName() {
      return this.subjectId.startsWith(Constants.INTERNAL_LOGINNAME_PREFIX_FOR_SERVICES) && !isCoreInternalLoginName();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isCoreInternalLoginName()
 */
@Override
public final boolean isCoreInternalLoginName() {
      return this.subjectId.startsWith(Constants.INTERNAL_LOGINNAME_PREFIX_FOR_CORE);
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getPublicSessionId()
 */
   @Override
public long getPublicSessionId() {
      return this.pubSessionId;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isSession()
 */
   @Override
public boolean isSession() {
      return this.pubSessionId != 0L;
   }

   // public void mutateToSubject() {
   // this.pubSessionId = 0L;
   // this.relativeName = null;
   // }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isPubSessionIdInternal()
 */
   @Override
public boolean isPubSessionIdInternal() {
      return this.pubSessionId < 0L;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#isPubSessionIdUser()
 */
   @Override
public boolean isPubSessionIdUser() {
      return this.pubSessionId > 0L;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#equalsRelative(com.ethercis.servicemanager.session.I_SessionName)
 */
   @Override
public boolean equalsRelative(I_SessionName sessionName) {
      return getRelativeName().equals(sessionName.getRelativeName());
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#equalsAbsolute(com.ethercis.servicemanager.session.I_SessionName)
 */
@Override
public boolean equalsAbsolute(I_SessionName sessionName) {
      return getAbsoluteName().equals(sessionName.getAbsoluteName());
   }

   /**
    * Guess a SessionName from given callback queueName of old xb_entries (was
    * created from ClusterController.getStrippedId)
    * 
    * <pre>
    * callback_nodeheronclientjack1   /node/frodo/client/jack/session/1
    * clientsubscriber71              /node/frodo/client/subscriber/session/71
    * clientsubscriber7-1             /node/frodo/client/subscriber7/session/-1
    * </pre>
    * 
    * With limitPositivePubToOneDigit==true:
    * 
    * <pre>
    * clientsubscriber71 / node / heron / client / subscriber7 / session / -1
    * </pre>
    * 
    * Is not bullet proof:
    * 
    * <pre>
    * clientpublisher-222   Could be publisher-22/session/2
    * clientsubscriber7777  Could be subscriber77/session/77
    * </pre>
    * 
    * @param glob
    * @param nodeId
    *           The Node to use
    * @param queueName
    *           Only for client names, can fail if subjectId ends with number
    *           and has positive session id!
    * @return
    */
   public static I_SessionName guessSessionName(RunTimeSingleton glob, String nodeId, String queueName) {
      return guessSessionName(glob, nodeId, queueName, false);
   }

   /**
    * Not reliable.
    * 
    * @param glob
    * @param nodeId
    *           If null is extracted from queueName
    * @param queueName
    *           e.g. "connection_nodefrodoclientjack1" or
    *           "connection_clientjack1"
    * @param limitPositivePubToOneDigit
    * @return null if no useful arguments
    */
   public static I_SessionName guessSessionName(RunTimeSingleton glob, String nodeId, String queueName,
         boolean limitPositivePubToOneDigit) {
      if (queueName == null)
         return null;
      int nodePos = queueName.indexOf("_node"); // connection_nodeheronclientjack1
      int pos = queueName.indexOf("client");
      if (pos == -1)
         return null;
      if (nodeId == null && nodePos != -1 && pos > nodePos) {
         nodeId = queueName.substring(nodePos + "_node".length());
         int posClient = nodeId.indexOf("client");
         nodeId = nodeId.substring(0, posClient);
      }
      String tail = queueName.substring(pos + "client".length());
      int len = tail.length();
      int i;
      boolean minusFound = false;
      boolean digitFound = false;
      int limit = -1;
      for (i = (len - 1); i >= 0; i--) {
         if (minusFound) {
            break;
         }
         if (digitFound && tail.charAt(i) == '-') {
            minusFound = true;
            continue;
         }
         if (digitFound && limitPositivePubToOneDigit && limit == -1) {
            limit = i;
         }
         if (Character.isDigit(tail.charAt(i))) {
            digitFound = true;
            continue;
         }
         if (limit != -1)
            i = limit;
         break;
      }
      String subjectId = "";
      int pubSessionId = 0;
      if (i > 0 && i < (len - 1)) {
         try {
            pubSessionId = Integer.parseInt(tail.substring(i + 1));
         } catch (NumberFormatException e) {
            e.printStackTrace();
         }
         subjectId = tail.substring(0, i + 1);
      } else
         subjectId = tail;
      return new SessionName(glob, new NodeId(nodeId), subjectId, pubSessionId);
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#matchRelativeName(java.lang.String)
 */
   @Override
public boolean matchRelativeName(String pattern) {
      if (pattern == null)
         return false;
      if (pattern.indexOf("*") == -1) // exact match
         return equalsRelative(new SessionName(this.glob, pattern));
      if (pattern.equals(ContextNode.SUBJECT_MARKER_TAG + ContextNode.SEP + "*" + ContextNode.SEP
            + ContextNode.SESSION_MARKER_TAG + ContextNode.SEP + "*"))
         return true; // "client/*/session/*"
      if (pattern.startsWith(ContextNode.SUBJECT_MARKER_TAG + ContextNode.SEP + "*" + ContextNode.SEP)) {
         I_SessionName tmp = new SessionName(this.glob, pattern);
         return tmp.getPublicSessionId() == this.getPublicSessionId();
      }
      if (pattern.endsWith(ContextNode.SESSION_MARKER_TAG + ContextNode.SEP + "*")) {
         // our ctor fails to parse "*" to a number, so we do it manually here
         String[] arr = ReplaceVariable.toArray(pattern, ContextNode.SEP); // "/"
         return (arr.length >= 2 && arr[1].equals(this.getLoginName()));
      }
      return false;
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getRelativeSubjectIdWildcard()
 */
   @Override
public String getRelativeSubjectIdWildcard() {
      return ContextNode.SUBJECT_MARKER_TAG + ContextNode.SEP + "*" + ContextNode.SEP + ContextNode.SESSION_MARKER_TAG
            + ContextNode.SEP + getPublicSessionId();
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getRelativePubSessionIdWildcard()
 */
   @Override
public String getRelativePubSessionIdWildcard() {
      return ContextNode.SUBJECT_MARKER_TAG + ContextNode.SEP + getLoginName() + ContextNode.SEP
            + ContextNode.SESSION_MARKER_TAG + ContextNode.SEP + "*";
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#getRelativeWildcard()
 */
   @Override
public String getRelativeWildcard() {
      return ContextNode.SUBJECT_MARKER_TAG + ContextNode.SEP + "*" + ContextNode.SEP + ContextNode.SESSION_MARKER_TAG
            + ContextNode.SEP + "*";
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#toXml()
 */
   @Override
public final String toXml() {
      return toXml((String) null);
   }

   /* (non-Javadoc)
 * @see com.ethercis.servicemanager.session.I_SessionName#toXml(java.lang.String)
 */
   @Override
public final String toXml(String extraOffset) {
      StringBuffer sb = new StringBuffer(256);
      if (extraOffset == null)
         extraOffset = "";
      String offset = Constants.OFFSET + extraOffset;

      sb.append(offset).append("<SessionName id='").append(getAbsoluteName());
      sb.append("' isSession='").append(isSession()).append("'>");
      sb.append(offset).append(" <nodeId>").append(getNodeIdStr()).append("</nodeId>");
      sb.append(offset).append(" <relativeName>").append(getRelativeName()).append("</relativeName>");
      sb.append(offset).append(" <loginName>").append(getLoginName()).append("</loginName>");
      sb.append(offset).append(" <pubSessionId>").append(getPublicSessionId()).append("</pubSessionId>");
      sb.append(offset).append("</SessionName>");

      return sb.toString();
   }

   /**
    * Method for testing only.
    * <p />
    * 
    */
   public static void main(String args[]) {
      RunTimeSingleton glob = RunTimeSingleton.instance().getClone(args);
      try {
         String name = (args.length >= 2) ? args[1] : "guest";
         I_SessionName sessionName = new SessionName(glob, name);
         System.out.println("AbsoluteName=" + sessionName.getAbsoluteName() + " RelativeName="
               + sessionName.getRelativeName());
      } catch (IllegalArgumentException e) {
         System.out.println("ERROR: " + e.toString());
      }
      String[] queueNames = { "clientsubscriberDummy", "client555", "clientsubscriber71", "clientsubscriber7-1",
            "clientpublisherToHeron222", "clientpublisherToHeron-222", "clientjoe" };
      for (int i = 0; i < queueNames.length; i++) {
         String queueName = queueNames[i];
         I_SessionName sn = SessionName.guessSessionName(glob, "heron", queueName);
         System.out.println("queueName='" + queueName + "' -> '" + sn.getAbsoluteName(true) + "'");
      }
      for (int i = 0; i < queueNames.length; i++) {
         String queueName = queueNames[i];
         I_SessionName sn = SessionName.guessSessionName(glob, "heron", queueName, true);
         System.out.println("queueName='" + queueName + "' -> '" + sn.getAbsoluteName(true)
               + "' (limited pubSessionId)");
      }
   }
}

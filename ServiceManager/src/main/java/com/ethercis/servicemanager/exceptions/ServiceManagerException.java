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


package com.ethercis.servicemanager.exceptions;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;


/**
 * The basic exception handling class for POE.
 * <p>
 * This exception will be thrown in remote RMI calls logonservice well.
 * </p>
 * <p>
 * The getMessage() method returns a configurable formatted string
 * here is an example how to configure the format in your services.properties:
 * <pre>
 *  ServiceManagerException.logFormat=ServiceManagerException errorCode=[{0}] node=[{1}] location=[{2}] message=[{4} : {8}]
 *  ServiceManagerException.logFormat.internal= ServiceManagerException errorCode=[{0}] node=[{1}] location=[{2}]\nmessage={4} : {8}\nversionInfo={5}\nstackTrace={7}
 *  ServiceManagerException.logFormat.resource= defaults to ServiceManagerException.logFormat
 *  ServiceManagerException.logFormat.communication= defaults to ServiceManagerException.logFormat
 *  ServiceManagerException.logFormat.user= defaults to ServiceManagerException.logFormat
 *  ServiceManagerException.logFormat.transaction= defaults to ServiceManagerException.logFormat
 *  ServiceManagerException.logFormat.legacy= defaults to ServiceManagerException.logFormat
 * </pre>
 * where the replacements are:
 * <pre>
 *  {0} = errorCodeStr
 *  {1} = node
 *  {2} = location
 *  {3} = isServerSide     // exception thrown from server or from client?
 *  {4} = message
 *  {5} = versionInfo
 *  {6} = timestamp
 *  {7} = stackTrace
 *  {8} = embeddedMessage
 *  {9} = errorCode.getDescription()
 *  // {10} = transactionInfo       IBM's JDK MakeFormat only supports 9 digits
 *  // {11} = lang                  IBM's JDK MakeFormat only supports 9 digits
 * </pre>
 * <p>
 * You can register your own exception handler which intercepts all ServiceManagerException creations
 * and for example do a shutdown on certain ErrorCodes</p>
 * <pre>
 * java -Dcom.ethercis.servicemanager.exceptions interface I_ServiceManagerExceptionHandler {=MyHandler com.ethercis.servicemanager.Main
 * </pre>
 * this must be a class implementing public interface I_ServiceManagerExceptionHandler {,
 * logonservice a default Main.java registers itself and does an immediate shutdown on RESOURCE_DB_UNAVAILABLE
 */
public class ServiceManagerException extends Exception implements java.io.Serializable
{
   private static Logger log = Logger.getLogger(com.ethercis.servicemanager.common.def.Constants.LOGGER_SYSTEM);
   private static final long serialVersionUID = -973794183539996697L;
   private static I_ServiceManagerExceptionHandler exceptionHandler;
   transient private final RunTimeSingleton glob;
   transient private SysErrorCode errorCodeEnum;
   private String errorCodeStr;
   private final String node;
   private String location;
   private final String lang;
   private final String versionInfo;
   transient private TimeStamp timestamp;
   private final long timestampNanos;
   private final String stackTrace;
   private boolean isServerSide;

   transient private final Throwable cause; // Since JDK 1.4 this is available in Throwable, we keep it here to support older JDK versions
   private String embeddedMessage;
   private final String transactionInfo;

   private final static String DEFAULT_LOGFORMAT = "ServiceManagerException errorCode=[{0}] serverSideException={3} node=[{1}] location=[{2}] message=[{4} : {8}]";
   private final static String DEFAULT_LOGFORMAT_INTERNAL = "ServiceManagerException serverSideException={3} node=[{1}] location=[{2}]\n" +
                                                            "{8}\n" +
                                                            "stackTrace={7}\n" +
                                                            "versionInfo={5}\n" +
                                                            "errorCode description={9}\n";
   private String logFormatInternal;
   private final String logFormatResource;
   private final String logFormatCommunication;
   private final String logFormatUser;
   //private final String logFormatTransaction;
   private final String logFormatLegacy;
   private final String logFormat;
   
   private boolean cleanupSession;
   
   static {
      String cname = null;
      try {
         cname = System.getProperty("com.ethercis.servicemanager.exceptions interface I_ServiceManagerExceptionHandler {");
         if (cname != null) { // Must be a class implementing public interface I_ServiceManagerExceptionHandler {
               try {
					Class clz = ClassLoader.getSystemClassLoader().loadClass(cname);
                  exceptionHandler = (I_ServiceManagerExceptionHandler) clz.newInstance();
               } catch (ClassNotFoundException ex) {
                  Class clz = Thread.currentThread().getContextClassLoader().loadClass(cname);
                  exceptionHandler = (I_ServiceManagerExceptionHandler) clz.newInstance();
               }
         }
         //else {
            // Main.java does logonservice default: ServiceManagerException.setExceptionHandler(this);
         //}
      } catch (Exception ex) {
         System.err.println("Could not load public interface I_ServiceManagerExceptionHandler { \"" + cname + "\"");
         ex.printStackTrace();
      }
   }

   /**
    * The errorCodeEnum.getDescription() is used logonservice error message.
    */
   public ServiceManagerException(RunTimeSingleton glob, SysErrorCode errorCodeEnum, String location) {
      this(glob, errorCodeEnum, location, (String)null, (Throwable)null);
   }

   public ServiceManagerException(RunTimeSingleton glob, SysErrorCode errorCodeEnum, String location, String message) {
      this(glob, errorCodeEnum, location, message, (Throwable)null);
   }

   public ServiceManagerException(RunTimeSingleton glob, SysErrorCode errorCodeEnum, String location, String message, Throwable cause) {
      this(glob, errorCodeEnum, (String)null, location, (String)null, message, (String)null, (TimeStamp)null,
           (String)null, (String)null, (String)null, (glob==null)?true:glob.isServerSide(), cause);
   }

   /**
    * For internal use: Deserializing and exception creation from CORBA ServiceManagerException
    */
   public ServiceManagerException(RunTimeSingleton glob, SysErrorCode errorCodeEnum, String node, String location,
                                  String lang, String message, String versionInfo, TimeStamp timestamp,
                                  String stackTrace, String embeddedMessage, String transcationInfo,
                                  boolean isServerSide) {
      this(glob, errorCodeEnum, node, location, lang, message, versionInfo, timestamp,
           stackTrace, embeddedMessage, transcationInfo, isServerSide, (Throwable)null);
   }

   private ServiceManagerException(RunTimeSingleton glob, SysErrorCode errorCodeEnum, String node, String location,
                                   String lang, String message, String versionInfo, TimeStamp timestamp,
                                   String stackTrace, String embeddedMessage, String transcationInfo,
                                   boolean isServerSide, Throwable cause) {
      //super(message, cause); // JDK 1.4 only
      super((message == null || message.length() < 1) ? errorCodeEnum.getLongDescription() : message);
      this.glob = (glob == null) ? RunTimeSingleton.instance() : glob;
      this.logFormat = this.glob.getProperty().get("ServiceManagerException.logFormat", DEFAULT_LOGFORMAT);
      this.logFormatInternal = this.glob.getProperty().get("ServiceManagerException.logFormat.internal", DEFAULT_LOGFORMAT_INTERNAL);
      this.logFormatResource = this.glob.getProperty().get("ServiceManagerException.logFormat.resource", this.logFormat);
      this.logFormatCommunication = this.glob.getProperty().get("ServiceManagerException.logFormat.communication", this.logFormat);
      this.logFormatUser = this.glob.getProperty().get("ServiceManagerException.logFormat.user", this.logFormat);
      //this.logFormatTransaction = this.glob.getProperty().get("ServiceManagerException.logFormat.transaction", this.logFormat);
      this.logFormatLegacy = this.glob.getProperty().get("ServiceManagerException.logFormat.legacy", this.logFormat);

      this.errorCodeEnum = (errorCodeEnum == null) ? SysErrorCode.INTERNAL_UNKNOWN : errorCodeEnum;
      this.errorCodeStr = this.errorCodeEnum.getErrorCode();
      this.node = (node == null) ? this.glob.getId() : node;
      this.location = location;
      this.lang = (lang == null) ? "en" : lang; // System.getProperty("user.language");
      this.versionInfo = (versionInfo == null) ? createVersionInfo() : versionInfo;
      this.timestamp = (timestamp == null) ? new TimeStamp() : timestamp;
      this.timestampNanos = this.timestamp.getTimeStamp();

      this.cause = cause;
      this.stackTrace = (stackTrace == null) ? "" : stackTrace;
      String causeStr = "";
      if (this.cause != null) {
         if (this.cause instanceof ServiceManagerException) {
            causeStr = ((ServiceManagerException)this.cause).getMessage();
         }
         else {
            causeStr = this.cause.toString();
         }
      }
      this.embeddedMessage = (embeddedMessage == null) ?
                                causeStr : embeddedMessage; // cause.toString() is <classname>:getMessage()
      this.transactionInfo = (transcationInfo == null) ? "<transaction/>" : transcationInfo;
      this.isServerSide = isServerSide;
      I_ServiceManagerExceptionHandler eh = exceptionHandler;
      if (eh != null)
         eh.newException(this);
   }
   
   /**
    * @return can be null
    */
   public final SysErrorCode getOriginalErrorCode() {
      if (this.embeddedMessage != null
          && this.embeddedMessage.startsWith("Original errorCode=")
          && this.embeddedMessage.length() > "Original errorCode=".length()) {
          String errorCodeStr = this.embeddedMessage.substring("Original errorCode=".length());
          return SysErrorCode.toErrorCode(errorCodeStr);
	   }
	   return null;
   }

   public final void changeErrorCode(SysErrorCode errorCodeEnum) {
      if (this.embeddedMessage == null || this.embeddedMessage.length() < 1) {
         this.embeddedMessage = "Original errorCode=" + this.errorCodeStr;
      }
      this.errorCodeEnum = (errorCodeEnum == null) ? SysErrorCode.INTERNAL_UNKNOWN : errorCodeEnum;
      this.errorCodeStr = this.errorCodeEnum.getErrorCode();
      
      I_ServiceManagerExceptionHandler eh = exceptionHandler;
      if (eh != null)
         eh.newException(this);
   }

   public final RunTimeSingleton getRunTimeSingleton() {
      return this.glob;
   }

   /**
    * @return The error code enumeration object, is never null
    */
   public final SysErrorCode getErrorCode() {
      if (this.errorCodeEnum == null) {
         try {
            this.errorCodeEnum = SysErrorCode.toErrorCode(this.errorCodeStr);
         }
         catch (IllegalArgumentException e) {
            this.errorCodeEnum = SysErrorCode.INTERNAL_UNKNOWN;
         }
      }
      return this.errorCodeEnum;
   }

   public final boolean isErrorCode(SysErrorCode code) {
      return this.errorCodeEnum == code;
   }

   public final String getErrorCodeStr() {
      return this.errorCodeStr;
   }

   public final String getNode() {
      return this.node;
   }

   public final String getLocation() {
      return this.location;
   }

   /** Overwrite the location */
   public final void setLocation(String location) {
      this.location = location;
   }

   public final String getLang() {
      return this.lang;
   }

   /**
    * Configurable with property <i>ServiceManagerException.logFormat</i>,
    * <i>ServiceManagerException.logFormat.internal</i> <i>ServiceManagerException.logFormat.resource</i> etc.
    * @return e.g. errorCode + ": " + getMessage() + ": " + getEmbeddedMessage()
    */
   public String getMessage() {
      Object[] arguments = {  (errorCodeStr==null) ? "" : errorCodeStr,  // {0}
                              (node==null) ? "" : node,                  // {1}
                              (location==null) ? "" : location,          // {2}
                              new Boolean(isServerSide()),               // {3}
                              getRawMessage(),                           // {4}
                              (versionInfo==null) ? "" : versionInfo,         // {5}
                              (timestamp==null) ? "" : timestamp.toString(),  // {6}
                              (stackTrace==null) ? "" : stackTrace,           // {7}
                              (embeddedMessage==null) ? "" : embeddedMessage, // {8}
                              (errorCodeEnum==null) ? "" : errorCodeEnum.getUrl() // {9}
                              // NOTE: IBM JDK 1.3 can't handle {} greater 9!
                              //(errorCodeEnum==null) ? "" : errorCodeEnum.getLongDescription(), // {9}
                              //(transactionInfo==null) ? "" : transactionInfo, // {10}
                              //(lang==null) ? "" : lang,                  // {11}
                              };

      boolean handleAsInternal = this.cause != null &&
              (
                 this.cause instanceof ServiceManagerException && ((ServiceManagerException)this.cause).isInternal() ||
                 this.cause instanceof NullPointerException ||
                 this.cause instanceof IllegalArgumentException ||
                 this.cause instanceof ArrayIndexOutOfBoundsException ||
                 this.cause instanceof StringIndexOutOfBoundsException ||
                 this.cause instanceof ClassCastException ||
                 this.cause instanceof OutOfMemoryError
              );

      try {
         if (isInternal() || handleAsInternal) {
            return MessageFormat.format(this.logFormatInternal, arguments);
         }
         else if (isResource()) {
            return MessageFormat.format(this.logFormatResource, arguments);
         }
         else if (isCommunication()) {
            return MessageFormat.format(this.logFormatCommunication, arguments);
         }
         else if (isUser()) {
            return MessageFormat.format(this.logFormatUser, arguments);
         }
         else if (errorCodeEnum == SysErrorCode.LEGACY) {
            return MessageFormat.format(this.logFormatLegacy, arguments);
         }
         else {
            return MessageFormat.format(this.logFormat, arguments);
         }
      }
      catch (IllegalArgumentException e) {
         log.error("Please check your formatting string for exceptions, usually set by 'ServiceManagerException.logFormat=...'" +
                    e.toString() +
                   "\nOriginal exception is: errorCode=" + errorCodeStr + " message=" + getRawMessage());
         if (isInternal() || handleAsInternal) {
            return MessageFormat.format(DEFAULT_LOGFORMAT_INTERNAL, arguments);
         }
         else if (isResource()) {
            return MessageFormat.format(DEFAULT_LOGFORMAT, arguments);
         }
         else if (isCommunication()) {
            return MessageFormat.format(DEFAULT_LOGFORMAT, arguments);
         }
         else if (isUser()) {
            return MessageFormat.format(DEFAULT_LOGFORMAT, arguments);
         }
         else if (errorCodeEnum == SysErrorCode.LEGACY) {
            return MessageFormat.format(DEFAULT_LOGFORMAT, arguments);
         }
         else {
            return MessageFormat.format(DEFAULT_LOGFORMAT, arguments);
         }
      }
   }

   /**
    * Get the original message text, it is prefixed by the current subversion revision number. 
    * For example: "#12702M Can't find class MyPlugin"
    * @return The original message text, never null
    */
   public final String getRawMessage() {
      if (super.getMessage()!=null && super.getMessage().startsWith("#")) {
         return super.getMessage();
      }
      String revision = "#" + (glob==null ? "" : glob.getRevisionNumber());
      return (super.getMessage()==null) ? revision : revision + " " + super.getMessage();
   }

   /**
    * A comma separated list with key/values containing detailed
    * information about the server environment
    */
   public final String getVersionInfo() {
      return this.versionInfo;
   }

   /**
    * Timestamp when exception was thrown
    * @return Never null
    */
   public final TimeStamp getTimestamp() {
      if (this.timestamp == null) {
         this.timestamp = new TimeStamp(this.timestampNanos);
      }
      return this.timestamp;
   }

   /**
    * The original exception, note that this is not serialized. 
    * @return The original exception or null
    */
   public final Throwable getEmbeddedException() {
      //return getCause(); // JDK 1.4 or better only
      return this.cause;
   }

   /**
    * @return The stack trace or null, e.g.
    * <pre>
    *  stackTrace= errorCode=internal.unknown message=Bla bla
    *    at com.ethercis.servicemanager.exceptions.ServiceManagerException.main(ServiceManagerException.java:488)
    * </pre>
    * The first line is the result from toString() and the following lines
    * are the stackTrace
    */
   public final String getStackTraceStr() {
      return this.stackTrace;
   }

   /**
    * @return The toString() of the embedded exception which is <classname>:getMessage()<br />
    *         or null if not applicable
    */
   public final String getEmbeddedMessage() {
      return this.embeddedMessage;
   }

   /**
    * @return Not defined yet
    */
   public final String getTransactionInfo() {
      return this.transactionInfo;
   }

   /**
    * @return true if the exception occured on server side, false if happened on client side
    */
   public final boolean isServerSide() {
      return this.isServerSide;
   }

   /**
    * @param serverSide true to mark the exception has occurred on server side, false if happened on client side
    */
   public final void isServerSide(boolean serverSide) {
      this.isServerSide = serverSide;
   }

   public boolean isInternal() {
      return this.errorCodeStr.startsWith("internal");
   }

   public boolean isResource() {
      return this.errorCodeStr.startsWith("resource");
   }

   public boolean isCommunication() {
      return this.errorCodeStr.startsWith("communication");
   }

   public boolean isUser() {
      return this.errorCodeStr.startsWith("user");
   }

   public boolean isTransaction() {
      return this.errorCodeStr.startsWith("transaction");
   }

   public String createStackTrace() {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      if (this.cause != null) {
         this.cause.printStackTrace(pw);
      }
      printStackTrace(pw);  // prints: toString() and in next lines the stack trace
      return sw.toString().trim();
   }

   public static String createStackTrace(Throwable e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      if (e.getCause() != null) {
         e.getCause().printStackTrace(pw);
      }
      e.printStackTrace(pw);  // prints: toString() and in next lines the stack trace
      return sw.toString().trim();
   }

   public static String createVersionInfo() {
      StringBuffer buf = new StringBuffer(512);
      buf.append("version=").append(RunTimeSingleton.instance().getVersion()).append(",");
      buf.append("revision=").append(RunTimeSingleton.instance().getRevisionNumber()).append(",");
      buf.append("os.name=").append(System.getProperty("os.name", "unknown").trim()).append(",");
      buf.append("os.version=").append(System.getProperty("os.version", "unknown").trim()).append(",");
      buf.append("java.vm.vendor=").append(System.getProperty("java.vm.vendor", "unknown").trim()).append(",");
      buf.append("java.vm.version=").append(System.getProperty("java.vm.version", "unknown").trim()).append(",");
      buf.append("os.arch=").append(System.getProperty("os.arch", "unknown").trim()).append(",");
      buf.append("build.timestamp=").append(RunTimeSingleton.instance().getBuildTimestamp()).append(",");
      buf.append("build.java.vendor=").append(RunTimeSingleton.instance().getBuildJavaVendor()).append(",");
      buf.append("build.java.version=").append(RunTimeSingleton.instance().getBuildJavaVersion()); // .append(",");
      return buf.toString();
   }

   /**
    * @deprecated Please use constructor which uses ErrorCode
    */
   public ServiceManagerException(String location, String message) {
      this((RunTimeSingleton)null, SysErrorCode.LEGACY, location, message, (Throwable)null);
   }

/**
    * Caution: The syntax is used by parseToString() to parse the stringified exception again.<br />
    * This is used by XmlRpc, see XmlRpcConnection.extractServiceManagerException()
    */
   public String toString() {
      //return getMessage();
      String text = "errorCode=" + getErrorCodeStr() + " message=" + getRawMessage();
      if (this.embeddedMessage != null && this.embeddedMessage.length() > 0) {
         text += " : " + embeddedMessage;
      }
      return text;
   }

   /**
    * Parsing what toString() produced
   * @param glob
   * @param toString The original exception
   * @param fallback The error code to use if 'toString' is unparsable
   */
   public static ServiceManagerException parseToString(RunTimeSingleton glob, String toString, SysErrorCode fallback) {
      String errorCode = toString;
      String reason = toString;
      int start = toString.indexOf("errorCode=");
      int end = toString.indexOf(" message=");
      if (start >= 0) {
         if (end >= 0) {
            try { errorCode = toString.substring(start+"errorCode=".length(), end); } catch(IndexOutOfBoundsException e1) {}
         }
         else {
            try { errorCode = toString.substring(start+"errorCode=".length()); } catch(IndexOutOfBoundsException e2) {}
         }
      }
      if (end >= 0) {
         try { reason = toString.substring(end+" message=".length()); } catch(IndexOutOfBoundsException e3) {}
      }
      try {
         return new ServiceManagerException(glob, SysErrorCode.toErrorCode(errorCode), "ServiceManagerException", reason);
      }
      catch (IllegalArgumentException e) {
         log.warn("Parsing exception string <" + toString + "> failed: " , e);
         return new ServiceManagerException(glob, fallback, "ServiceManagerException", toString);
      }
   }

   /**
    * @see #toXml(String)
    */
   public final String toXml() {
      return toXml((String)null);
   }

   /**
    * Create a XML representation of the Exception.
    * <pre>
    *   &lt;exception errorCode='resource.outOfMemory'>
    *      &lt;class>JavaClass&lt;/class>
    *      &lt;message>&lt;![cdata[  bla bla ]]>&lt;/message>
    *   &lt;/exception>
    * </pre>
    */
   public String toXml(String extraOffset) {
      StringBuffer sb = new StringBuffer(getMessage().length() + 256);
      if (extraOffset == null) extraOffset = "";
      String offset = Constants.OFFSET + extraOffset;

      sb.append(offset).append("<exception errorCode='").append(getErrorCodeStr()).append("'>");
      sb.append(offset).append(" <class>").append(getClass().getName()).append("</class>");
      sb.append(offset).append(" <isServerSide>").append(isServerSide()).append("</isServerSide>");
      sb.append(offset).append(" <node>").append(getNode()).append("</node>");
      sb.append(offset).append(" <location>").append(getLocation()).append("</location>");
      sb.append(offset).append(" <lang>").append(getLang()).append("</lang>");
      sb.append(offset).append(" <message><![CDATA[").append(getRawMessage()).append("]]></message>");
      sb.append(offset).append(" <versionInfo>").append(getVersionInfo()).append("</versionInfo>");
      sb.append(offset).append(" <timestamp>").append(getTimestamp().toString()).append("</timestamp>");
      sb.append(offset).append(" <stackTrace><![CDATA[").append(getStackTraceStr()).append("]]></stackTrace>");
      sb.append(offset).append(" <embeddedMessage><![CDATA[").append(getEmbeddedMessage()).append("]]></embeddedMessage>");
      //sb.append(offset).append(" <transactionInfo><![CDATA[").append(getTransactionInfo()).append("]]></transactionInfo>");
      sb.append(offset).append("</exception>");
      return sb.toString();
   }

   /**
    * Serialize the complete exception
    */
   public byte[] toByteArr() {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream(1024);
      PrintWriter out = new PrintWriter(byteOut);
      out.write(getErrorCodeStr());
      out.write(0);
      out.write(getNode());
      out.write(0);
      out.write(getLocation());
      out.write(0);
      out.write(getLang());
      out.write(0);
      out.write(getRawMessage());
      out.write(0);
      out.write(getVersionInfo());
      out.write(0);
      out.write(getTimestamp().toString());
      out.write(0);
      out.write(getStackTraceStr());
      out.write(0);
      out.write(getEmbeddedMessage());
      out.write(0);
      out.write(getTransactionInfo());
      out.write(0);
      out.write(""+isServerSide());
      out.write(0);
      out.flush();
      byte[] result = byteOut.toByteArray();
      return result;
   }

   public static ServiceManagerException parseByteArr(RunTimeSingleton glob, byte[] data) {
      return parseByteArr(glob, data, SysErrorCode.INTERNAL_UNKNOWN);
   }
   /**
    * Serialize the complete exception. 
    * Take care when changing!!!
    * Is used e.g. in CallbackServerUnparsed.c and XmlScriptInterpreter.java
    */
   public static ServiceManagerException parseByteArr(RunTimeSingleton glob, byte[] data, SysErrorCode fallback) {
      if (data == null)
         return new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, "ServiceManagerException", "Can't parse given serial ServiceManagerException data");
      int start = 0;
      int end = start;
      String errorCodeStr = null;
      String node = null;
      String location = null;
      String lang = null;
      String message = null;
      String versionInfo = null;
      String timestampStr = null;
      String stackTrace = null;
      String embeddedMessage = null;
      String transactionInfo = null;
      Boolean exceptionFromServer = new Boolean(true);

      try {
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         errorCodeStr = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         node = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         location = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         lang = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         message = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         versionInfo = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         timestampStr = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         stackTrace = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         embeddedMessage = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         transactionInfo = new String(data, start, end-start);

         start = end+1;
         for (end=start; end<data.length; end++)
            if (data[end] == 0)
               break;
         exceptionFromServer = new Boolean(new String(data, start, end-start));
      }
      catch (java.lang.StringIndexOutOfBoundsException e) {
         log.error("Receiving invalid format for ServiceManagerException in '" + Constants.toUtf8String(data) + "'");
      }
      SysErrorCode errorCode = (fallback == null) ? SysErrorCode.INTERNAL_UNKNOWN : fallback;
      try {
         errorCode = SysErrorCode.toErrorCode(errorCodeStr);
      }
      catch (Throwable e) {
         log.error("Receiving invalid errorCode in ServiceManagerException in '" + Constants.toUtf8String(data) + "', handling it logonservice " + errorCode.toString());
         message = "Receiving invalid errorCode in ServiceManagerException: Can't parse ServiceManagerException in method parseByteArr(). original message is '" + Constants.toUtf8String(data) + "'";
      }
      TimeStamp ti = new TimeStamp();
      try {
         ti = TimeStamp.valueOf(timestampStr);
      }
      catch (Throwable e) {
         log.debug("Receiving invalid timestamp in ServiceManagerException in '" + Constants.toUtf8String(data) + "'");
      }
      return new ServiceManagerException(glob, errorCode,
                               node, location, lang, message, versionInfo, ti,
                               stackTrace, embeddedMessage, transactionInfo, exceptionFromServer.booleanValue());
   }

   /**
    * If throwable is of type ServiceManagerException it is just casted (and location/message are ignored)
    * else if throwable is one if IllegalArgumentException, NullpointerException or OutOfMemoryError
    * it is converted to an ServiceManagerException with corresponding ErrorCode
    * otherwise the ErrorCode is INTERNAL_UNKNOWN
    * @param location null if not of interest
    * @param message null if not of interest
    * @param throwable Any exception type you can think of
    * @return An exception of type ServiceManagerException
    */
   public static ServiceManagerException convert(RunTimeSingleton glob, String location, String message, Throwable throwable) {
      return convert(glob, SysErrorCode.INTERNAL_UNKNOWN, location, message, throwable);
   }

   /**
    * @param errorCodeEnum is the fallback error code
    */
   public static ServiceManagerException convert(RunTimeSingleton glob, SysErrorCode errorCodeEnum, String location, String message, Throwable throwable) {
      if (throwable instanceof ServiceManagerException) {
         return (ServiceManagerException)throwable;
      }
      else if (throwable instanceof NullPointerException) {
         return new ServiceManagerException(glob, SysErrorCode.INTERNAL_NULLPOINTER, location, message, throwable);
      }
      else if (throwable instanceof IllegalArgumentException) {
         return new ServiceManagerException(glob, SysErrorCode.INTERNAL_ILLEGALARGUMENT, location, message, throwable);
      }
      else if (throwable instanceof ArrayIndexOutOfBoundsException) {
         return new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, location, message, throwable);
      }
      else if (throwable instanceof StringIndexOutOfBoundsException) {
         return new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, location, message, throwable);
      }
      else if (throwable instanceof ClassCastException) {
         return new ServiceManagerException(glob, SysErrorCode.INTERNAL_UNKNOWN, location, message, throwable);
      }
      else if (throwable instanceof OutOfMemoryError) {
         return new ServiceManagerException(glob, SysErrorCode.RESOURCE_OUTOFMEMORY, location, message, throwable);
      }
      else {
         return new ServiceManagerException(glob, errorCodeEnum, location, message, throwable);
      }
   }

   /**
    * Overwrite the formatting of internal logs
    * (the env property -ServiceManagerException.logFormat.internal)
    */
   public void setLogFormatInternal(String logFormatInternal) {
      this.logFormatInternal = logFormatInternal;
   }
   

   public static ServiceManagerException transformCallbackException(ServiceManagerException e) {
      // TODO: Marcel: For the time being the client has the chance
      // to force requeueing by sending a USER_UPDATE_HOLDBACK which will lead
      // to a COMMUNICATION exception behaviour
      if (SysErrorCode.USER_UPDATE_HOLDBACK.toString().equals(e.getErrorCode().toString())) {
         // Will set dispatcherActive==false
         ServiceManagerException ret = new ServiceManagerException(e.getRunTimeSingleton(),
              SysErrorCode.COMMUNICATION_USER_HOLDBACK, "transformCallbackException", 
              e.getEmbeddedMessage(), e);
         ret.isServerSide(e.isServerSide());
         return ret;
      }
   
      // WE ONLY ACCEPT ErrorCode.USER... FROM CLIENTS !
      if (e.isUser())
         return e;
   
      
      // and server side communication problems (how to assure if from server?)
      //if (e.isCommunication() && e.isServerSide())
      //it can also be thrown by client side, if for example the client side SOCKET callback server is marked shutdown
      if (e.isCommunication())
         return e;
   
      // The SOCKET protocol plugin throws this when a client has shutdown its callback server
      //if (ServiceManagerException.getErrorCode() == ErrorCode.COMMUNICATION_NOCONNECTION_CALLBACKSERVER_NOTAVAILABLE)
      //   throw ServiceManagerException;
   
      return new ServiceManagerException(e.getRunTimeSingleton(), SysErrorCode.USER_UPDATE_ERROR, e.getLocation(), e.getRawMessage(), e);
   }


   /**
    * @return Returns the exceptionHandler.
    */
   public static I_ServiceManagerExceptionHandler getExceptionHandler() {
      return exceptionHandler;
   }

   /**
    * @param exceptionHandler The exceptionHandler to set.
    */
   public static void setExceptionHandler(
         I_ServiceManagerExceptionHandler exceptionHandler) {
      ServiceManagerException.exceptionHandler = exceptionHandler;
   }

   public boolean isCleanupSession() {
      return cleanupSession;
   }

   public void setCleanupSession(boolean cleanupSession) {
      this.cleanupSession = cleanupSession;
   }
}

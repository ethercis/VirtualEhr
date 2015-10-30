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


package com.ethercis.servicemanager.common.def;

import com.ethercis.servicemanager.common.ReplaceVariable;

import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * This class holds an enumeration error codes. 
 * <p>
 * If you need new error code add it here following the same schema.
 * </p>
 * <p>
 * The documentation is created by examining this class with it links.
 * </p>
 */
public final class SysErrorCode implements java.io.Serializable
{
   private static final long serialVersionUID = 6926365721931493917L;
   private final static TreeMap errorCodeMap = new TreeMap(); // The key is the 'errorCode' String and the value is an 'ErrorCode' instance
   private final String errorCode;
   private final String description;
   private final int httpCode;
   private final ResourceInfo[] resourceInfos;

   ////////// BEGIN /////////// Add the error code instances here ////////////////////
   public static final SysErrorCode LEGACY = new SysErrorCode("legacy",
         "This error code marks all old style Exceptions until they are ported to the new behaviour.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "admin.errorcodes", "admin.errorcodes")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode INTERNAL = new SysErrorCode("internal",
         "These category is an internal exception, usually a Java runtime exception, please report the issue to the maintenance group.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "admin.errorcodes", "admin.errorcodes")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR         
      );

   public static final SysErrorCode INTERNAL_UNKNOWN = new SysErrorCode("internal.unknown",
         "This is an unknown and unexpected error, usually a Java runtime exception, please report the issue to the maintenance group.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "admin.errorcodes", "admin.errorcodes")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode INTERNAL_NULLPOINTER = new SysErrorCode("internal.nullpointer",
         "A null pointer is an internal programming error, please report the issue to the maintenance group.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode INTERNAL_ILLEGALARGUMENT = new SysErrorCode("internal.illegalArgument",
         "An illegal argument is an internal programming error, please report the issue to the maintenance group.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode INTERNAL_INTERRUPTED = new SysErrorCode("internal.interrupted",
         "An unexpected InterruptedException for a thread occurred, please report the issue to the maintenance group.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
   
      );

   public static final SysErrorCode INTERNAL_NOTIMPLEMENTED = new SysErrorCode("internal.notImplemented",
         "The feature is not implemented yet.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_NOT_IMPLEMENTED
      );

   public static final SysErrorCode INTERNAL_CONNECTIONFAILURE = new SysErrorCode("internal.connectionFailure",
         "An internal error occurred, we were not able to access the server handle.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect"),
         },
         HttpServletResponse.SC_NOT_FOUND
   
      );

   public static final SysErrorCode INTERNAL_ILLEGALSTATE = new SysErrorCode("internal.illegalState",
         "The state of an object is not allowed.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_CONFLICT
      );

   public static final SysErrorCode INTERNAL_DISCONNECT = new SysErrorCode("internal.disconnect",
         "An internal error occurred when processing a disconnect() request.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.disconnect", "interface.disconnect")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );


   public static final SysErrorCode RESOURCE = new SysErrorCode("resource",
         "This category is for resource problems like too low memory. It can usually be fixed by the administrator",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_MAINTENANCE = new SysErrorCode("resource.maintenance",
	         "The resource is under maintenance, please try again later",
	         new ResourceInfo[] {
	         },
	         HttpServletResponse.SC_SERVICE_UNAVAILABLE
	      );

   public static final SysErrorCode RESOURCE_OUTOFMEMORY = new SysErrorCode("resource.outOfMemory",
         "The JVM has no more RAM memory, try increasing it like 'java -Xms18M -Xmx256M ...'",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.URL, "Increasing JVM heap", "http://java.sun.com/docs/hotspot/ism.html")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_TOO_MANY_THREADS = new SysErrorCode("resource.tooManyThreads",
         "The number of threads used is exceeded, try increasing the number of threads in the properties",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "Increasing the number of threads", "queue.jdbc.commontable")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_EXHAUST = new SysErrorCode("resource.exhaust",
         "A resource of your system exhausted",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );


   public static final SysErrorCode RESOURCE_UNAVAILABLE = new SysErrorCode("resource.unavailable",
         "The resource is not available (e.g. it is shutdown)",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode RESOURCE_TEMPORARY_UNAVAILABLE = new SysErrorCode("resource.temporary.unavailable",
         "The server has a temporary resource timeout, please try again.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode RESOURCE_DB_UNAVAILABLE = new SysErrorCode("resource.db.unavailable",
         "There is no connection to a backend database using JDBC",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "queue.jdbc.hsqldb", "queue.jdbc.hsqldb"),
            new ResourceInfo(ResourceInfo.API, "JDBC connection management", "JdbcConnectionPool")
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode RESOURCE_ADMIN_UNAVAILABLE = new SysErrorCode("resource.admin.unavailable",
         "The administrative support is switched off for this instance",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "admin.messages", "admin.messages")
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode RESOURCE_CONFIGURATION = new SysErrorCode("resource.configuration",
         "Please check your configuration.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "client.configuration", "client.configuration"),
            new ResourceInfo(ResourceInfo.REQ, "The CORBA protocol plugin", "protocol.corba.JacORB")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_CONFIGURATION_CONNECT = new SysErrorCode("resource.configuration.connect",
         "Please check your connection configuration settings or the availability of a remote server.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "client.configuration", "client.configuration"),
            new ResourceInfo(ResourceInfo.REQ, "The EMAIL protocol plugin", "protocol.email")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_CONFIGURATION_SERVICEFAILED = new SysErrorCode("resource.configuration.serviceFailed",
         "A service required couldn't be loaded, please check your configuration.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "The runlevel manager howto", "engine.runlevel.howto"),
            new ResourceInfo(ResourceInfo.REQ, "The runlevel manager", "engine.runlevel")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_CONFIGURATION_XML = new SysErrorCode("resource.configuration.xml",
         "Your XML / XSL configuration needs to be adjusted, please check your configuration.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.URL, "Changing XML/XSL implementation", "xml-parser")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_CONFIGURATION_SSLSOCKET = new SysErrorCode("resource.configuration.sslSocket",
         "A SOCKET plugin required couldn't be loaded, please check your (ssl) configuration.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "The runlevel manager howto", "engine.runlevel.howto"),
            new ResourceInfo(ResourceInfo.REQ, "The SOCKET protocol specification", "protocol.socket")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_CONFIGURATION_ADDRESS = new SysErrorCode("resource.configuration.address",
         "A remote address you passed is invalid, please check your configuration.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.API, "client queue configuration", "AddressBase")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_FILEIO = new SysErrorCode("resource.fileIO",
         "A file access failed.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_FILEIO_FILELOST = new SysErrorCode("resource.fileIO.fileLost",
         "A file disappeared, access failed.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode RESOURCE_CLUSTER_NOTAVAILABLE = new SysErrorCode("resource.cluster.notAvailable",
         "A remote cluster node is not reachable.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "cluster requirement", "cluster")
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode RESOURCE_CLUSTER_CIRCULARLOOP = new SysErrorCode("resource.cluster.circularLoop",
         "A message loops between cluster nodes and can't reach its destination, please check the destination cluster node name.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "cluster PtP requirement", "cluster.PtP"),
            new ResourceInfo(ResourceInfo.REQ, "cluster requirement", "cluster")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode COMMUNICATION = new SysErrorCode("communication",
         "This category is related to communication problems between client and server.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_GONE
      );

   // Is thrown by server but we want the client to behave like a communication problem (re-connect)
   public static final SysErrorCode COMMUNICATION_RESOURCE_TEMPORARY_UNAVAILABLE = new SysErrorCode("communication.resource.temporary.unavailable",
         "The server has a temporary resource timeout, please try again.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode COMMUNICATION_NOCONNECTION = new SysErrorCode("communication.noConnection",
         "A specific remote connection throws an exception on invocation.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_GONE
      );

   public static final SysErrorCode COMMUNICATION_TIMEOUT = new SysErrorCode("communication.timeout",
         "The socket call blocked until a timeout occurred.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_GONE
      );
   
   public static final SysErrorCode COMMUNICATION_RESPONSETIMEOUT = new SysErrorCode("communication.responseTimeout",
        "A method call blocked when waiting on the ACK/NAK return message.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_GONE
      );


   public static final SysErrorCode COMMUNICATION_FORCEASYNC = new SysErrorCode("communication.forceAsync",
         "Thrown if a ping is called but we can't afford to block until it succeeds.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode COMMUNICATION_NOCONNECTION_SERVERDENY = new SysErrorCode("communication.noConnection.serverDeny",
         "Thrown by the server if no connection is accepted, usually on startup when the server is not ready for it (standby mode).",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "run level requirement", "engine.runlevel")
         },
         HttpServletResponse.SC_UNAUTHORIZED         
      );

   public static final SysErrorCode COMMUNICATION_NOCONNECTION_CALLBACKSERVER_NOTAVAILABLE = new SysErrorCode("communication.noConnection.callbackServer.notavailable",
         "The callback server is not available, this usually happens when the callback server is shutdown on client side",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.update", "interface.update")
         },
         HttpServletResponse.SC_SEE_OTHER
      );

   public static final SysErrorCode COMMUNICATION_NOCONNECTION_POLLING = new SysErrorCode("communication.noConnection.polling",
         "The remote connection is not established and we are currently polling for it.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect"),
            new ResourceInfo(ResourceInfo.API, "client queue configuration", "Address"),
            new ResourceInfo(ResourceInfo.API, "callback queue configuration", "CallbackAddress")
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE 
      );

   public static final SysErrorCode COMMUNICATION_NOCONNECTION_DEAD = new SysErrorCode("communication.noConnection.dead",
         "The remote connection is not established and we have given up to poll for it.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect"),
            new ResourceInfo(ResourceInfo.REQ, "client.configuration", "client.configuration"),
            new ResourceInfo(ResourceInfo.REQ, "client.failsafe", "client.failsafe"),
            new ResourceInfo(ResourceInfo.API, "client queue configuration", "Address"),
            new ResourceInfo(ResourceInfo.API, "callback queue configuration", "CallbackAddress")
         },         
         HttpServletResponse.SC_GONE

      );

   // The dispatch framework reacts specific for communication exceptions
   public static final SysErrorCode COMMUNICATION_USER_HOLDBACK = new SysErrorCode("communication.user.holdback",
         "See USER_UPDATE_HOLDBACK.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );

   public static final SysErrorCode USER = new SysErrorCode("user",
         "This category stands for wrong usage by the programmer.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   // TODO: Replace by finer adjusting possibilities (like retry timeouts etc.)
   public static final SysErrorCode USER_UPDATE_HOLDBACK = new SysErrorCode("user.update.holdback",
         "You can throw this on client side in your update() method: Like this the server queues the message and sets the dispatcActive to false. You need to manually activate the dispatcher again.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode USER_UPDATE_DEADMESSAGE = new SysErrorCode("user.update.deadMessage",
	         "You can throw this on client side in your update() method: Like this the server publishes the message logonservice dead letter and removes it from the callback queue.",
	         new ResourceInfo[] {
	         },
	         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
	      );

   public static final SysErrorCode USER_WRONG_API_USAGE = new SysErrorCode("user.wrongApiUsage",
         "Please check your client code.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode USER_CONFIGURATION = new SysErrorCode("user.configuration",
         "Login failed due to configuration problems.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "client.configuration", "client.configuration"),
            new ResourceInfo(ResourceInfo.REQ, "client.failsafe", "client.failsafe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode USER_VERSION = new SysErrorCode("user.version",
	         "Your version is outdated, login failed.",
	         new ResourceInfo[] {
	         },
	         HttpServletResponse.SC_FORBIDDEN
	      );

   public static final SysErrorCode USER_CONFIGURATION_MAXSESSION = new SysErrorCode("user.configuration.maxSession",
         "Login failed due to maximum sessions of a authenticate reached.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "client.failsafe", "client.failsafe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_FORBIDDEN
      );

   public static final SysErrorCode USER_CONFIGURATION_IDENTICALCLIENT = new SysErrorCode("user.configuration.identicalClient",
         "Login failed, reconnect for other client instance on existing public session is switched off, see connect QoS reconnectSameClientOnly=true setting.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "client.failsafe", "client.failsafe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_FORBIDDEN
      );

   public static final SysErrorCode USER_SECURITY = new SysErrorCode("user.security",
	         "General security exception, authentication or authorization.",
	         new ResourceInfo[] {
	         },
	         HttpServletResponse.SC_UNAUTHORIZED
	      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION = new SysErrorCode("user.security.authentication",
	         "Login failed due to some reason.",
	         new ResourceInfo[] {
	            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
	         },
	         HttpServletResponse.SC_UNAUTHORIZED
	      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION_ACCESSDENIED = new SysErrorCode("user.security.authentication.accessDenied",
         "Login failed due to missing privileges.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_UNAUTHORIZED
      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION_ACCESSDENIED_UNKNOWNLOGINNAME = new SysErrorCode("user.security.authentication.accessDenied.unknownLoginName",
	         "Login failed due to unkown login name.",
	         new ResourceInfo[] {
	            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
	         },
	         HttpServletResponse.SC_UNAUTHORIZED
	      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION_ACCESSDENIED_WRONGPASSWORD = new SysErrorCode("user.security.authentication.accessDenied.wrongPassword",
	         "Login failed due to wrong password.",
	         new ResourceInfo[] {
	            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
	         },
	         HttpServletResponse.SC_UNAUTHORIZED
	      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION_INACTIVE = new SysErrorCode("user.security.authentication.inactive",
	         "Login failed, the account is not active anymore.",
	         new ResourceInfo[] {
	            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
	         },
	         HttpServletResponse.SC_UNAUTHORIZED
	      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION_EXPIRED = new SysErrorCode("user.security.authentication.expired",
	         "Login failed, the account is expired.",
	         new ResourceInfo[] {
	            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
	         },
	         HttpServletResponse.SC_UNAUTHORIZED
	      );

   public static final SysErrorCode USER_SECURITY_AUTHENTICATION_ILLEGALARGUMENT = new SysErrorCode("user.security.authentication.illegalArgument",
         "Login failed due to illegal arguments.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_UNAUTHORIZED
      );

   public static final SysErrorCode USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED = new SysErrorCode("user.security.authorization.notAuthorized",
         "Login failed due to missing privileges.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_UNAUTHORIZED
      );

   public static final SysErrorCode USER_UPDATE_ERROR = new SysErrorCode("user.update.error",
         "Exception thrown by client on callback update invocation.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.update", "interface.update")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode USER_UPDATE_INTERNALERROR = new SysErrorCode("user.update.internalError",
         "Unexpected exception thrown by client code on programming error.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.update", "interface.update")
         },
         HttpServletResponse.SC_INTERNAL_SERVER_ERROR
      );

   public static final SysErrorCode USER_UPDATE_ILLEGALARGUMENT = new SysErrorCode("user.update.illegalArgument",
         "The update method was invoked without useful data.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.update", "interface.update")
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_ILLEGALARGUMENT = new SysErrorCode("user.illegalArgument",
         "You have invoked a server method with illegal arguments.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_UPDATE_SECURITY_AUTHENTICATION_ACCESSDENIED = new SysErrorCode("user.update.security.authentication.accessDenied",
         "The update method was invoked with an invalid callback session ID.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.update", "interface.update")
         },
         HttpServletResponse.SC_UNAUTHORIZED
      );

   public static final SysErrorCode USER_PUBLISH_READONLY = new SysErrorCode("user.publish.readonly",
         "You published a message which is marked logonservice readonly.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "engine.qos.publish.readonly", "engine.qos.publish.readonly"),
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_SUBSCRIBE_ID = new SysErrorCode("user.subscribe.id",
         "Your subscription tries to pass an illegal subscriptionId.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.subscribe", "interface.subscribe"),
            new ResourceInfo(ResourceInfo.REQ, "engine.qos.subscribe.id", "engine.qos.subscribe.id")
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_SUBSCRIBE_NOCALLBACK = new SysErrorCode("user.subscribe.noCallback",
         "You try to subscribe to a topic but have no callback registered on connect.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect"),
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_OID_UNKNOWN = new SysErrorCode("user.oid.unknown",
         "You passed a message oid which is not known.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_JDBC_INVALID = new SysErrorCode("user.jdbc.invalid",
         "Illegal JDBC query or access.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "engine.service.rdbms", "engine.service.rdbms")
         },
         HttpServletResponse.SC_METHOD_NOT_ALLOWED
      );

   public static final SysErrorCode USER_CONNECT = new SysErrorCode("user.connect",
         "Your connection request could not be handled, check your QoS",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_CONNECT_MULTIPLE = new SysErrorCode("user.connect.multiple",
         "You have invoked connect() multiple times",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_NOT_CONNECTED = new SysErrorCode("user.notConnected",
         "Your operation is not possible, please login with connect() first",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.connect", "interface.connect")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_PUBLISH = new SysErrorCode("user.publish",
         "Your published message could not be handled, check your QoS",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.publish", "interface.publish")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_PTP_UNKNOWNSESSION = new SysErrorCode("user.ptp.unknownSession",
         "You have send a point to point message to a specific user session but the receiver is not known.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.publish", "interface.publish")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_PTP_UNKNOWNDESTINATION = new SysErrorCode("user.ptp.unknownDestination",
         "You have send a point to point message but the receiver is not known and <destination forceQueuing='true'> is not set.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.publish", "interface.publish")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_PTP_UNKNOWNDESTINATION_SESSION = new SysErrorCode("user.ptp.unknownDestinationSession",
         "You have send a point to point message but the receiver session is not known.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.publish", "interface.publish")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_PTP_DENIED = new SysErrorCode("user.ptp.denied",
           "You have send a point to point message but the receiver session does not accept PtP.",
           new ResourceInfo[] {
               new ResourceInfo(ResourceInfo.REQ, "interface.connect",
                   "interface.connect")
         },
         HttpServletResponse.SC_UNAUTHORIZED
      );

   public static final SysErrorCode USER_MESSAGE_INVALID = new SysErrorCode("user.message.invalid",
         "Usually thrown by a mime plugin if your MIME type does not fit to your message content, e.g. mime='text/xml' and content='Nice weather'.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "mime.plugin.accessfilter", "mime.plugin.accessfilter")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_QUERY_INVALID = new SysErrorCode("user.query.invalid",
         "You have invoked get(), subscribe(), unSubscribe() or erase() with an illegal query syntax.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.get", "interface.get"),
            new ResourceInfo(ResourceInfo.REQ, "interface.subscribe", "interface.subscribe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.unSubscribe", "interface.unSubscribe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.erase", "interface.erase"),
            new ResourceInfo(ResourceInfo.API, "query syntax", "QueryKeyData")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_ADMIN_INVALID = new SysErrorCode("user.admin.invalid",
         "Your administrative request was illegal.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "admin.messages", "admin.messages")
         },
         HttpServletResponse.SC_UNAUTHORIZED
      );

   public static final SysErrorCode USER_QUERY_TYPE_INVALID = new SysErrorCode("user.query.type.invalid",
         "You have invoked get(), subscribe(), unSubscribe() or erase() with an illegal query type, try EXACT or XPATH.",
         new ResourceInfo[] {
            new ResourceInfo(ResourceInfo.REQ, "interface.get", "interface.get"),
            new ResourceInfo(ResourceInfo.REQ, "interface.subscribe", "interface.subscribe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.unSubscribe", "interface.unSubscribe"),
            new ResourceInfo(ResourceInfo.REQ, "interface.erase", "interface.erase"),
            new ResourceInfo(ResourceInfo.API, "query syntax", "QueryKeyData")
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   public static final SysErrorCode USER_CLIENTCODE = new SysErrorCode("user.clientCode",
         "You may use this error code in your client implementation to throw your own exceptions.",
         new ResourceInfo[] {
         },
         HttpServletResponse.SC_NOT_ACCEPTABLE
      );

   //** Add by Noppadol to support application exception 
   //
   public static final SysErrorCode USER_NOT_MODIFIED = new SysErrorCode("user.notModified",
	"Usually throw when applcation cannot create or update",
	new ResourceInfo[] {
   	},
	HttpServletResponse.SC_NOT_MODIFIED
    );
   
    //** End
   
 //** Add by Mogens for patient allready exist check
   //
   public static final SysErrorCode USER_ALREADY_EXIST = new SysErrorCode("user.alreadyExist",
   "Patient already exit without consent - Delete the existing patient",
   new ResourceInfo[] {
      },
   HttpServletResponse.SC_FOUND
    );
   
    //** End
   

   /**
    * @exception IllegalArgumentException if the given errorCode is null
    */
   private SysErrorCode(String errorCode, String description, ResourceInfo[] resourceInfos, int httpcode) {
      if (errorCode == null)
         throw new IllegalArgumentException("Your given errorCode is null");
      this.errorCode = errorCode;
      this.description = (description == null) ? "" : description;
      this.resourceInfos = (resourceInfos == null) ? new ResourceInfo[0] : resourceInfos;
      this.httpCode = httpcode;
      errorCodeMap.put(errorCode, this);
   }

   /**
    * Return a human readable string of the errorCode and description
    * @return never null
    */
   public String toString() {
      return "errorCode=" + this.errorCode + ": " + this.description;
   }

   /**
    * Returns 'true' if this error code is a 'child' of the error code
    * specified in baseCode. It follows the name convention of the error
    * code. For example USER_SECURITY_AUTHENTICATION_DENIED
    * would be of type (logonservice it is a subtype) of USER_SECURITY_AUTHENTICATION
    * If one of the error codes code name is null, false is returned.
    * @param baseCode the base ErrorCode to check against. If null, false is
    * returned. 
    * @return
    */
   public final boolean isOfType(SysErrorCode baseCode) {
	   if (baseCode == null)
		   return false;
	   String baseCodeTxt = baseCode.getErrorCode();
	   if (this.errorCode == null || baseCodeTxt == null)
		   return false;
	   return this.errorCode.startsWith(baseCodeTxt);
   }
   /**
    * Returns the errorCode string. 
    * @return never null
    */
   public String getErrorCode() {
      return this.errorCode;
   }

   /**
    * Returns the description of the errorCode. 
    * @return never null
    */
   public String getDescription() {
      return this.description;
   }
   
   /**
    * returns the http code of this error
    * @see HttpServletResponse
    * @return never null
    */
   public int getHttpCode(){
	   return this.httpCode;
   }

   /**
    * Returns the description of the errorCode including the online link with further explanations. 
    * @return never null
    */
   public String getLongDescription() {
      return this.description + " -> " + getUrl();
   }

   /**
    * The link to find more information about this problem
    */
   public String getUrl() {
      return "http://.../doc/requirements/admin.errorcodes.listing.html#" + getErrorCode();
   }

   /**
    * Return resource info object telling us where to find more information
    * on this errorCode
    */
   public ResourceInfo[] getResourceInfos() {
      return this.resourceInfos;
   }
   
   /**
    * @return The top level category like 'internal'
    */
   public static SysErrorCode getCategory(SysErrorCode errorCode) {
      if (errorCode == null || errorCode.errorCode == null) return SysErrorCode.INTERNAL;
      int index = errorCode.errorCode.indexOf(".");
      if (index == -1) return errorCode; // is already a top level
      if (index == 0) return SysErrorCode.INTERNAL; // ".blabla" shouldn't appear (no leading dots)
      String top = errorCode.errorCode.substring(0,index);
      return toErrorCode(top);
   }

   /**
    * @return The top level category like 'internal'
    */
   public static SysErrorCode getCategory(String errorCode) {
      if (errorCode == null) return SysErrorCode.INTERNAL;
      int index = errorCode.indexOf(".");
      if (index == -1) return SysErrorCode.toErrorCode(errorCode); // is already a top level
      if (index == 0) return SysErrorCode.INTERNAL; // ".blabla" shouldn't appear (no leading dots)
      String top = errorCode.substring(0,index);
      return toErrorCode(top);
   }

   /**
    * Returns the ErrorCode object for the given String error code. 
    * @param errorCode The String code to lookup
    * @return The enumeration object for this errorCode
    * @exception IllegalArgumentException if the given errorCode is invalid
    */
   public static final SysErrorCode toErrorCode(String errorCode) throws IllegalArgumentException {
      if (errorCode == null) {
         throw new IllegalArgumentException("ErrorCode: The given errorCode=" + errorCode + " is null");
      }
      Object entry = errorCodeMap.get(errorCode);
      if (entry == null)
         throw new IllegalArgumentException("ErrorCode: The given errorCode=" + errorCode + " is unknown");
      return (SysErrorCode)entry;
   }

   /**
    * Returns the ErrorCode object for the given String error code. 
    * @param errorCode The String code to lookup
    * @param fallback code to use if errorCode is not known
    * @return The enumeration object for this errorCode
    */
   public static final SysErrorCode toErrorCode(String errorCode, SysErrorCode fallback) {
      if (errorCode == null) {
         return fallback;
      }
      Object entry = errorCodeMap.get(errorCode);
      if (entry == null)
         return fallback;
      return (SysErrorCode)entry;
   }

   public final boolean equals(SysErrorCode other) {
      return this.errorCode.equals(other.getErrorCode());
   }

   /**
    * Dump a plain list of all errorCodes. 
    * @return The list with each errorCode in a new line
    */
   public static String toPlainList() {
      StringBuffer sb = new StringBuffer(2560);
      String offset = "\n";
      java.util.Date date = new java.util.Date();
      String d = new java.sql.Timestamp(date.getTime()).toString();
      sb.append("# ErrorCode listing " + d);
      Iterator it = errorCodeMap.keySet().iterator();
      while (it.hasNext()) {
         String code = (String)it.next();
         SysErrorCode errorCode = (SysErrorCode)errorCodeMap.get(code);
         sb.append(offset).append(errorCode.getErrorCode());
      }
      return sb.toString();
   }

   /**
    * Generate a HTML table listing of all error codes. 
    * @return The HTML markup
    */
   public static String toHtmlTable() {
      StringBuffer sb = new StringBuffer(2560);
      String offset = "\n ";
      sb.append(offset).append("<table border='1'>");
      Iterator it = errorCodeMap.keySet().iterator();
      sb.append(offset).append("<tr><th>Error Code</th><th>Description</th><th>See</th></tr>");
      while (it.hasNext()) {
         sb.append(offset).append("<tr>");
         String code = (String)it.next();
         SysErrorCode errorCode = (SysErrorCode)errorCodeMap.get(code);

         sb.append(offset).append(" <td><a name='").append(errorCode.getErrorCode()).append("'></a>");
         sb.append(errorCode.getErrorCode()).append("</td>");

         String desc = ReplaceVariable.replaceAll(errorCode.getDescription(),
                              "&", "&amp;");
         desc = ReplaceVariable.replaceAll(errorCode.getDescription(),
                              "<", "&lt;");
         sb.append(offset).append(" <td>").append(desc).append("</td>");
         
         ResourceInfo[] resourceInfos = errorCode.getResourceInfos();
         sb.append(offset).append(" <td>");
         for (int i=0; i<resourceInfos.length; i++) {
            if (i>0)
               sb.append("<br />");

            String resource = ReplaceVariable.replaceAll(resourceInfos[i].getResource(),
                              "<", "&lt;"); 
            String url=null;

            if (ResourceInfo.REQ.equals(resourceInfos[i].getType()))
               url="http://.../doc/requirements/"+resource+".html";
            else if (ResourceInfo.URL.equals(resourceInfos[i].getType()))
               url= resource;
            else if (ResourceInfo.API.equals(resourceInfos[i].getType())) {
               String replace = ReplaceVariable.replaceAll(resource, ".", "/"); 
               url="http://.../doc/api/"+replace+".html";
            }
            else {
               System.out.println("Ignoring unknown resource type '" + resourceInfos[i].getType() + "'");
               continue;
            }

            sb.append("<a href='").append(url).append("' target='others'>");
            sb.append(resourceInfos[i].getLabel()).append("</a>");
         }
         if (resourceInfos.length == 0)
            sb.append("-");

         sb.append("</td>");
         sb.append(offset).append("</tr>");
      }
      sb.append(offset).append("</table>");

      return sb.toString();
   }

   public static String toRequirement() {
      String req=
         "<?xml version='1.0' encoding='ISO-8859-1' ?>\n"+
         "<!DOCTYPE requirement SYSTEM 'requirement.dtd'>\n" +
         "<requirement id='admin.errorcodes.listing' type='NEW' prio='LOW' status='CLOSED'>\n" +
         "   <topic>error code reference</topic>\n" +
         "   <description>\n" +
         toHtmlTable() +
         "\nGenerated by ErrorCode\n" +
         "   </description>\n" +
         "</requirement>";
      return req;
   }

   public static String toXmlAll(String extraOffset) {
      StringBuffer sb = new StringBuffer(2560);
      String offset = "\n ";
      if (extraOffset == null) extraOffset = "";
      offset += extraOffset;

      sb.append(offset).append("<ErrorCodes>");
      Iterator it = errorCodeMap.keySet().iterator();
      while (it.hasNext()) {
         String code = (String)it.next();
         SysErrorCode errorCode = (SysErrorCode)errorCodeMap.get(code);
         sb.append(errorCode.toXml(" "));
      }
      sb.append(offset).append("</ErrorCodes>");

      return sb.toString();
   }

   public String toXml(String extraOffset) {
      StringBuffer sb = new StringBuffer(256);
      String offset = "\n ";
      if (extraOffset == null) extraOffset = "";
      offset += extraOffset;

      sb.append(offset).append("<errorCode id='").append(getErrorCode()).append("'>");
      sb.append(offset).append(" <description>").append(getLongDescription()).append("</description>");
      for (int i=0; i<resourceInfos.length; i++)
         sb.append(resourceInfos[i].toXml(extraOffset+" "));
      sb.append(offset).append("</errorCode>");

      return sb.toString();
   }

   /**
    * This code is a helper for serialization so that after
    * deserial the check
    *   <pre>ErrorCode.INTERNAL_UNKNOWN == internalUnknownInstance</pre>
    * is still usable (the singleton is assured when deserializing)
    * <br />
    * See inner class SerializedForm
    */
   public Object writeReplace() throws java.io.ObjectStreamException {
      return new SerializedForm(this.getErrorCode());
   }
   /**
    * A helper class for singleton serialization. 
    */
   private static class SerializedForm implements java.io.Serializable {
      private static final long serialVersionUID = 1L;
      String errorCode;
      SerializedForm(String errorCode) { this.errorCode = errorCode; }
      Object readResolve() throws java.io.ObjectStreamException {
         return SysErrorCode.toErrorCode(errorCode);
      }
   }



   public static void verifySerialization() {
      String fileName = "ErrorCode.ser";
      SysErrorCode pOrig = SysErrorCode.USER_PTP_UNKNOWNSESSION;
      {

         try {
            java.io.FileOutputStream f = new java.io.FileOutputStream(fileName);
            java.io.ObjectOutputStream objStream = new java.io.ObjectOutputStream(f);
            objStream.writeObject(pOrig);
            objStream.flush();
            System.out.println("SUCCESS written " + pOrig.toString());
         }
         catch (Exception e) {
            System.err.println("ERROR: " + e.toString());
         }
      }

      SysErrorCode pNew = null;
      {

         try {
            java.io.FileInputStream f = new java.io.FileInputStream(fileName);
            java.io.ObjectInputStream objStream = new java.io.ObjectInputStream(f);
            pNew = (SysErrorCode)objStream.readObject();
            System.out.println("SUCCESS loaded " + pNew.toString());
         }
         catch (Exception e) {
            System.err.println("ERROR: " + e.toString());
         }
      }

      if (pNew.toString().equals(pOrig.toString())) {
         System.out.println("SUCCESS, string form is equals " + pNew.toString());
      }
      else {
         System.out.println("ERROR, string form is different " + pNew.toString());
      }

      int hashOrig = pOrig.hashCode();
      int hashNew = pNew.hashCode();

      if (pNew == pOrig) {
         System.out.println("SUCCESS, hash is same, the objects are identical");
      }
      else {
         System.out.println("ERROR, hashCode is different hashOrig=" + hashOrig + " hashNew=" + hashNew);
      }
   }
}


   /**
    * class holding reference data about other documentation locations
    */
   final class ResourceInfo {
      public final static String REQ = "REQ";
      public final static String API = "API";
      public final static String URL = "URL";

      private final String type; // "API", "REQ", "URL"
      private final String label;
      private final String resource;

      /**
       * @param type One of "API", "REQ", "URL"
       * @param label The visible name for the link
       * @param resource The classname (for API), the requirement name (for REQ) or the href url (for URL)
       */
      public ResourceInfo(String type, String label, String resource) {
         if (!REQ.equalsIgnoreCase(type) && !API.equalsIgnoreCase(type) && !URL.equalsIgnoreCase(type))
            throw new IllegalArgumentException("Construction of ResourceInfo with illegal type=" + type);
         if (label == null || label.length() < 1)
            throw new IllegalArgumentException("Construction of ResourceInfo with empty lable");
         this.type = type.toUpperCase();
         this.label = label;
         this.resource = (resource==null) ? "" : resource;
      }

      public String getType() { return this.type; }
      public String getLabel() { return this.label; }
      public String getResource() { return this.resource; }

      public final String toXml(String extraOffset) {
         StringBuffer sb = new StringBuffer(256);
         String offset = "\n ";
         if (extraOffset == null) extraOffset = "";
         offset += extraOffset;

         sb.append(offset).append("<ResourceInfo type='").append(getType()).append("'");
         sb.append(" label='").append(getLabel()).append("'>");

         sb.append(offset).append(" ").append(getResource());
         sb.append(offset).append("</ResourceInfo>");

         return sb.toString();
      }
   }






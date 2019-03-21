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
 * Project: ethercis openEHR system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.common.def;

import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;


/**
 * Miscellaneous constants used to ease refactorings
 * @author christian
 *
 */
public final class Constants {
	//logger names
	public static final String LOGGER_SECURITY="Security";
	public static final String LOGGER_SYSTEM="System";
	public static final String LOGGER_JOBSCHEDULER = "JobScheduler";

    public static final String DEFAULT_SERVICE_SECURITY_MANAGER_ID = "ServiceSecurityManager";
    public static final String DEFAULT_SERVICE_SECURITY_MANAGER_VERSION = "1.0";


    public static final String UTF8_ENCODING="UTF-8";

	public final static long MINUTE_IN_MILLIS = 1000L*60;
	public final static long HOUR_IN_MILLIS = MINUTE_IN_MILLIS*60;
	public final static long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
	public final static long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;

	public final static String EMAIL_TRANSFER_ENCODING = "Content-Transfer-Encoding";
	public final static String ENCODING_BASE64 = "base64";
	public final static String ENCODING_FORCE_PLAIN = "forcePlain";
	public final static String ENCODING_QUOTED_PRINTABLE = "quoted-printable";
	public final static String ENCODING_NONE = null;

	public final static String TYPE_STRING = "String"; // is default, same logonservice ""
	public final static String TYPE_BLOB = "byte[]";
	/* See JMS types */
	public final static String TYPE_BOOLEAN = "boolean";
	public final static String TYPE_BYTE = "byte";
	public final static String TYPE_DOUBLE = "double";
	public final static String TYPE_FLOAT = "float";
	public final static String TYPE_INT = "int";
	public final static String TYPE_SHORT = "short";
	public final static String TYPE_LONG = "long";
	/** used to tell that the entry is really null (not just empty) */
	public final static String TYPE_NULL = "null";

	/**
	 * The SOCKET protocol can support zlib compression with streaming compression
	 * A partial flush means that all data will be response,
	 * but the next packet will continue using compression tables from the end of the previous packet.
	 * As described in [RFC-1950] and in [RFC-1951]
	 * @see http://www.jcraft.com/jzlib/
	 */
	public final static String COMPRESS_ZLIB_STREAM = "zlib:stream";

	/**
	 * The SOCKET protocol supports zlib compression for each message individually
	 * As described in [RFC-1950] and in [RFC-1951]
	 * @see http://www.jcraft.com/jzlib/
	 */
	public final static String COMPRESS_ZLIB = "zlib";


	// Status id, on error usually an exception is thrown so we don't need "ERROR":

	/** The returned message status if OK */
	public final static String STATE_OK = "OK";
	public final static String INFO_INITIAL = "INITIAL";
	public final static String CLIENTPROPERTY_INITIAL_CALLBACK_PING = "__initialCallbackPing";
	public final static String CLIENTPROPERTY_REMOTEPROPERTIES = "__remoteProperties";
	public final static String STATE_WARN = "WARNING";
	/** The returned message status if message timeout occurred (but not erased) */
	public final static String STATE_TIMEOUT = "TIMEOUT";
	/** The returned message status if message is expired (timeout occurred and is erased) */
	public final static String STATE_EXPIRED = "EXPIRED";
	/** The returned message status if message is explicitly erased by a call to erase() */
	public final static String STATE_ERASED = "ERASED";
	/** The returned message status if message couldn't be forwarded to the master cluster node */
	public final static String STATE_FORWARD_ERROR = "FORWARD_ERROR";

	/** Additional info for state.
	       The returned message status if message couldn't be forwarded to the master cluster node but
	       is in the tail back queue to be delivered on reconnect or on client side message
	       recording.
	 */
	public final static String INFO_QUEUED = "QUEUED";

	/** Type of a message callback queue */
	public final static String RELATING_CALLBACK = "callback";
	/** Type of a message callback queue */
	public final static String RELATING_SUBJECT = "authenticate";
	/** Type of a message queue  on client side */
	public final static String RELATING_CLIENT = "connection";
	/** Type of a message queue on client side for updates (not really a type but used for id) */
	public final static String RELATING_CLIENT_UPDATE = "clientUpdate";
	/** Type of a history message queue containing references on messages */
	public final static String RELATING_HISTORY = "history";
	/** Type of a subscription message queue containing subscriptions */
	public final static String RELATING_SUBSCRIBE = "subscribe";
	/** Type of a subscription message queue containing sessions */
	public final static String RELATING_SESSION = "session";
	/** MessageUnit cache */
	public final static String RELATING_MSGUNITSTORE = "msgUnitStore";
	/** Topics persistence */
	public final static String RELATING_TOPICSTORE = "topicStore";

	/* message queue onOverflow handling, blocking until queue takes messages again (client side) */
	public final static String ONOVERFLOW_BLOCK = "block";
	/** message queue onOverflow handling */
	public final static String ONOVERFLOW_DEADMESSAGE = "deadMessage";
	/** message queue onOverflow handling */
	public final static String ONOVERFLOW_DISCARD = "discard";
	/** message queue onOverflow handling */
	public final static String ONOVERFLOW_DISCARDOLDEST = "discardOldest";
	/** message queue onOverflow handling */
	public final static String ONOVERFLOW_EXCEPTION = "exception";

	/** If callback fails more often than is configured the login session is destroyed */
	public final static String ONEXHAUST_KILL_SESSION = "killSession";

	/** ClientProperty of QoS for messages from persistent store */
	public final static String PERSISTENCE_ID = "__persistenceId";

	/** Prefix to create a sessionId */
	public final static String SESSIONID_PREFIX = "sessionId:";
	public final static String SUBSCRIPTIONID_PREFIX = "__subId:";
	public final static String SUBSCRIPTIONID_PtP = SUBSCRIPTIONID_PREFIX+"PtP";

	public final static String INTERNAL_LOGINNAME_PREFIX_FOR_SERVICES = "_";
	public final static String INTERNAL_RESOURCE_PREFIX_FOR_SERVICES = "_";
	public final static String INTERNAL_RESOURCE_ADMIN_CMD = "__cmd:";
	public final static String INTERNAL_LOGINNAME_PREFIX_FOR_CORE = "__";
	public final static String INTERNAL_RESOURCE_PREFIX_FOR_CORE = "__";
	public final static String INTERNAL_RESOURCE_PREFIX = "__sys__";  // Should be replaced by INTERNAL_RESOURCE_PREFIX_FOR_CORE in future
	public final static String INTERNAL_RESOURCE_CLUSTER_PREFIX = INTERNAL_RESOURCE_PREFIX + "cluster";  // "__sys__cluster"
	public final static String INTERNAL_RESOURCE_REMOTE_PROPERTIES = INTERNAL_RESOURCE_PREFIX + "remoteProperties"; // __sys__remoteProperties
	public final static String INTERNAL_RESOURCE_RUNLEVEL_MANAGER = INTERNAL_RESOURCE_PREFIX + "RunlevelManager"; // __sys__RunlevelManager
	/**
	 * these are used to qualify a service for method mapping in Dispatcher
	 */
	public final static String INTERNAL_RESOURCE_INSTANCE = INTERNAL_RESOURCE_PREFIX + "LocalInstance"; // __sys__LocalInstance
	public final static String REMOTE_RESOURCE_INSTANCE = INTERNAL_RESOURCE_PREFIX + "RemoteInstance"; // __sys__RemoteInstance
	public final static String INTERNAL_RESOURCE_MAP_ONLY = INTERNAL_RESOURCE_PREFIX + "MapOnly"; // __sys__MapOnly
	

	public final static String EVENT_RESOURCE_LOGIN = "__sys__Login";
	public final static String EVENT_RESOURCE_LOGOUT = "__sys__Logout";
	public final static String EVENT_RESOURCE_USERLIST = "__sys__UserList";
	public final static String EVENT_RESOURCE_ERASEDTOPIC = "__sys__ErasedTopic";

	/** JDBC access messages */
	public final static String JDBC_RESOURCE = INTERNAL_RESOURCE_PREFIX + "jdbc";

	/** message queue onOverflow handling "__sys__deadMessage */
	public final static String RESOURCE_DEAD_LETTER = INTERNAL_RESOURCE_PREFIX + "deadMessage";

	/** Client sends with ConnectQos its current UTC timestamp string so server knows approximate offset in time logonservice client may not have accurate time set */
	public final static String CLIENTPROPERTY_UTC = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "UTC";
	/** Dead messages transport in their QoS clientProperty the original message key in '__key' */
	public final static String CLIENTPROPERTY_DEADMSGKEY = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "key";
	/** Dead messages transport in their QoS clientProperty the original message QoS in '__qos' */
	public final static String CLIENTPROPERTY_DEADMSGQOS = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "qos";
	/** Dead messages contain the information of the sending session */
	public final static String CLIENTPROPERTY_DEADMSGSENDER = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "sender";
	/** Dead messages contain the information of the session for which the delivery failed */
	public final static String CLIENTPROPERTY_DEADMSGRECEIVER = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "receiver";
	/** Dead messages transport in their QoS clientProperty the rcvTimestamp in '__rcvTimestamp' */
	public final static String CLIENTPROPERTY_RCVTIMESTAMP = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "rcvTimestamp";
	/** Dead messages transport in their QoS clientProperty the original message resource in '__resource' */
	public final static String CLIENTPROPERTY_RESOURCE = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "resource";
	/** Dead messages transport in their QoS clientProperty the error reason in '__deadMessageReason' */
	public final static String CLIENTPROPERTY_DEADMSGREASON = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "deadMessageReason";
	/** The plugin xml markup send to RunlevelManager '__plugin.xml' */
	public final static String CLIENTPROPERTY_PLUGIN_XML = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "service.xml";
	/** The plugin xml markup send to RunlevelManager '__plugin.jarName' */
	public final static String CLIENTPROPERTY_PLUGIN_JARNAME = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "service.jarName";

	/** ConnectReturnQos their QoS clientProperty the rcvTimestampStr in '__rcvTimestampStr' */
	public final static String CLIENTPROPERTY_RCVTIMESTAMPSTR = INTERNAL_RESOURCE_PREFIX_FOR_CORE + "rcvTimestampStr";

	/** Used in Client Properties to define that the content is encoded with the specified value (default to UTF-8) */
	public final static String CLIENTPROPERTY_CONTENT_CHARSET = "__contentCharset";

	/** For xml key attribute, contentMimeExtended="1.0" */
	public static final String DEFAULT_CONTENT_MIME_EXTENDED = "1.0";

	public static final String INDENT = " ";
	public static final String OFFSET = "\n" + INDENT;


	/** used for permissioning */
	public static final String RESOURCE_TAG = "RESOURCE";
	public static final String PATH_TAG = "PATH";
	public static final String ADL_PATH_TAG = "ADL_PATH";
	public static final String TEMPLATE_FIELD_TAG = "TEMPLATE_FIELD_TAG";
	public static final String TEMPLATE_HEADING_TAG = "TEMPLATE_HEADING_TAG";
	public static final String VALID_CONSENT_TAG = ADL_PATH_TAG+"|"+TEMPLATE_FIELD_TAG+"|"+TEMPLATE_HEADING_TAG;
	public static final Pattern VALID_CONSENT_TAG_CHECKER = Pattern.compile(VALID_CONSENT_TAG);
	
	public static final String PARM_ORGANIZATION_TAG="ORGANIZATION";
	public static final String PARM_PROGRAM_TAG="PROGRAM";
	public static final String PARM_CATEGORY_TAG="CATEGORY";
	public static final String PARM_USER_TAG="USER";
	public static final String PARM_VALID_OPTIONS = PARM_ORGANIZATION_TAG+"|"+PARM_PROGRAM_TAG+"|"+PARM_CATEGORY_TAG+"|"+PARM_USER_TAG; //regexp to check a parm name
	public static final Pattern PARM_VALID_CHECKER = Pattern.compile(PARM_VALID_OPTIONS);
	public static final String PARM_DELIMITER_INPUT=":";
	public static final String PARM_WILDCARD_INPUT="*";
	
	/** XmlKey queryType enum */
	public static final String RESOURCE_URL_PREFIX = "resource:";
	public static final String XPATH = "XPATH";
	public static final String XPATH_URL_PREFIX = "xpath:";
	public static final String EXACT = "EXACT";
	public static final String EXACT_URL_PREFIX = "exact:";
	public static final String DOMAIN = "DOMAIN";
	public static final String DOMAIN_URL_PREFIX = "domain:";
	public static final String SUBSCRIPTIONID_URL_PREFIX = "subscriptionId:";
	public static final String REGEX = "REGEX";

	public static final String TOXML_NOSECURITY = "noSecurity";
	public static final String TOXML_EXTRAOFFSET = "extraOffset";
	public static final String TOXML_FORCEREADABLE = "forceReadable";
	public static final String TOXML_FORCEREADABLE_TIMESTAMP = "forceReadableTimestamp";
	public static final String TOXML_FORCEREADABLE_BASE64 = "forceReadableBase64";
	public static final String TOXML_ENCLOSINGTAG = "enclosingTag";
	public static final String TOXML_MAXCONTENTLEN = "maxContentLen";

	public static final String EVENTPLUGIN_PROP_SUMMARY = "_summary";
	public static final String EVENTPLUGIN_PROP_DESCRIPTION = "_description";
	public static final String EVENTPLUGIN_PROP_EVENTTYPE = "_eventType";
	public static final String EVENTPLUGIN_PROP_ERRORCODE = "_errorCode";
	public static final String EVENTPLUGIN_PROP_PUBSESSIONID = "_publicSessionId";
	public static final String EVENTPLUGIN_PROP_SUBJECTID = "_subjectId";
	public static final String EVENTPLUGIN_PROP_ABSOLUTENAME = "_absoluteName";
	public static final String EVENTPLUGIN_PROP_NODEID = "_nodeId";

	
	//Principal constant
	public static final String EHR_PROGRAM_MANAGER_CGSC_DRS = "EHR_PROGRAM_MANAGER_CGSC_DRS";
	
	/** Stuff used for Streaming */
	/** Mimetypes */
	// see @apache/mime.conf or so

	/**
	 * Hypertext Markup Language
	 */
	public final static String MIME_HTML = "text/html";

	/**
	 * Cascading Style Sheet
	 */
	public final static String MIME_CSS = "text/css";

	/**
	 * Javascript
	 */
	public final static String MIME_JS = "text/javascript";

	/**
	 * The mime type for the xml.
	 * See http://www.rfc-editor.org/rfc/rfc3023.txt
	 */
	public final static String MIME_XML = "text/xml";

	/**
	 * Joint Photographic Experts Group
	 */
	public final static String MIME_JPG = "image/jpeg";

	/**
	 * Portable Network Graphics Format Image
	 */
	public final static String MIME_PNG = "image/png";

	/**
	 * GIF Image
	 */
	public final static String MIME_GIF = "image/gif";
	public static String URI_TAG = "__URI__";


	/**
	 * Adds to the key a prefix JMS_PREFIX if and only if the key is one of the JMSX properties
	 * defined by the XmlBlaster. It does not add anything if it already starts with JMS_PREFIX.
	 *
	 * @param key
	 * @param log
	 * @return
	 */
	public static String addJmsPrefix(String key, Logger log) {
//		if (key.startsWith(JMS_PREFIX)) {
//			log.fine("JMS Property '" + key + "' is already starting with '" + JMS_PREFIX + "'");
//			return key;
//		}
//		if (XBConnectionMetaData.getReservedProps().contains(key) || XBConnectionMetaData.getStandardProps().contains(key))
//			key = JMS_PREFIX + key;
		return key;
	}

	public static byte[] toUtf8Bytes(String s) {
		if (s == null || s.length() == 0)
			return new byte[0];
		try {
			return s.getBytes(Constants.UTF8_ENCODING);
		} catch (UnsupportedEncodingException e) {
			System.out.println("PANIC in Constants.toUtf8Bytes(" + s
					+ ", " + Constants.UTF8_ENCODING + "): " + e.toString());
			e.printStackTrace();
			return s.getBytes();
		}
	}

	public static String toUtf8String(byte[] b) {
		if (b == null || b.length == 0)
			return "";
		try {
			return new String(b, Constants.UTF8_ENCODING);
		} catch (UnsupportedEncodingException e) {
			System.out.println("PANIC in toUtf8String(" + b + ", "
					+ Constants.UTF8_ENCODING + "): " + e.toString());
			e.printStackTrace();
			return new String(b);
		}
	}

	public static String toEncodedString(byte[] b, String encoding) {
		if (b == null)
			return null;
		if (encoding == null || encoding.trim().length() < 1)
			encoding = Constants.UTF8_ENCODING;

		try {
			return new String(b, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.err.println("PANIC Could not encode according to '" + encoding + "': " + e.getMessage());
			return Constants.toUtf8String(b);
		}
	}	
	
	
	//used in recovery mode from agent (history time stamp in alarm events)
	public static final String HISTORY_TS="HistoryTS";
	public static final String HISTORY_TS_FORMAT = "yyyy-MM-dd HH:mm:ss Z";
	public static final String XML_DATE_FORMAT="yyyy.MM.dd HH:mm:ss Z"; //unless specified otherwise
	public static final String I_AUTHENTICATE_PROPERTY_KEY = "Authenticate";
	
	//used for mapping between REST queries and service capabilities
	public static final String SERVICE_RESOURCE = "SERVICE_RESOURCE";
	public static final String SERVICE_METHOD = "SERVICE_METHOD";

	


	//Filewatcher constants
	public static String FW_FILENAME_ATTR = "_filename"; 
	public static String FW_SUBDIR_ATTR = "_subdir"; 
	public static String FW_FILENAME_ATTR_OLD_FASHION = "_fileName"; 
	public static String FW_TIMESTAMP_ATTR = "_timestamp"; 
	public static String FW_TOPIC_NAME = "_topicName"; 
	public static String FW_FILE_DATE = "_fileDate";
	public static String USE_REGEX  = "regexFilter";
	public static String USE_SIMPLE  = "simpleFilter";
	
	//used by AEPController to identify connect queries (only one without sessionid)
	public static Object CONNECT_PATH = "vehr";
	public static Object POLICY_PATH = "vehr/policy";
	public static Object CONNECT_METHOD = "connect";
	public static Object DISCONNECT_METHOD = "disconnect";
	public static String AEP_COMMAND = "command";
	public static String AEP_CHECK = "isallowed";
	public static final String AEP_EDITCONFIG = "cfgedit";
	public static final String AEP_GETCONFIG = "cfgget";
	//used in HTTP POST 
	public static final String REQUEST_CONTENT = "x-request-content";
	public static final String REQUEST_CONTENT_TYPE = "x-request-content-type";
	public static final String REQUEST_CONTENT_LENGTH = "x-request-content-length";

	public static final String	REQUEST_BODY		= "x-request-content";
	
	//more HTTP Header constants
	public static final String SESSION_KEY_HEADER = "x-session-id";
	public static final String SESSION_ERROR_CODE_HEADER = "x-error-code";
	public static final String SESSION_IN_USED = "x-session-in-use";
	public static final String RECONNECT = "x-reconnect";
	public static final String SESSION_NAME = "x-session-name";
	public static final String SESSION_TIMEOUT = "x-session-timeout";
	public static final String MAX_SESSION = "x-max-session";
	public static final String CLEAR_SESSION = "x-clear-session";
	public static final String CLIENT_IP = "x-client-ip";
	public static final String LAST_LOGIN = "x-last-login";
	
	//Property tags
	public static final String SERVER_PERSISTENCE_IMPLEMENTATION = "server.persistence.implementation";
	public static final String SERVER_PERSISTENCE_JOOQ_DIALECT ="server.persistence.jooq.dialect";
	public static final String SERVER_PERSISTENCE_JOOQ_URL ="server.persistence.jooq.url";
	public static final String SERVER_PERSISTENCE_JOOQ_LOGIN ="server.persistence.jooq.login";
	public static final String SERVER_PERSISTENCE_JOOQ_PASSWORD ="server.persistence.jooq.password";
	public static final String SERVER_PERSISTENCE_JOOQ_DATABASE = "server.persistence.jooq.database";
	public static final String SERVER_PERSISTENCE_JOOQ_HOST = "server.persistence.jooq.host";
	public static final String SERVER_PERSISTENCE_JOOQ_PORT = "server.persistence.jooq.port";
	public static final String SERVER_PERSISTENCE_JOOQ_MAX_CONNECTIONS = "server.persistence.jooq.max_connections";
	public static final String SERVER_PERSISTENCE_MAX_IDLE = "server.persistence.dbcp2.max_idle";
	public static final String SERVER_PERSISTENCE_MAX_ACTIVE = "server.persistence.dbcp2.max_active";
	public static final String SERVER_PERSISTENCE_TEST_ON_BORROW="server.persistence.dbcp2.test_on_borrow";
	public static final String SERVER_PERSISTENCE_AUTO_RECONNECT="server.persistence.dbcp2.auto_reconnect";
	public static final String SERVER_PERSISTENCE_WAIT_MS="server.persistence.dbcp2.max_wait";
	public static final String SERVER_PERSISTENCE_SET_POOL_PREPARED_STATEMENTS="server.persistence.dbcp2.set_pool_prepared_statements";
	public static final String SERVER_PERSISTENCE_MAX_PREPARED_STATEMENTS="server.persistence.dbcp2.set_max_prepared_statements";
	public static final String SERVER_PERSISTENCE_REMOVE_ABANDONNED="server.persistence.dbcp2.remove_abandonned";
	public static final String SERVER_PERSISTENCE_REMOVE_ABANDONNED_TIMEOUT="server.persistence.dbcp2.remove_abandonned_timeout";
	public static final String SERVER_PERSISTENCE_LOG_ABANDONNED="server.persistence.dbcp2.log_abandonned";
	public static final String SERVER_PERSISTENCE_INITIAL_CONNECTIONS="server.persistence.dbcp2.initial_size";

	public static final String XML_POLICY_PATH_TAG = "server.security.policy.xml.path";
	public static final String POLICY_TYPE_TAG     = "server.security.policy.type";
	public static final String DB_SECURITY_ROLE     = "server.security.db_role";
	public static final String SERVER_AUDIT     = "server.audit";
	public static final String DB_SECURITY_PRINCIPAL_PRECEDENCE = "server.security.role_precedence";
	public static final String SQL_ENABLED     = "server.query.sql_enabled";
	public static final String JWT_KEY     = "server.jwt.key";
	public static final String JWT_ALGORITHM = "server.jwt.algorithm";
	public static final String JWT_KEY_FILE_PATH     = "server.jwt.key_file_path";
	public static final String TOKEN_TYPE_BEARER = "Bearer";
	public static final String TOKEN_USER_SESSION = "userSession";
	public static final String TOKEN_PRINCIPAL_SESSION = "principalSession";
	public static final String JWT_KEY_SIGNATURE     = "server.jwt.signature";
	
	//Policy Ldap configuration
	public static final String LDAP_POLICY_HOST="server.security.policy.ldap.host";
	public static final String LDAP_POLICY_PORT="server.security.policy.ldap.port";
	public static final String LDAP_POLICY_PERMISSION_DC="ethercisPermissionRef";
    public static final String LDAP_POLICY_BASE_DN="server.security.policy.ldap.baseDn";
    public static final String LDAP_POLICY_PRINCIPAL_CLASS="yourPrincipalClass";

	
	public static final String STR_POLICY_XML = "XML";
	public static final String STR_POLICY_LDAP = "LDAP";
	public static final String STR_POLICY_JDBC = "JDBC";
	public static final String STR_POLICY_DEBUG = "DEBUG";
    public static final String STR_POLICY_SHIRO = "SHIRO";
	public static final String STR_POLICY_JWT = "JWT";
	public static final String STR_POLICY_UNDEF = "";
	
	public static final int POLICY_XML = 1;
	public static final int POLICY_LDAP = 2;
	public static final int POLICY_JDBC = 3;
	public static final int POLICY_DEBUG = 4;
    public static final int POLICY_SHIRO = 5;
	public static final int POLICY_JWT = 6;
	public static final int POLICY_UNDEF = 0;

	//used by RM builder
	public static final String DEFAULT_STRING = "$DEFAULT$";

	//used by JwtAuthenticate
	public static final String JWT_CONTEXT = "JWT_CONTEXT";
}

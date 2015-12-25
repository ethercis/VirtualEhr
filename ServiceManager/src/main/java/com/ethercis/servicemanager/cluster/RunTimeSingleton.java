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


package com.ethercis.servicemanager.cluster;
import com.ethercis.servicemanager.common.*;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.property.Property;
import com.ethercis.servicemanager.common.session.I_Authenticate;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxMBeanHandle;
import com.ethercis.servicemanager.jmx.JmxWrapper;
import com.ethercis.servicemanager.runlevel.RunlevelManager;
import com.ethercis.servicemanager.service.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;

/**
 * Main class to control an aggregation of services (a cluster of services). 
 * <p>
 * There should be a unique RunTimeSingleton instance per instance server. The singleton
 * can be used to share globals between services. In particular it is used to register 
 * and identify registered services.
 * <p>
 * The singleton is also used for JMX operation including registering of instrumentalized
 * services.
 * <p>
 * The singleton provides the operational parameters of the instance including JVM status,
 * memory usage, communication info etc. logonservice well logonservice build details such logonservice version etc. if used
 * in conjunction with a build procedure updating the fields:<p>
 * <ul>
 * <li>version
 * <li>revision.number
 * <li>build.timestamp
 * <li>build.java.vendor
 * <li>build.java.version
 * </ul>
 * <p>
 * @author Christian
 *
 */
public class RunTimeSingleton implements Cloneable
{
	private static Logger log = Logger.getLogger(RunTimeSingleton.class);
	private static boolean logIsInitialized;

	private volatile static RunTimeSingleton firstInstance;

	/** The amount of physical RAM of this machine.
	 * Set by JmxWrappter.java, else 0
	 * @since JDK 1.5
	 */
	public static long totalPhysicalMemorySize;

	/** Number of bytes this JVM can allocate max, the -Xmx???M setting
	 * Set by JmxWrappter.java
	 * (defaults to Runtime.getRuntime().maxMemory())
	 * @since JDK 1.5
	 */
	public static long heapMemoryUsage = Runtime.getRuntime().maxMemory();

	/** The max number of file descriptors this JVM may use
	 * Set by JmxWrappter.java, else 0
	 * @since JDK 1.5
	 */
	public static long maxFileDescriptorCount;

	/** Version string, please change for new releases (4 digits) */
	private String versionDefault = "1.0.0";
	/** This will be replaced by build.xml with the current version */
	private String version = "@version@";
	/** This will be replaced by build.xml with the current subversion revision number */
	private String revisionNumber = "@revision.number@";
	/** This will be replaced by build.xml with the build timestamp */
	private String buildTimestamp = "@build.timestamp@";
	/** This will be replaced by build.xml with the compiling JDK vendor */
	private String buildJavaVendor = "@build.java.vendor@";
	/** This will be replaced by build.xml with the compiling JDK version */
	private String buildJavaVersion = "@build.java.version@";

	protected String ME = "ClusterController";
	protected String ip_addr = null;
	protected String id = "";
	private volatile String instanceId;
	private NodeId nodeId;

	protected volatile Property property = null;
	protected String errorText = null;

	protected ContextNode contextNode;
	protected ContextNode scopeContextNode;

	private ServiceHolder serviceHolder;

	protected String addressNormalized = null;

	/** Store objects in the scope of one client connection or server instance */
	protected /*final*/ Map<String, Object> objectMap;
	/** Helper to synchronize objectMap access */
	public final Object objectMapMonitor = new Object();

	protected Hashtable<Object, Object> logChannels = new Hashtable<Object, Object>();

	protected SAXParserFactory saxFactory;
	protected DocumentBuilderFactory docBuilderFactory;
	//protected TransformerFactory transformerFactory;

	protected static int counter = 0;

	private volatile ServiceManagerBase serviceManager;
	private volatile ServiceRegistry serviceRegistry;
	private volatile RunlevelManager runlevelManager;
	protected boolean isDoingShutdown = false;

	/** set to allow wipe out the persistence on restarts */
	protected boolean wipeOutDB = false;

	/** JMX notification sequence number. */
	protected long sequenceNumber = 1;
	private int currRunlevel = 0;

	private Map<Object, Object> weakRegistry = new WeakHashMap<Object, Object>();
	private Timeout sessionTimer;
	private I_Authenticate authenticate;

	/**
	 * Constructs an initial ClusterController object,
	 * same logonservice ClusterController(null, true, true)
	 */
    protected RunTimeSingleton() {
		this(null, true, false);
	}

	/**
	 * Constructs an initial ClusterController object which is initialized
	 * by your properties (without leading '-'),
	 * same logonservice ClusterController(args, true, true)
	 */
    protected RunTimeSingleton(Properties props) {
		this(Property.propsToArgs(props));
	}

	/**
	 * for compatibility purpose
	 */
    protected RunTimeSingleton(Properties props, boolean bcompat) {
		this(Property.propsToArgs(props));
	}	    
	/**
	 * Constructs an initial ClusterController object which is initialized
	 * by your args array (usually the command line args).
	 * Same logonservice ClusterController(args, true, true)
	 */
	protected RunTimeSingleton(String[] args)
	{
		this(args, true, false);
	}

    protected RunTimeSingleton(String[] args, boolean loadPropFile, boolean checkInstance) {
		this(args, loadPropFile, checkInstance, true);
	}

	/**
	 * Constructs an initial ClusterController object which is initialized
	 * by your args array (usually the command line args).
	 *
	 * <p>By setting loadPropFile to false it is possible to create a ClusterController
	 * which does not automatically search out the services.properties file,
	 * which is good when you want to start ehrserver in an embedded environment.
	 * <p>It is possible to later load the property file if one wants, here is one
	 * way to do it:</p>
	 * <pre>
	          Property p = glob.getProperty();
	          Properties prop = new Properties();
	          FileInfo i = p.findPath("services.properties");
	          InputStream is = i.getInputStream();
	          prop.load(is);
	          String[] ar = Property.propsToArgs(prop);
	          p.addArgs2Props( ar != null ? ar : new String[0] );
	        </pre>
	      <p>It is also possible to load an entire second property file or find it
	         with some other algorithm byte using the same pattern logonservice above, just
	         don't use findPath, but some other code.</p>
	 * @param args args array (usually the command line args).
	 * @param loadPropFile if automatic loading of services.properties should be done.
	 */
    protected RunTimeSingleton(String[] args, boolean loadPropFile, boolean checkInstance, boolean doReplace)
	{
		counter++;
		if (checkInstance == true) {
			if (firstInstance != null) {
				System.out.println("######ClusterController args constructor invoked again, try ClusterController.instance()");
				Thread.dumpStack();
			}
		}
		synchronized (RunTimeSingleton.class) {
			if (firstInstance == null)
				firstInstance = this;
		}
		initProps(args,loadPropFile, doReplace);
		initId();

		objectMap = new HashMap<>();
	}

	public boolean supportJmx() {
		boolean supportJmx = getProperty().get("services/jmx/support", true);
		return supportJmx;
	}

	public static int getCounter() { return counter; }

	/**
	 * @return the JmxWrapper used to manage the MBean knowledge
	 */
	public final JmxWrapper getJmxWrapper() throws ServiceManagerException {
		if (!supportJmx()) return null;
		return JmxWrapper.getInstance(this);
	}

	/**
	 * Check if JMX is activated.
	 * @return true if JMX is in use
	 */
	public boolean isJmxActivated() {
		if (!supportJmx()) return false;
		try {
			return getJmxWrapper().isActivated();
		}
		catch (ServiceManagerException e) {
			return false;
		}
	}

	/**
	 * Send an administrative notification.
	 */
	public void sendNotification(javax.management.NotificationBroadcasterSupport source,
			String msg, String attributeName,
			String attributeType, Object oldValue, Object newValue) {
		// Avoid any log.warn or log.error to prevent looping alert events
		if (isJmxActivated()) {
			javax.management.Notification n = new javax.management.AttributeChangeNotification(source,
					sequenceNumber++,
					System.currentTimeMillis(),
					msg,
					attributeName,
					attributeType,
					oldValue,
					newValue);
			source.sendNotification(n);
		}
	}

	/**
	 * JMX support.
	 * Start ehrserver with <code>java -Dcom.sun.management.jmxremote org.ehrserver.Main</code>
	 * You can access ehrserver from 'jconsole' delivered with JDK1.5 or above.
	 * The root node is always the cluster node id.
	 * @param contextNode Used to retrieve a unique instance name for the given MBean
	 * @param mbean the MBean object instance
	 * @return The object name used to register or null on error
	 * @since 1.0.5
	 */
	public JmxMBeanHandle registerMBean(ContextNode contextNode, Object mbean) throws ServiceManagerException {
		if (!supportJmx()) return null;
		return getJmxWrapper().registerMBean(contextNode, mbean);
	}

	/**
	 * Unregister a JMX MBean.
	 * Never throws any exception
	 * @param objectName The object you got from registerMBean() of type ObjectName,
	 *                   if null nothing happens
	 */
	public void unregisterMBean(Object objectName) {
		if (!supportJmx()) return;
		if (objectName == null) return;
		try {
			if (objectName instanceof JmxMBeanHandle)
				getJmxWrapper().unregisterMBean((JmxMBeanHandle)objectName);
			else
				getJmxWrapper().unregisterMBean((javax.management.ObjectName)objectName);
		}
		catch (ServiceManagerException e) {
			log.warn("unregisterMBean(" + objectName.toString() + ") failed: " + e.toString());
		}
		catch (Throwable e) {
			log.error("unregisterMBean(" + objectName.toString() + ") failed: " + e.toString());
		}
	}

	public boolean isRegisteredMBean(ContextNode ctxNode) throws ServiceManagerException {
		return getJmxWrapper().isRegistered(ctxNode);
	}

	/**
	 * See @version@ which will be replaced by build.xml with the current version
	 * @return e.g. "0.79f"
	 */
	public String getVersion() {
		if (version.indexOf("@") == -1) // Check if replaced
			return version;
		return versionDefault;
	}

	/**
	 * See @revision.number@ which will be replaced by build.xml with the current subversion revision
	 * @return e.g. "12702" or "12702M". If no subversion is available getVersion() is returned
	 */
	public String getRevisionNumber() {
		if (this.revisionNumber.indexOf("@") == -1 && !"${revision.number}".equals(this.revisionNumber)) // Check if replaced
			return this.revisionNumber;
		return versionDefault;
	}

	/**
	 * Combination from getVersion() and getRevisionNumber().
	 * @return e.g. "0.91 #12702"
	 */
	public String getReleaseId() {
		if (!getVersion().equals(getRevisionNumber()))
			return getVersion() + " #" + getRevisionNumber();
		return getVersion();
	}

	/**
	 * See @build.timestamp@ which will be replaced by build.xml with the current timestamp
	 * @return e.g. "06/17/2002 01:38 PM"
	 */
	public String getBuildTimestamp() {
		return buildTimestamp;
	}

	/**
	 * @return e.g. "1.3.1-beta"
	 */
	public String getBuildJavaVendor() {
		return buildJavaVendor;
	}

	/**
	 * @return e.g. "1.3.1-beta"
	 */
	public String getBuildJavaVersion() {
		return buildJavaVersion;
	}

	/**
	 * Our identifier, the cluster node we want connect to
	 */
	protected void initId() {
		this.id = getProperty().get("server.node.id", (String)null);
		if (this.id == null)
			this.id = getProperty().get("cluster.node.id", "ehrserver");  // fallback
		if (nodeId == null) //give it a nodeid otherwise it crashes...
			nodeId = new NodeId(this.id);
	}

	protected void shallowCopy(RunTimeSingleton utilGlob)
	{
		this.ip_addr = utilGlob.ip_addr;
		this.id = utilGlob.id;
		this.property = utilGlob.property;
		this.errorText = utilGlob.errorText;
		this.objectMap = utilGlob.objectMap;
		this.logChannels = utilGlob.logChannels;
	}

	/**
	 * private, called from constructor
	 * @param args arguments to initialize the property with.
	 * @param loadPropFile if loading of services.properties
	 *        file should be done, if false no loading of the file is done.
	 * @return -1 on error
	 * @exception If no Property instance can be created
	 */
	private int initProps(String[] args, boolean loadPropFile, boolean doReplace) {
		if (property == null) {
			synchronized (Property.class) {
				if (property == null) {
					try {
						if (loadPropFile)
							property = new Property("services.properties", true, args, doReplace);
						else
							property = new Property(null, true, args, doReplace);
					}
					catch (ServiceManagerException e) {
						errorText = ME + ": Error in services.properties: " + e.toString();
						System.err.println(errorText);
						try {
							property = new Property(null, true, args, doReplace);  // initialize without properties file!
						}
						catch (ServiceManagerException e2) {
							errorText = ME + " ERROR: " + e2.toString();
							System.err.println(errorText);
							try {
								property = new Property(null, true, new String[0], doReplace);  // initialize without args
							}
							catch (ServiceManagerException e3) {
								errorText = ME + " ERROR: " + e3.toString();
								System.err.println(errorText);
								e3.printStackTrace();
								throw new IllegalArgumentException("Can't create Property instance: " + errorText);
							}
						}
						return -1;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Configure JDK 1.4 java.common.logging (only once per JVM-Classloader, multiple ClusterController instances share the same).
	 * </p>
	 * Switch off ehrserver specific logging:
	 * <pre>
	 * ehrserver/java.common.logging=false
	 * </pre>
	 * </p>
	 * Lookup a specific logging.properties:
	 * <pre>
	 * java.common.logging.config.file=logging.properties
	 * </pre>
	 * @return The used configuration file (can be used for user notification) or null
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException if redirection fails
	
	private URL initLogManager(String[] args) throws ServiceManagerException {
		if (args == null) return null;
		if (logIsInitialized) return null;

		final String propertyName = "java.common.logging.config.file";

		if ("false".equals(getProperty().get("ehrserver/java.common.logging", (String)null))) {
			logIsInitialized = true;
			System.out.println("Switched off logging configuration with 'ehrserver/java.common.logging=false'");
			return null;
		}

		FileLocator fl = new FileLocator(this);
		URL url = fl.findFileInSearchPath(propertyName, "logging.properties");
		if (url == null) {
			throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION,
					"ClusterController", "Can't find java.common.logging.config.file=logging.properties");
		}
		try {
			InputStream in = url.openStream();

			LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(in);
			in.close();

			// initializeSession from command line (or services.properties)
			synchronized (ClusterController.class) {
				if (!logIsInitialized) {
					Map<?, ?> map = this.property.getPropertiesForContextNode(this.contextNode, ContextNode.LOGGING_MARKER_TAG, "__default");
					String defVal = (String)map.get("__default");
					if (defVal != null) {
						try {
							Level defaultLevel = Level.parse(defVal);
							Logger defLogger = logManager.getLogger("");
							if (defLogger != null) {
								defLogger.setLevel(defaultLevel);
								log.info("Setting default log level to '" + defaultLevel.getUserId() + "'");
							}
							else
								log.warn("Setting default log level to '" + defaultLevel.getUserId() + "' failed since default log level is null");
						}
						catch (Throwable ex) {
							log.warn("An exception occured when parsing '" + defVal + "' logonservice a log level");
						}
					}
					Iterator<?> iter = map.entrySet().iterator();

					Logger defLogger = logManager.getLogger("");
					// Handler[] tmpHandlers = defLogger.getHandlers();
					// Handler[] refHandlers = new Handler[tmpHandlers.length];
					Handler[] refHandlers = defLogger.getHandlers();
					for (int i=0; i < refHandlers.length; i++) {
						refHandlers[i].setLevel(Level.FINEST);
						Formatter formatter = refHandlers[i].getFormatter();
						if (formatter instanceof SBFormatter) {
							SBFormatter xb = (SBFormatter)formatter;
							xb.setRunTimeSingleton(this);
						}
					}

					while (iter.hasNext()) {
						Map.Entry entry = (Map.Entry)iter.next();
						String key = (String)entry.getKey();
						String val = (String)entry.getValue();
						try {
							Level level = Level.parse(val);
							Logger tmpLogger = Logger.getLogger(key);
							if (tmpLogger != null) {
								tmpLogger.setLevel(level);
								tmpLogger.setUseParentHandlers(false);
								for (int i=0; i < refHandlers.length; i++) {
									// handlers[i].setLevel(level);
									tmpLogger.addHandler(refHandlers[i]);
								}
								log.info("Setting log level for '" + key + "' to '" + level.getUserId() + "'");
							}
							else
								log.info("Setting log level for '" + key + "' to '" + level.getUserId() + "' failed since logger was null");
						}
						catch (Throwable ex) {
							log.warn("An exception occured when parsing '" + val + "' logonservice a log level for '" + key + "'");
						}
					}
					logIsInitialized = true;
				}
			}
			return url;
		}
		catch (Exception e) {
			throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION,
					"ClusterController.initLogManager", url.toString(), e);
		}
	}
 */
	/**
	 * Get the current loglevel.
	 *
	 * @param loggerName e.g. "logging" or "/node/heron/logging/com.ethercis.common.Timestamp"
	 * @return The logging level, for example "warn" or "FINE" */
	
	public Level getLogLevel(String loggerName) throws ServiceManagerException {
		if (loggerName == null || loggerName.length() < 1)
			throw new ServiceManagerException(this, SysErrorCode.USER_CONFIGURATION, ME, "Illegal loglevel syntax '" + loggerName + "'");
		if (log != null)
		{
			Level level = log.getLevel();
			if (level == null){ //try with parent
				level = log.getParent().getLevel();
				if (level == null){
					return Level.INFO;
				}
				else
					return level;
			}
			return log.getLevel();
		}
		return Level.INFO;
	}

	/**
	 * Changes the given logger to given level.
	 * @param loggerName e.g. "logging" or "logging/com.ethercis.common.StopWatch"
	 * @param level For example "FINE"
	 * @return The set level
	 * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException if your bool is strange */
	
	public Level changeLogLevel(String loggerName, Level level) throws ServiceManagerException {
		Logger logger = Logger.getLogger(loggerName);
		logger.setLevel(level);
		return level;
	}

	/**
	 * Calls initializeSession(String[] args), the props keys have no leading "-".
	 * @return 1 Show usage, 0 OK, -1 error
	 */
	public int init(Map props) {
		return init(Property.propsToArgs(props));
	}

	/**
	 * The args key needs a leading "-".
	 * @return 1 Show usage, 0 OK, -1 error
	 */
	public int init(String[] args)
	{
		args = (args==null) ? new String[0] : args;

		try {
			property.addArgs2Props(args);
			initId();
			/*
			try { // since JKD 1.4:
				URL url = initLogManager(args);
				if (url != null)
					log.info("Configuring JDK 1.4 logging with configuration '" + url.toString() + "'");
			}
			catch (ServiceManagerException e) {
				System.err.println("Configuring JDK 1.4 logging response failed: " + e.toString());
			}*/
			return property.wantsHelp() ? 1 : 0;
		}
		catch (ServiceManagerException e) {
			errorText = ME + " ERROR: " + e.toString();
			System.err.println(errorText); // Log probably not initialized yet.
			return -1;
		}
	}

	/**
	 * Allows you to query if user wants help.
	 * @return true If '-help' or '-?' was passed to us
	 */
	public final boolean wantsHelp() {
		return property.wantsHelp();
	}

	/**
	 * @return If not null there was an error during construction / initialization
	 */
	public String getErrorText() {
		return this.errorText;
	}

	/**
	 * @return 1 Show usage, 0 OK
	 */
	public int init(java.applet.Applet applet) {
		property.setApplet(applet);
		return property.wantsHelp() ? 1 : 0;
	}

	/**
	 * The unique name of this instance.
	 * @return Can be null during startup
	 */
	public ContextNode getContextNode() {
		return this.contextNode;
	}

	/**
	 * The unique name of this instance.
	 * @param contextNode The new node id
	 */
	public void setContextNode(ContextNode contextNode) {
		this.contextNode = contextNode;
	}

	/**
	 * Helper for the time being to be used on client side by
	 * services like SmtpClient.
	 * Is filled by ehrserverAccess with for example "/node/heron/client/joe/session/1"
	 * @return
	 */
	public ContextNode getScopeContextNode() {
		if (this.scopeContextNode == null) return getContextNode();
		return this.scopeContextNode;
	}
	public void setScopeContextNode(ContextNode contextNode) {
		this.scopeContextNode = contextNode;
	}

	/**
	 * Check where we are, on client or on server side?
	 * engine.ClusterController overwrites this
	 * @return false As we are common.ClusterController and running client side
	 */
	public boolean isServerSide() {
		return true;
	}

	/**
	 * Access the unique local id (logonservice a String),
	 * on client side typically the loginName with the public sessionId,
	 * on server side the server instance unique id.
	 * @return ""
	 */
	public String getId() {
		if (getNodeId() == null) return null;
		return getNodeId().getId();
	}

	/**
	 * Same logonservice getId() but all 'special characters' are stripped
	 * so you can use it for file names.
	 * @return ""
	 */
	public String getStrippedId() {
		return getStrippedString(getId());
	}

	/**
	 * @return for XBSTORE.XBNODE, typically the cluster.node.id
	 */
	public String getDatabaseNodeStr() {
		return getStrippedId();
	}


	/**
	 * Utility method to strip any string, all characters which prevent
	 * to be used for e.g. file names are replaced.
	 * <p>
	 * This conversion is used for file names and for the administrative
	 * hierarchy e.g. "/node/heron/client/joe" is OK but 'http://xy:8080' instead of 'heron' is not
	 * </p>
	 * @param text e.g. "http://www.ehrserver.org:/home\\x"
	 * @return e.g. "http_www_ehrserver_org_homex"
	 */
	public static final String getStrippedString(String text) {
		if (text == null) return null;
		String strippedId = ReplaceVariable.replaceAll(text, "/", "");
		// JMX does not like commas, but we can't introduce this change in 1.0.5
		// logonservice the persistent queue names would change and this is not backward compatible
		//strippedId = ReplaceVariable.replaceAll(strippedId, ",", "_");
		strippedId = ReplaceVariable.replaceAll(strippedId, " ", "_");
		strippedId = ReplaceVariable.replaceAll(strippedId, ".", "_");
		strippedId = ReplaceVariable.replaceAll(strippedId, ":", "_");
		strippedId = ReplaceVariable.replaceAll(strippedId, "[", "_");
		strippedId = ReplaceVariable.replaceAll(strippedId, "]", "_");
		return ReplaceVariable.replaceAll(strippedId, "\\", "");
	}

	/**
	 * @see org.ehrserver.util.admin.extern.JmxWrapper#validateJmxValue(String)
	 */
	public final String validateJmxValue(String value) {
		if (!supportJmx()) return value;
		return JmxWrapper.validateJmxValue(value);
	}

	/**
	 * Currently set by enging.ClusterController, used server side only.
	 * @param a unique id
	 */
	public synchronized void setId(String id) {
		if (id == null) return;
		this.nodeId = new NodeId(id); // ContextNode should replace NodeId one day
		this.contextNode = new ContextNode(ContextNode.CLUSTER_MARKER_TAG, getStrippedId(), (ContextNode)null);
	}

	/**
	 * Is coded in derived engine.ClusterController
	 */
	public String getLogPrefixDashed() {
		return "";
	}

	/**
	 * Is coded in derived engine.ClusterController
	 */
	public String getLogPrefix() {
		return "";
	}

	/**
	 * ClusterController access to the default 'ClusterController' instance.
	 * If you have parameters (e.g. from the main() method) you should
	 * initialize RunTimeSingleton first before using instance():
	 * <pre>
	 *    public static void main(String[] args) {
	 *       new RunTimeSingleton(args);
	 *       ...
	 *    }
	 *
	 *    //later you can get this initialized instance with:
	 *    ClusterController glob = ClusterController.instance();
	 *    ...
	 * </pre>
	 * <p>
	 * Note that you should avoid to use RunTimeSingleton.instance() and preferably
	 * use the RunTimeSingleton which describes your current context, e.g. the specific
	 * client connection like ehrserverAccess.getRunTimeSingleton().
	 * Use RunTimeSingleton.getClone(String[]) to create a new RunTimeSingleton instance.
	 * </p>
	 */
	public static RunTimeSingleton instance() {
		if (firstInstance == null) {
			synchronized (RunTimeSingleton.class) {
				if (firstInstance == null)
					new RunTimeSingleton();
			}
		}
		//System.out.println("Accessing ClusterController.instance()");
		//Thread.currentThread().dumpStack();
		return firstInstance;
	}

	/**
	 * Get a cloned instance.
	 * <p>
	 * Calls clone() and sets the given args thereafter.
	 * </p>
	 * <p>
	 * This is the preferred way to create a new and independent
	 * RunTimeSingleton instance for example for another client connection.
	 * </p>
	 * <p>
	 * Note that RunTimeSingleton.instance() will return the original instance
	 * even if called on the cloned object (it's a static variable).
	 * You should avoid to use ClusterController.instance()
	 * </p>
	 *
	 * @param args Additional configuration parameters
	 */
	public final RunTimeSingleton getClone(String[] args) {
		RunTimeSingleton g = (RunTimeSingleton)clone();
		if (args != null && args.length > 0)
			g.init(args);
		return g;
	}

	/**
	 * Get a deep clone (everything is independent from the origin).
	 * <p />
	 * The properties and log channels and ContextNode are copied with a deep copy
	 * manipulating these will not affect the original ClusterController.<br />
	 * All other attributes are initialized logonservice on startup.
	 */
	protected Object clone() {
		// We should not use a ctor for clones, but instead:
		//ClusterController newObject = (ClusterController)super.clone();
		// but our ClusterController ctor uses counter++, so we nevertheless do it like this (breaking Object.clone javadoc that no ctor is called):
		RunTimeSingleton g = new RunTimeSingleton(Property.propsToArgs(this.property.getProperties()), false, false);
		if (this.contextNode != null) {
			g.setContextNode(new ContextNode(this.contextNode.getClassName(), this.contextNode.getInstanceName(), this.contextNode.getParent()));
		}

		return g;
	}

	/**
	 * Access the environment properties, is never null.
	 */
	public final Property getProperty() {
		return (this.property == null) ? new Property() : this.property;
	}




	/**
	 * Get an object in the scope of an ehrserver client connection or of one cluster node.
	 * <p />
	 * This is helpful if you have more than one I_ehrserverAccess or cluster nodes
	 * running in the same JVM
	 *
	 * @param key  e.g. <i>"SOCKET192.168.2.2:7604"</i> from 'cbAddr.getType() + cbAddr.getRawAddress()'<br />
	 *             or <i>"/ehrserver/I_Authenticate"</i>
	 * @return The instance of this object
	 */
	public final Object getObjectEntry(String key)
	{
		synchronized (this.objectMapMonitor) {
			return objectMap.get(key);
		}
	}

	/**
	 * Add an object in the scope of an I_ehrserverAccess or of one cluster node.
	 * <p />
	 * This is helpful if you have more than one I_ehrserverAccess or cluster nodes
	 * running in the same JVM
	 *
	 * @param key  e.g. "SOCKET192.168.2.2:7604" from 'cbAddr.getType() + cbAddr.getRawAddress()'
	 * @param The instance of the protocol callback driver
	 */
	public final void addObjectEntry(String key, Object driver)
	{
		synchronized (this.objectMapMonitor) {
			objectMap.put(key, driver);
		}
	}

	/**
	 * Remove an object from the scope of an I_ehrserverAccess or of one cluster node.
	 * <p />
	 * This is helpful if you have more than one I_ehrserverAccess or cluster nodes
	 * running in the same JVM
	 *
	 * @param key  e.g. "SOCKET192.168.2.2:7604" from 'cbAddr.getType() + cbAddr.getRawAddress()'
	 */
	public final void removeObjectEntry(String key)
	{
		synchronized (this.objectMapMonitor) {
			objectMap.remove(key);
		}
	}

	/**
	 * The IP address where we are running.
	 * <p />
	 * You can specify the local IP address with e.g. -bootstrapHostname 192.168.10.1
	 * on command line, useful for multi-homed hosts.
	 *
	 * @return The local IP address, defaults to '127.0.0.1' if not known.
	 */
	public final String getLocalIP()
	{
		if (this.ip_addr == null) {
			try {
				this.ip_addr = java.net.InetAddress.getLocalHost().getHostAddress(); // e.g. "204.120.1.12"
			} catch (java.net.UnknownHostException e) {
				log.warn("Can't determine local IP address, try e.g. '-bootstrapHostname 192.168.10.1' on command line: " + e.toString());
			}
			if (this.ip_addr == null) this.ip_addr = "127.0.0.1";
		}
		return this.ip_addr;
	}

	
	/**
	 * Access the handle of the user session timer thread.
	 * @return The Timeout instance
	 */
	public final Timeout getSessionTimer() {
		if (this.sessionTimer == null) {
			synchronized(this) {
				if (this.sessionTimer == null)
					this.sessionTimer = new Timeout("EhrServer.SessionTimer");
			}
		}
		return this.sessionTimer;
	}
	/**
	 * Get the configured SAXParserFactory.
	 *
	 * <p>
	 * The implementation of the SAXParser factory is decided
	 * by the property <code>javax.xml.parsers.SAXParserFactory</code>
	 * if available in ClusterController, otherwise the JDK1.4 default
	 * <code>org.apache.crimson.jaxp.SAXParserFactoryImpl</code>is returned.
	 * </p>
	 * <p>The JDK 1.5 default would be
	 *    <code>com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl</code>
	 *
	 * @see #getDocumentBuilderFactory()
	 */
	public SAXParserFactory getSAXParserFactory() throws ServiceManagerException {
		if ( saxFactory == null) {
			try {
				//if (log.isLoggable(Level.FINEST)) log.finest(getProperty().toXml());

				String fac = getProperty().get(
						"javax.xml.parsers.SAXParserFactory", (String)null);
				if (fac == null)  {
					saxFactory = JAXPFactory.newSAXParserFactory();
				}
				else {
					saxFactory = JAXPFactory.newSAXParserFactory(fac);
				}
			} catch (FactoryConfigurationError e) {
				throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION_XML, ME, "SAXParserFactoryError", e);
			} // end of try-catch

		} // end of if ()
		return saxFactory;
	}
	   /**
	    * Sets the authentication in the engine.Global scope.
	    * <p>
	    * Additionally the I_Authentication is registered in the <i>common.Global.addObjectEntry</i>
	    * under the name <i>".../I_Authenticate"</i> (see Constants.I_AUTHENTICATE_PROPERTY_KEY).<br />
	    * This allows lookup similar to a naming service if we are in the same JVM.
	    */
	   public void setAuthenticate(I_Authenticate auth) {
	      this.authenticate = auth;
	      addObjectEntry(Constants.I_AUTHENTICATE_PROPERTY_KEY, this.authenticate);
	   }

	   public I_Authenticate getAuthenticate() {
	      return this.authenticate;
	   }
	//IBM1.4.2
	// <java.vendor>IBM Corporation</java.vendor>
	// <java.vm.vendor>IBM Corporation</java.vm.vendor>
	// <java.fullversion>J2RE 1.4.2 IBM build cxia32142-20060824 (SR6) (JIT enabled: jitc)</java.fullversion>
	// <java.vm.info>J2RE 1.4.2 IBM build cxia32142-20060824 (SR6) (JIT enabled: jitc)</java.vm.info>
	//IBM1.5
	//  <java.vendor>IBM Corporation</java.vendor>
	//  <java.vm.vendor>IBM Corporation</java.vm.vendor>
	//  <java.fullversion>J2RE 1.5.0 IBM J9 2.3 Linux amd64-64 j9vmxa6423-20060504 (JIT enabled)
	//  <java.vm.info>J2RE 1.5.0 IBM J9 2.3 Linux amd64-64 j9vmxa6423-20060504 (JIT enabled)
	//      J9VM - 20060501_06428_LHdSMr
	//      JIT  - 20060428_1800_r8
	//      GC   - 20060501_AA</java.vm.info>
	//  <java.vm.name>IBM J9 VM</java.vm.name>
	private final boolean isIbmVM() {
		String vm = System.getProperty("java.vm.vendor", "");
		if (vm.indexOf("IBM") != -1) return true;
		return false;
	}

	/**
	 * Get the configured  DocumentBuilderFactoryFactory.
	 *
	 * <p>
	 * The implementation of the  DocumentBuilderFactory is decided by the property
	 * <code>javax.xml.parsers.DocumentBuilderFactory</code> if available in ClusterController,
	 * otherwise the default <code>org.apache.crimson.jaxp.DocumentBuilderFactoryImpl</code>
	 * is returned for JDK 1.3 and smaller.
	 * </p>
	 * Currently only crimson is actually possible to use for JDK 1.3 and JDK 1.4
	 * (see ehrserver/lib/parser.jar#/META-INF/services setting)
	 * </p>
	 * <p>
	 * For JDK 1.5 the default delivered parser is used:
	 * <code>com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl</code>
	 * and ehrserver/lib/parser.jar and jaxp.jar are obsolete.
	 * For JDK 1.5 or higher any DOM Level 3 compliant parser should be OK.
	 * </p>
	 */
	public DocumentBuilderFactory getDocumentBuilderFactory() throws ServiceManagerException {
		if ( docBuilderFactory == null) {
			try {
				//if (log.isLoggable(Level.FINEST)) log.finest(getProperty().toXml());

				String fac = getProperty().get(
						"javax.xml.parsers.DocumentBuilderFactory", (String)null);
				if (fac == null) {
					docBuilderFactory =JAXPFactory.newDocumentBuilderFactory();
				}
				else {
					docBuilderFactory =JAXPFactory.newDocumentBuilderFactory(fac);
				}
			} catch (FactoryConfigurationError e) {
				throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION_XML, ME, "DocumentBuilderFactoryError", e);
			} // end of try-catch
		} // end of if ()
		return docBuilderFactory;
	}
	/**
	 * Get the configured  TransformerFactory.
	 *
	 * <p>The implementation of the   TransformerFactory is decided by the property
	 * <code>javax.xml.transform.TransformerFactory</code> if available in ClusterController,
	 * otherwise the default <code>org.apache.xalan.processor.TransformerFactoryImpl</code>
	 * is returned
	 * </p>
	 * <p>The JDK 1.5 default would be
	 * <code>com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl</code>
	 *
	 * @see #getDocumentBuilderFactory()
	 */
	//   public TransformerFactory getTransformerFactory() throws ServiceManagerException {
	//	       if ( transformerFactory == null) {
	//	          try {
	//	             String fac = getProperty().get(
	//	                   "javax.xml.transform.TransformerFactory", (String)null);
	//	             if (fac == null) {
	//	                transformerFactory =JAXPFactory.newTransformerFactory();
	//	             }
	//	             else {
	//	                transformerFactory =JAXPFactory.newTransformerFactory(fac);
	//	             }
	///*
	//	             String defaultFac = (XmlNotPortable.JVM_VERSION<=14) ?
	//	                "org.apache.xalan.processor.TransformerFactoryImpl" :
	//	                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
	//	             if (isIbmVM()) {
	//	                defaultFac =
	//	                "org.apache.xalan.processor.TransformerFactoryImpl";
	//	             }
	//
	//	             transformerFactory =JAXPFactory.newTransformerFactory(
	//	                getProperty().get(
	//	                   "javax.xml.transform.TransformerFactory", defaultFac));
	//*/
	//	          } catch (javax.xml.transform.TransformerFactoryConfigurationError e) {
	//	             throw new ServiceManagerException(this, ErrorCode.RESOURCE_CONFIGURATION_XML, ME, "TransformerFactoryError", e);
	//	          } // end of try-catch
	//	       } // end of if ()
	//	       return transformerFactory;
	//   }


	/**
	 * This notation is URLEncoder since JDK 1.4.
	 * @param enc If null it defaults to "UTF-8"
	 */
	public static String encode(String s, String enc) {
		try {
			return java.net.URLEncoder.encode(s, (enc==null) ? Constants.UTF8_ENCODING : enc);
		} catch (UnsupportedEncodingException e) {
			System.out.println("PANIC in encode(" + s + ", " + enc + "): " + e.toString());
			e.printStackTrace();
			return s;
		}
	}

	/**
	 * This notation is URLDecoder since JDK 1.4.
	 * @param enc If null it defaults to "UTF-8"
	 */
	public static String decode(String s, String enc) {
		try {
			return java.net.URLDecoder.decode(s, (enc==null) ? Constants.UTF8_ENCODING : enc);
		}
		catch (Exception e) {
			System.out.println("PANIC in decode(" + s + ", " + enc + "): " + e.toString());
			e.printStackTrace();
			return s;
		}
	}


	public void finalize() {
		try {
			//if (log.isLoggable(Level.FINE)) log.fine("Entering finalize");
			shutdown();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		try {
			super.finalize();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		if (this.isDoingShutdown) {
			return;
		}
		this.isDoingShutdown = true;

		//if (log != null && log.isLoggable(Level.FINE)) log.fine("Destroying common.ClusterController handle");

		/* This is a singleton, so only the last ClusterController instance may do a shutdown
	       try {
	          getJmxWrapper().shutdown();
	       }
	       catch (ServiceManagerException e) {
	          log.warn(ME, "Ignoring: " + e.toString());
	       }
		 */

		//	       shutdownHttpServer();

		/*
	       if (supportJmx()) {
	       try {
	          unregisterJmx();
	       }
	       catch (ServiceManagerException e) {
	          log.warn(ME, "Ignoring exception during JMX unregister: " + e.getMessage());
	       }
	       }
		 */
		synchronized (RunTimeSingleton.class) {
			if (firstInstance != null && this == firstInstance) {
				//System.out.println("###################################First instance of ClusterController destroyed");
				firstInstance = null;
			}
		}

		if (sessionTimer != null){
			sessionTimer.removeAll();
			sessionTimer.shutdown();
		}
		
		this.isDoingShutdown = false;
	}


	/**
	 * Prints the stack trace logonservice a String so it can be put on the normal logs.
	 * @param ex The exception for which to write out the stack trace. If you pass null it will print the Stack trace of
	 * a newly created exception.
	 * @return The Stack trace logonservice a String.
	 */
	public static String getStackTraceAsString(Throwable ex) {
		// this is just to send the stack trace to the log file (stderr does not go there)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream pstr = new PrintStream(baos);
		if (ex == null)
			ex = new Exception();
		ex.printStackTrace(pstr);
		return new String(baos.toByteArray());
	}

	/**
	 * Command line usage.
	 * <p />
	 * These variables may be set in your property file logonservice well.
	 * Don't use the "-" prefix there.
	 * <p />
	 * Set the verbosity when loading properties (outputs with System.out).
	 * <p />
	 * 0=nothing, 1=info, 2=trace, configure with
	 * <pre>
	 * java -Dproperty.verbose 2 ...
	 *
	 * java org.ehrserver.Main -property.verbose 2
	 * </pre>
	 */
	public String usage() {
		StringBuilder sb = new StringBuilder(4028);
		sb.append(logUsage());
		return sb.toString();
	}

	public static String logUsage() {
		StringBuilder sb = new StringBuilder(1024);
		sb.append("Control properties framework:\n");
		sb.append("   -propertyFile <file> Specify an services.properties file to load.\n");
		sb.append("                        The contained settings overwrite a property file found in the ehrserver.jar file.\n");
		sb.append("   -property.verbose   0 switches logging off, 2 is most verbose when loading properties on startup [" + Property.DEFAULT_VERBOSE + "].\n");
		sb.append("   -servicesFile  <file> Specify an services.xml property file to load.\n");
		sb.append("                        The contained settings overwrite a services file found in the ehrserver.jar file.\n");
		return sb.toString();
	}

	/** To play with a profiling tool */
	public static void main(String[] args) {
		System.out.println("NO ClusterController, Hit a key");
	}

	/**
	 * It searches for the given property.
	 * The replacement for '${...}' is supported. Note that the assignment of a '$'
	 * variable can only be done in ClusterController scope, that is in the services.properties or command line,
	 * and JVM properties but not in the services.xml.
	 *
	 * It first looks into the map (the hardcoded properties). If one is found it is returned.
	 * Then it looks into the ClusterController. If one is found it is returned. If none is found it is
	 * searched in the service
	 * @param shortKey the key (in its short form without prefix) of the property
	 * @param defaultValue the default value of the property (weakest)
	 * @param map the hardcoded properties (strongest)
	 * @param serviceConfig the serviceConfig used, checks the properties from PluginInfo
	 * @return
	 */
	public String get(String shortKey, String defaultValue, Properties map, I_ServiceConfig serviceConfig)
			throws ServiceManagerException {
		try {
			if (shortKey == null) {
				return defaultValue;
			}
			String ret = (serviceConfig == null) ? defaultValue : serviceConfig.getParameters().getProperty(shortKey, defaultValue);
			String prefix = (serviceConfig == null) ? "" : serviceConfig.getPrefix();  // "service/" + getType() + "/"
			ret = getProperty().get(shortKey, ret); // without prefix (ClusterController) is weaker than with specific prefix
			ret = getProperty().get(prefix + shortKey, ret);
			if (map != null)
				ret = map.getProperty(shortKey, ret);
			return getProperty().replaceVariableWithException(shortKey, ret);
		}
		catch (ServiceManagerException ex) {
			throw new ServiceManagerException(this, SysErrorCode.USER_CONFIGURATION, ME + ".get", "exception when getting property '" + shortKey + "'", ex);
		}
	}

	/**
	 *
	 * @param shortKey
	 * @param defaultValue
	 * @param map
	 * @param serviceConfig
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @see get(String, String, map, I_PluginConfig)
	 */
	public long get(String shortKey, long defaultValue, Properties map, I_ServiceConfig serviceConfig)
			throws ServiceManagerException {
		String tmp = get(shortKey, null, map, serviceConfig);
		if (tmp == null) // should never happen
			return defaultValue;
		try {
			return Long.parseLong(tmp);
		}
		catch (Throwable ex) {
			throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION, ME + ".get", "wrong type for '" + shortKey + "': should be long but is '" + tmp + "'");
		}
	}

	/**
	 *
	 * @param shortKey
	 * @param defaultValue
	 * @param map
	 * @param serviceConfig
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @see get(String, String, map, I_PluginConfig)
	 */
	public int get(String shortKey, int defaultValue, Properties map, I_ServiceConfig serviceConfig)
			throws ServiceManagerException {
		String tmp = get(shortKey, null, map, serviceConfig);
		if (tmp == null) // should never happen
			return defaultValue;
		try {
			return Integer.parseInt(tmp);
		}
		catch (Throwable ex) {
			throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION, ME + ".get", "wrong type for '" + shortKey + "': should be int but is '" + tmp + "'");
		}
	}

	/**
	 * Checks PluginInfo logonservice well.
	 * @param shortKey
	 * @param defaultValue
	 * @param map
	 * @param serviceConfig
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @see get(String, String, map, I_PluginConfig)
	 */
	public boolean get(String shortKey, boolean defaultValue, Properties map, I_ServiceConfig serviceConfig)
			throws ServiceManagerException {
		String tmp = get(shortKey, null, map, serviceConfig);
		if (tmp == null) // should never happen
			return defaultValue;
		try {
			return Boolean.valueOf(tmp).booleanValue();
		}
		catch (Throwable ex) {
			throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION, ME + ".get", "wrong type for '" + shortKey + "': should be boolean but is '" + tmp + "'");
		}
	}

	/**
	 *
	 * @param shortKey
	 * @param defaultValue
	 * @param map
	 * @param serviceConfig
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @see get(String, String, map, I_PluginConfig)
	 */
	public double get(String shortKey, double defaultValue, Properties map, I_ServiceConfig serviceConfig)
			throws ServiceManagerException {
		String tmp = get(shortKey, null, map, serviceConfig);
		if (tmp == null) // should never happen
			return defaultValue;
		try {
			return Double.parseDouble(tmp);
		}
		catch (Throwable ex) {
			throw new ServiceManagerException(this, SysErrorCode.RESOURCE_CONFIGURATION, ME + ".get", "wrong type for '" + shortKey + "': should be double but is '" + tmp + "'");
		}
	}

	/**
	 * Reset the cached instance id
	 */
	public void resetInstanceId() {
		synchronized(this) {
			this.instanceId = null;
		}
	}

	/**
	 * Unique id of the client, changes on each restart.
	 * If 'client/joe' is restarted, the instanceId changes.
	 * @return id + timestamp, '/client/joe/instanceId/33470080380'
	 */
	public String getInstanceId() {
		if (this.instanceId == null) {
			synchronized(this) {
				if (this.instanceId == null) {
					ContextNode node = new ContextNode("instanceId", ""+System.currentTimeMillis(),
							getContextNode());
					this.instanceId = node.getAbsoluteName();
				}
			}
		}
		return this.instanceId;
	}


	/**
	 * Access a file from the CLASSPATH, typically from ehrserver.jar
	 *
	 * It is searched in the directory of the package of the calling java class
	 * <tt>org.ehrserver.common.http</tt> => <tt>org/ehrserver/common/http</tt>
	 * @param file The file to lookup
	 * @return The byte[] of the found file
	 * @exception  IOException  if an I/O error occurs.
	 *             or IllegalArgumentException if not found
	 */
	public static byte[] getFromClasspath(String file, Object location) {
		try {
			//java.lang.IllegalArgumentException: Can't handle unknown status.html
			//  at org.ehrserver.common.ClusterController.getFromClasspath(ClusterController.java:2122)
			//  at org.ehrserver.contrib.htmlmonitor.HtmlMonitorPlugin.service(HtmlMonitorPlugin.java:151)
			//  at org.ehrserver.common.http.HandleRequest.run(HttpIORServer.java:368)
			// I had to throw it into org/ehrserver/contrib/htmlmonitor to be found because location was 'this' instance of HtmlMonitorPlugin
			java.net.URL oUrl = location.getClass().getResource(file); // "favicon.ico"
			if (oUrl != null) {
				InputStream in = oUrl.openStream();

				int size = 10;
				byte[] tmp = new byte[size];
				ByteArrayOutputStream bo = new ByteArrayOutputStream(size);
				while (in.available() > 0) {
					int length = in.read(tmp);
					if (length > 0)
						bo.write(tmp, 0, length);
				}
				in.close();
				return bo.toByteArray();
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Can't find " + file + ": " + e.toString());
		}
		throw new IllegalArgumentException("Can't handle unknown " + file);
	}

	/**
	 * Build a nice, human readable string for the size in MB/KB/Bytes.
	 * <br><b>Example:</b><br>
	 * <code>System.out.println(Memory.DataLenStr(136000));</code><br>
	 *  -> "136 KB"
	 * @param size is the size in bytes
	 * @return a nice readable memory string
	 */
	public static final String byteString(long size) {
		// 1060970496 bytes  532742144 bytes
		// 1.060 GBytes      532.742 MByte
		long gBytes = size / 1000000000L;
		long mBytes = size % 1000000000L / 1000000L;
		long kBytes = size % 1000000L / 1000L;
		long bytes = size % 1000L;
		String str;
		if (gBytes != 0) {
			long a = Math.abs(mBytes);
			String z = (a < 10) ? "00" : ((a < 100) ? "0" : "");
			str = "" + gBytes + "." + z + a + " GBytes";
		}
		else {
			if (mBytes != 0) {
				long a = Math.abs(kBytes);
				String z = (a < 10) ? "00" : ((a < 100) ? "0" : "");
				str = "" + mBytes + "." + z + a + " MBytes";
			}
			else
				if (kBytes != 0) {
					long a = Math.abs(bytes);
					String z = (a < 10) ? "00" : ((a < 100) ? "0" : "");
					str = "" + kBytes + "." + z + a + " KBytes";
				}
				else
					str = "" + bytes + " Bytes";
		}
		return str;
	}

	/**
	 * http://www.ehrserver.org/ehrserver/doc/api/org/ehrserver/common/admin/I_AdminPop3Driver.html#setPollingInterval(long)
	 * @param className
	 * @param methodName
	 * @return http://www.ehrserver.org/ehrserver/doc/api/org/ehrserver/common/admin/I_AdminPop3Driver.html#setPollingInterval(long)
	 */
	public static String getJavadocUrl(String className, String methodName) {
		String prefix = "http://www.ehrserver.org/ehrserver/doc/api/";
		className = ReplaceVariable.replaceAll(className, ".", "/");
		String url = prefix + className + ".html";
		if (methodName != null)
			url += "#" + methodName;
		return url;
	}

	/**
	 * http://www.ehrserver.org/ehrserver/doc/api/org/ehrserver/common/admin/I_AdminPop3Driver.html#setPollingInterval(long)
	 * @param className
	 * @param methodName
	 * @return
	 */
	public static String getJmxUsageLinkInfo(String className, String methodName) {
		return "\n\nUsage Details:"
				+ "\n" + getJavadocUrl(className, methodName);
	}

	/**
	 * Returns a persistent map.
	 * @param id The id identifying the map. Normally this would be the sessionId. If you pass null or an empty String, then a default map is returned.
	 * @return the persistent map.
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public Map<?, ?> getPersistentMap(String id) throws ServiceManagerException {
		log.error("Not yet implemented");
		return null;
		/*
	       if (id == null | id.trim().length() < 1)
	          id = "defaultPersistentMap";
	       Map map = new PersistentMap(this, id, 0L, 0L);
	       return map;
		 */
	}

	/**
	 * Do some garbage collect attempts
	 */
	public static void gc(int numGc, long sleep) {
		for (int ii=0; ii<numGc; ii++) {
			System.gc();
			try { Thread.sleep(sleep); } catch( InterruptedException i) {}
		}
	}

	/**
	 * Access a nice, human readable string with the current RAM memory situation.
	 * @return a nice readable memory statistic string
	 */
	public static final String getMemoryStatistic() {
		StringBuilder statistic = new StringBuilder(256);
		statistic.append("Total memory allocated = ");
		statistic.append(byteString(Runtime.getRuntime().totalMemory()));
		statistic.append(".");
		statistic.append(" Free memory available = ");
		statistic.append(byteString(Runtime.getRuntime().freeMemory()));
		statistic.append(".");
		return statistic.toString();
	}

	public void putInWeakRegistry(Object key, Object value) {
		synchronized(weakRegistry) {
			if (key != null)
				weakRegistry.put(key, value);
		}
	}

	public Object getFromWeakRegistry(Object key) {
		synchronized(weakRegistry) {
			if (key == null)
				return null;
			return weakRegistry.get(key);
		}
	}

	public Object removeFromWeakRegistry(Object key) {
		synchronized(weakRegistry) {
			if (key != null)
				return weakRegistry.remove(key);
		}
		return null;
	}


	/**
	 * Returns the service manager used by the run level manager. All other specific
	 * Managers extend this class and reference the cache on this instance.
	 */
	public ServiceManagerBase getServiceManager() {
		if (this.serviceManager == null) {
			synchronized(this) {
				if (this.serviceManager == null)
					this.serviceManager = new ServiceManagerBase(this);
			}
		}
		return this.serviceManager;
	}


	/**
	 * Returns the service registry.
	 */
	public ServiceRegistry getServiceRegistry() {
		if (this.serviceRegistry == null) {
			synchronized(this) {
				if (this.serviceRegistry == null)
					this.serviceRegistry = new ServiceRegistry(this);
			}
		}
		return this.serviceRegistry;
	}

	/**
	 * Initialize runlevel manager used to start/stop ehrserver with different run levels.
	 */
	public final RunlevelManager getRunlevelManager() {
		if (this.runlevelManager == null) {
			boolean initJmx = false;
			synchronized(this) {
				if (this.runlevelManager == null) {
					this.runlevelManager = new RunlevelManager(this);
					initJmx = true;
				}
			}
			if (initJmx) {

				this.runlevelManager.initJmx();
			}
		}
		return this.runlevelManager;
	}

	public int getRunlevel() {
		return this.currRunlevel;
	}

	public String getDump() throws ServiceManagerException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(100000);
		getDump(out);
		try {
			return out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return out.toString();
		}
	}

	public void getDump(OutputStream out) throws ServiceManagerException {
		try {
			StringBuffer sb = new StringBuffer(10000);
			String offset = "\n";
			sb.append(offset).append("<ehrserver id='").append(getId()).append("'");
			sb.append(" version='").append(getVersion()).append("' counter='").append(counter).append("'");
			sb.append("\n   ");
			sb.append(" buildTimestamp='").append(getBuildTimestamp()).append("'");
			sb.append(" buildJavaVendor='").append(getBuildJavaVendor()).append("'");
			sb.append(" buildJavaVersion='").append(getBuildJavaVersion()).append("'");
			sb.append("\n   ");
			sb.append(" java.vendor='").append(System.getProperty("java.vendor")).append("'");
			sb.append(" java.version='").append(System.getProperty("java.version")).append("'");
			sb.append("\n   ");
			sb.append(" os.name='").append(System.getProperty("os.name")).append("'");
			sb.append(" os.version='").append(System.getProperty("os.version")).append("'");
			sb.append("\n   ");
			sb.append(" freeMemory='").append(Runtime.getRuntime().freeMemory()).append("'");
			sb.append(" totalMemory='").append(Runtime.getRuntime().totalMemory()).append("'");
			sb.append("\n   ");
			sb.append(" dumpTimestamp='").append(IsoDateParser.getCurrentUTCTimestamp()).append("'");
			// sb.append(" ='").append(get()).append("'");
			sb.append(">");
			out.write(sb.toString().getBytes("UTF-8"));

			out.write(getProperty().toXml().getBytes("UTF-8"));
			out.write((offset + " <ThreadDump><![CDATA[").getBytes("UTF-8"));
			out.write(ThreadLister.listAllThreads().getBytes("UTF-8"));
			out.write((offset + " ]]></ThreadDump>").getBytes("UTF-8"));

			out.write((offset + "</ehrserver>").getBytes("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * gets the object holding all configuration information for the services (both for
	 * statically loaded services (by the run level manager) and dynamically loaded
	 * services (such services loaded on client request).
	 */
	public ServiceHolder getServiceHolder() throws ServiceManagerException {
		if (this.serviceHolder != null) return this.serviceHolder;
		synchronized(this) {
			if (this.serviceHolder == null) {
				//TODO
				// Check to use annotation base configuration
				ServiceHolderFactory factory = new ServiceHolderAnnotationFactory(this);
				this.serviceHolder = factory.loadServiceHolder();
			}
			return this.serviceHolder;
		}
	}

	/**
	 * The unique name of this ehrserver server instance.
	 * @return Can be null during startup
	 */
	public final NodeId getNodeId() {
		return this.nodeId;
	}



}

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


import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.StringPairTokenizer;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import javax.security.auth.Subject;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.*;

// since JDK 1.5 (instead of javax.management.MBeanServerFactory from separate jmxri.jar from http://java.sun.com/products/JavaManagement/download.html)
//import java.lang.management.ManagementFactory;


/**
 * JmxWrapper wraps the MBeanServer instance.
 * Current supported adaptors are a HTTPAdaptor, the SoaBaseAdaptor and the JDK1.5 jconsole adaptor.
 * <table>
 * <tr>
 * <td>1.</td>
 * <td>ehrserver/jmx/HtmlAdaptor</td>
 * <td>true: Start the adaptor for html-browser access</td>
 * </tr>
 * <tr>
 * <td>2.</td>
 * <td>ehrserver/jmx/SoaBaseAdaptor</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td>3a.</td>
 * <td>java -Dcom.sun.management.jmxremote ...</td>
 * <td>Start the JDK 1.5 jconsole adaptor by the java virtual machine</td>
 * </tr>
 * <tr>
 * <td>3b.</td>
 * <td>ehrserver/jmx/rmi</td>
 * <td>true: Start the JDK 1.5 jconsole adaptor by our own coding</td>
 * </tr>
 * </table>
 * <p />
 * Note for 3b.:<br />
 * A rmiregistry server is created automatically. If there is running already one, that is used.<br />
 * You can specify another port or host to create/use a rmiregistry server:
 * <pre>
 *     -ehrserver/jmx/rmiregistry/port   Specify a port number where rmiregistry listens.
 *                         Default is port 1099
 *     -ehrserver/jmx/rmiregistry/hostname Specify a hostname where rmiregistry runs.
 *                         Default is the dns name of the running host.
 * </pre>
 * @see http://java.sun.com/developer/technicalArticles/J2SE/jmx.html
 * @see org.ehrserver.util.ehrserverSecurityManager
 * @see <a href="http://www.ehrserver.org/ehrserver/doc/requirements/admin.jmx.html">The admin.jmx requirement</a>
 */
public class JmxWrapper
{
   private final RunTimeSingleton glob;
   private static Logger log = LogManager.getLogger(JmxWrapper.class);
   private final String ME;
   private MBeanServer mbeanServer;
   private int useJmx;
   /**
    * We hold an own map for mbeans registered to support renaming.
    * The key is the objectName.toString() and the value is the JmxMBeanHandle instance.
    */
   private Map mbeanMap = new HashMap();
   /** Export ClusterController.getProperty() to JMX */
   private JmxProperties jmxProperties;
   private JmxMBeanHandle jmxPropertiesHandle;
   private JmxLogLevel jmxLogLevel;
   private JmxMBeanHandle jmxLogLevelHandle;
   /** ehrserver RMI registry listen port is 1099, to access for bootstrapping */
   public static final int DEFAULT_REGISTRY_PORT = 1099;
   private static JmxWrapper theJmxWrapper;
   private boolean createSecurityManager = true;

   /**
    * Singleton to avoid that different ClusterController instances create more than one JmxWrapper.
    */
   public static JmxWrapper getInstance(RunTimeSingleton glob) throws ServiceManagerException {
      if (theJmxWrapper == null) {
         synchronized (JmxWrapper.class) {
            if (theJmxWrapper == null) {
               theJmxWrapper = new JmxWrapper(glob);
            }
         }
      }
      return theJmxWrapper;
   }

   /**
    * Constructs an initial JmxWrapper object.
    */
   public JmxWrapper(RunTimeSingleton glob) throws ServiceManagerException
   {
      this.glob = glob;

      this.ME = "JmxWrapper" + this.glob.getLogPrefixDashed();

 	/*
 	If having an embedded jboss EJB3 plugin you have two options:

 	1. Switch off our JMX support
      java org.ehrserver.Main -ehrserver/jmx/support false

 	2. or initialize jboss directly in main() (you need to edit and recompile ehrserver)
 	      try { // JMX conflict with embedded jboss workaround
 			  ...
            EJB3StandaloneBootstrap.scanClasspath("myproject/build"); // this creates the conflict
 			  ...
          } catch (Exception e) {
            e.printStackTrace();
          }

 	   and then start with:
        java org.ehrserver.Main -ehrserver/jmx/rmi true -ehrserver/jmx/rmi/user joe -ehrserver/jmx/rmi/password xyz
        jconsole service:jmx:rmi:///jndi/rmi://127.0.0.2:1099/jmxrmi
 	*/
      // Embedded jboss seems to has its own and if this is initialized first our ehrserverSecurityManager.createSecurityManager(glob); call fails because of ehrserver.policy problems
      this.createSecurityManager = glob.getProperty().get("ehrserver/jmx/createSecurityManager", this.createSecurityManager);

		boolean supportJmx = glob.getProperty().get("ehrserver/jmx/support", true);
		if (!supportJmx) { // Embedded jboss can't handle it if we initialize JMS ourself
			log.warn("Any JMX support is switched off with 'ehrserver/jmx/support=false'");
         return;
      }

      getMBeanServer(); // Initialize this.mbeanServer
      init();
      if (useJmx > 0) {
         // Export ClusterController.getProperty() to JMX
         this.jmxProperties = new JmxProperties(this.glob);
         ContextNode propNode = new ContextNode(ContextNode.SYSPROP_MARKER_TAG, "sysprop", this.glob.getContextNode());
         this.jmxPropertiesHandle = registerMBean(propNode, jmxProperties); // "sysprop"

         this.jmxLogLevel = new JmxLogLevel(this.glob);
         ContextNode logNode = new ContextNode(ContextNode.LOGGING_MARKER_TAG, "logging", this.glob.getContextNode());
         this.jmxLogLevelHandle = registerMBean(logNode, jmxLogLevel); // "logging"
      }

      if (useJmx > 0 && this.mbeanServer != null) {
         // display some statistics ...
         try {
            // javap com.sun.management.UnixOperatingSystem
            ObjectName name = new ObjectName("java.lang:type=OperatingSystem");
            Object obj = this.mbeanServer.getAttribute(name, "TotalPhysicalMemorySize");
            RunTimeSingleton.totalPhysicalMemorySize = (obj instanceof Long) ? ((Long)obj).longValue() : 0;
            obj = this.mbeanServer.getAttribute(name, "CommittedVirtualMemorySize");
            //long committed = (obj instanceof Long) ? ((Long)obj).longValue() : 0;
            obj = this.mbeanServer.getAttribute(name, "MaxFileDescriptorCount");
            RunTimeSingleton.maxFileDescriptorCount = (obj instanceof Long) ? ((Long)obj).longValue() : 0;


            name = new ObjectName("java.lang:type=Memory");
            obj = this.mbeanServer.getAttribute(name, "HeapMemoryUsage");
            if (obj instanceof javax.management.openmbean.CompositeDataSupport) {
               //java.lang.management.MemoryUsage.getMax()
               javax.management.openmbean.CompositeDataSupport comp =
                 (javax.management.openmbean.CompositeDataSupport)obj;
               Long max = (Long)comp.get("max");
               RunTimeSingleton.heapMemoryUsage = max.longValue();
            }

            log.info("Physical RAM size is " + RunTimeSingleton.byteString(RunTimeSingleton.totalPhysicalMemorySize) + "," +
                         " this JVM may use max " + RunTimeSingleton.byteString(RunTimeSingleton.heapMemoryUsage) +
                         " and max " + RunTimeSingleton.maxFileDescriptorCount + " file descriptors");

         }
         catch (Exception e) {
         }
      }
   }

   /**
    * Check if JMX is activated.
    * @return true if JMX is in use
    */
   public boolean isActivated() {
      return (this.mbeanServer != null) && (this.useJmx != 0);
   }

   /**
    * Create the unique MBeanServer instance.
    */
   public MBeanServer getMBeanServer() {
      if (this.mbeanServer == null) {
         synchronized (this) {
            if (this.mbeanServer == null) {
               try {
                  Class clazz = java.lang.Class.forName("java.lang.management.ManagementFactory");
                  if (clazz != null) {
                     // this.mbeanServer = ManagementFactory.getPlatformMBeanServer(); // new for JDK 1.5
                     java.lang.reflect.Method method = clazz.getMethod("getPlatformMBeanServer", new Class[0]);
                     this.mbeanServer = (javax.management.MBeanServer)method.invoke(clazz, (Object[])new Class[0]);
                  }
               }
               catch (Exception e) {
                  log.debug("java.lang.management.ManagementFactory is not available for JMX monitoring: " + e.toString());
               }
               if (this.mbeanServer == null) {
                  // For JDK < 1.5 fall back to
                  //  JMX Remote API 1.0.1_03 Reference Implementation
                  //  JMX 1.2.1 Reference Implementation
                  //  Download from http://java.sun.com/products/JavaManagement/download.html
                  String hostname = glob.getProperty().get("ehrserver/jmx/hostname", glob.getLocalIP());
                  this.mbeanServer = MBeanServerFactory.createMBeanServer(hostname);
               }
            }
         }
      }
      return this.mbeanServer;
   }

   /**
    * JMX property values may not contain a comma ','.
    * Here we replace commas with an underscore.
    * Even if we use quoted ObjectName values the comma is not allowed.
    * <br />
    * Additionally we replace '/' logonservice these would break the admin telnet commands
    * syntax, it is nice to be able to use those interchangeable
    * @param value The value to verify
    * @return The beautified value to be usable logonservice a value for JMX properties
    */
   public static String validateJmxValue(String value) {
      if (value == null) return value;
      while (true) {
         int index = value.indexOf(",");
         if (index >= 0)
            value = value.substring(0,index) + "_" + value.substring(index+1);
         else
            break;
      }

      while (true) {
         int index = value.indexOf("/");
         if (index >= 0)
            value = value.substring(0,index) + "_" + value.substring(index+1);
         else
            break;
      }
      return value;
   }

   /**
    * Initialize the MBean server and adaptors.
    */
   public synchronized void init() throws ServiceManagerException {

      if (this.mbeanServer == null) return;

      boolean supportsJconsole = true;
      try {
         if (new Float(System.getProperty("java.runtime.version").substring(0,3)).floatValue() < 1.5)
            supportsJconsole = false;
      }
      catch (Throwable e) {
         log.debug(e.toString());
      }

      // In a cluster environment set for example jndiPath=="/"+${cluster.node.id}
      String jndiPath = glob.getProperty().get("ehrserver/jmx/rmiregistry/jndiPath", "/jmxrmi"); // "/jmxconnector";

      String ssl = System.getProperty("com.sun.management.jmxremote.ssl");
      //String aut = System.getProperty("com.sun.management.jmxremote.authenticate");
      String pwd = System.getProperty("com.sun.management.jmxremote.password.file");
      //boolean isAuthenticated = !((ssl==null||"false".equals(ssl)) && (aut==null||"false".equals(aut)));
      boolean isAuthenticated = !(ssl==null||"false".equals(ssl)) || pwd!=null;

      if (System.getProperty("com.sun.management.jmxremote.port") != null) {
         // For localhost or remote access with specific port
         // You have to configure authentication!
         // JDK >= 1.5 automatically creates an RMI connector and start it for us
         int port = new Integer(System.getProperty("com.sun.management.jmxremote.port")).intValue();
         if (supportsJconsole) {
            String loc = "service:jmx:rmi:///jndi/rmi://"+glob.getLocalIP()+":"+port+jndiPath;
            log.info("'java -Dcom.sun.management.jmxremote.port=" + port +
                         "' is specified, JMX is switched on, try to start 'jconsole " + loc + "'");
            if (!isAuthenticated)
               log.warn("Caution: Your JMX access is not protected with SSL or password");
            useJmx++;
         }
         else {
            log.info("'java -Dcom.sun.management.jmxremote.port="+port+"' is specified "+
                         " but is not supported in this runtime " + System.getProperty("java.runtime.version"));
         }
      }
      else if (System.getProperty("com.sun.management.jmxremote") != null) {
         // For localhost or remote access with default port 1099
         // You have to configure authentication!
         // JDK >= 1.5 automatically creates an RMI connector and start it for us
         String loc = "service:jmx:rmi:///jndi/rmi://"+glob.getLocalIP()+":"+DEFAULT_REGISTRY_PORT+jndiPath;
         if (supportsJconsole) {
            log.info("'java -Dcom.sun.management.jmxremote' is specified, JMX is switched on, try to start 'jconsole' or 'jconsole "+loc+"'");
            if (!isAuthenticated)
               log.warn("Caution: Your JMX access is not protected with SSL or password");
            useJmx++;
         }
         else {
            log.info("'java -Dcom.sun.management.jmxremote' is specified "+
                         " but is not supported in this runtime " + System.getProperty("java.runtime.version"));
         }
      }
      else if (glob.getProperty().get("ehrserver/jmx/rmi", false)) {
         // JDK >= 1.5: Create manually an RMI connector and start it
         // If there is no rmiregistry around we start one
         // url ::= service:jmx:protocol:sap
         // sap ::= //[host[:port]][url-path]
         //
         // JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost:3000/jndi/rmi://localhost:9999/server");
         //
         //   which tells the RMIConnectorServer to bind to the RMIRegistry running
         //   on port 9999 and to export the RMIServer and RMIConnection remote objects on port 3000.
         if (!supportsJconsole) {
            log.warn("JMX setting '-ehrserver/jmx/rmi true' ignored in this JVM runtime, you need JDK 1.5 or higher.");
         }
         else {
            try {
               // Force to export the RMIServer and RMIConnection remote objects on host and port
               int rmiServerPort = glob.getProperty().get("ehrserver/jmx/rmiserver/port", 6011);
               String rmiServerHost = glob.getProperty().get("ehrserver/jmx/rmiserver/hostname", (String)null);

               // Force the RMI registry
               int rmiRegistryPort = glob.getProperty().get("ehrserver/jmx/rmiregistry/port", DEFAULT_REGISTRY_PORT); // 1099
               String rmiRegistryHost = glob.getProperty().get("ehrserver/jmx/rmiregistry/hostname", glob.getLocalIP());
               startRmiRegistry(rmiRegistryHost, rmiRegistryPort);

               String bindName = "rmi://"+rmiRegistryHost+":"+rmiRegistryPort+jndiPath;

               try {  // If an external rmiregistry is running cleanup old entries:
                  Naming.unbind(bindName);
               } catch (Exception e) {
                  log.debug("Can't unbind '" + bindName + "': " + e.toString());
               }

               //String loc = "service:jmx:rmi://"+rmiRegistryHost+":"+rmiRegistryPort+"/jndi/" + bindName;
               String loc = "service:jmx:rmi:///jndi/" + bindName;
               if (rmiServerHost != null) {
                  loc = "service:jmx:rmi://" + rmiServerHost + ":" + rmiServerPort + "/jndi/" + bindName;
               }

               //since JDK 1.5 or with including jmxremote.jar for JDK 1.3/1.4
               final String storedUser = glob.getProperty().get("ehrserver/jmx/rmi/user", (String)null);
               final String storedPassword = glob.getProperty().get("ehrserver/jmx/rmi/password", (String)null);
               Map props = new TreeMap();
               if (storedUser != null) {
                  javax.management.remote.JMXAuthenticator auth = new javax.management.remote.JMXAuthenticator() {
                     public javax.security.auth.Subject authenticate(Object credentials) {
                        log.debug("Calling authenticate(" + ((credentials==null)?"null":credentials.toString()) + ")");
                        if (!(credentials instanceof String[])) throw new SecurityException("ehrserver responds: Bad credentials, please pass user name and password");
                        String[] creds = (String[])credentials;
                        if (creds.length != 2) throw new SecurityException("ehrserver responds: Bad credentials, please pass user name and password");

                        String user = creds[0];
                        String password = creds[1];
                        log.debug("Calling authenticate(user=" + user + ", password=" + password + ")");

                        if (password == null) throw new SecurityException("ehrserver responds: Missing password");
                        if (!storedUser.equals(user)) throw new SecurityException("ehrserver responds: Unknown user " + user + ",  please try with user '" + storedUser + "'");
                        if (!storedPassword.equals(password)) throw new SecurityException("ehrserver responds: Bad password, please try again");

                        Set principals = new HashSet();
                        principals.add(new javax.management.remote.JMXPrincipal(user));
                        return new Subject(true, principals, Collections.EMPTY_SET, Collections.EMPTY_SET);
                     }
                  };
                  props.put("jmx.remote.authenticator", auth); // JMXConnectorServer.AUTHENTICATOR
               }
               else {
                  log.warn("You should switch on authentication with '-ehrserver/jmx/rmi/user' and '-ehrserver/jmx/rmi/password'");
               }

               //since JDK 1.5 or with including jmxremote.jar for JDK 1.3/1.4
               //javax.management.remote.JMXServiceURL url = new javax.management.remote.JMXServiceURL(loc);
               //javax.management.remote.JMXConnectorServer cs = javax.management.remote.JMXConnectorServerFactory.newJMXConnectorServer(url, props, getMBeanServer());
               //cs.start();

               // JDK 1.3 and 1.4: Not available so we need to use reflection
               Class clazz = java.lang.Class.forName("javax.management.remote.JMXServiceURL");
               Class[] paramCls =  new Class[] { java.lang.String.class }; // url location
               Object[] params = new Object[] { loc };
               java.lang.reflect.Constructor ctor = clazz.getConstructor(paramCls);
               Object jMXServiceURL = ctor.newInstance(params);

               clazz = java.lang.Class.forName("javax.management.remote.JMXConnectorServerFactory");
               paramCls = new Class[] {
                  java.lang.Class.forName("javax.management.remote.JMXServiceURL"),
                  java.util.Map.class,                   // properties
                  javax.management.MBeanServer.class
               };
               params = new Object[] {
                  jMXServiceURL,
                  props,
                  getMBeanServer()
               };
               java.lang.reflect.Method method = clazz.getMethod("newJMXConnectorServer", paramCls);
               Object jMXConnectorServer = method.invoke(clazz, params); // Returns "JMXConnectorServer"

               clazz = java.lang.Class.forName("javax.management.remote.JMXConnectorServer");
               paramCls = new Class[0];
               params = new Object[0];
               method = clazz.getMethod("start", paramCls);
               method.invoke(jMXConnectorServer, params); // finally ... starts JMX

               log.info("JMX is switched on because of 'ehrserver/jmx/rmi true' is set, try to start 'jconsole " + loc + "' and user=" + storedUser);
               useJmx++;
            }
            catch(Throwable ex) {
               ex.printStackTrace();
               throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_UNAVAILABLE, ME, " could not create JMXConnectorServer", ex);
            }
         }
      }
      else {
         if (supportsJconsole) {
            log.info("JMX over RMI is switched off, for details see http://www.ehrserver.org/ehrserver/doc/requirements/admin.jmx.html#jconsole");
         }
      }


      if (glob.getProperty().get("ehrserver/jmx/SoaBaseAdaptor", false)) {
         try {
            startServiceManagerConnector(this.mbeanServer);
            useJmx++;
         }
         catch (ServiceManagerException ex) {
            log.error("Error when starting ehrserverConnector " + ex.toString());
            ex.printStackTrace();
         }
      }

      if (this.useJmx > 0) {
         boolean observeLowMemory = glob.getProperty().get("ehrserver/jmx/observeLowMemory", true);
         if (observeLowMemory) {
            try {
                float thresholdFactor = glob.getProperty().get("ehrserver/jmx/memoryThresholdFactor", (float)0.9);
            	new LowMemoryDetector(thresholdFactor);
//               // JDK 1.3 and 1.4: Not available so we need to use reflection
//               Class clazz = java.lang.Class.forName("org.ehrserver.common.admin.extern.LowMemoryDetector");
//               if (clazz != null) {
//                  Class[] paramCls =  new Class[] { float.class }; // url location
//                  java.lang.reflect.Constructor ctor = clazz.getConstructor(paramCls);
//
//                  float thresholdFactor = glob.getProperty().get("ehrserver/jmx/memoryThresholdFactor", (float)0.9);
//                  Object[] params = new Object[] { new Float(thresholdFactor) };
//                  ctor.newInstance(params);
//               }
            }
            catch (Throwable e) {
            	e.printStackTrace();
               log.warn("org.ehrserver.common.admin.extern.LowMemoryDetector is not available for low memory detection");
            }
         }
      }
   }

   /**
    * Start ehrserver security manager and RMI registry.
    * @param registryPort ehrserver/jmx/rmiregistry/port=1099
    * @param registryHost ehrserver/jmx/rmiregistry/hostname=loclhost
    */
   private synchronized void startRmiRegistry(String registryHost, int registryPort) throws ServiceManagerException {

      try {
         if (registryPort > 0) {
            // Start a 'rmiregistry' if desired
            try {
               java.rmi.registry.LocateRegistry.createRegistry(registryPort);
               log.info("Started RMI registry on port " + registryPort);
            } catch (java.rmi.server.ExportException e) {
               // Try to bind to an already running registry:
               try {
                  java.rmi.registry.LocateRegistry.getRegistry(registryHost, registryPort);
                  log.info("Another rmiregistry is running on port " + DEFAULT_REGISTRY_PORT +
                               " we will use this one. You could change the port with e.g. '-ehrserver/jmx/rmiregistry/port 1122' to run your own rmiregistry.");
               }
               catch (RemoteException e2) {
                  String text = "Port " + DEFAULT_REGISTRY_PORT + " is already in use, but does not seem to be a rmiregistry. Please can change the port with e.g. -ehrserver/jmx/rmiregistry/port 1122 : " + e.toString();
                  throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_UNAVAILABLE, ME, text, e2);
               }
            }
         }
      } catch (Exception e) {
         throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_UNAVAILABLE, ME, " could not initialize RMI registry", e);
      }

      log.debug("Initialized RMI registry");
   }

   /**
    * Reorganize the registration for a new parent node.
    * Newer variant
    * @param oldName The existing registry, is used to lookup all matching sub-entries.
    *        For example "org.ehrserver:nodeClass=node,node=clientSUB1"
    * @param oldRootClassname The existing parent node like
    *        "org.ehrserver:nodeClass=node,node=clientjoe1"
    *        Note: Instance names may not contain commas "," for example "joe,Smith" is not valid
    * @return The resulting registration like
    *        "org.ehrserver:nodeClass=node,node=heron,clientClass=connection,connection=jack,queueClass=queue,queue=connection-99"
    */
   public synchronized int renameMBean(String oldName, String oldRootClassname, final ContextNode newRootNode) {
      if (this.mbeanServer == null) return 0;
      if (useJmx == 0) return 0;
      if (oldName == null || newRootNode == null) return 0;
      String newRootName = newRootNode.getAbsoluteName(ContextNode.SCHEMA_JMX);
      int count = 0;
      log.debug("JMX rename registration from '" + oldName + "' to new root '" + newRootName + "'");
      try {
         // e.g. "org.ehrserver:nodeClass=node,node=clientSUB1,*"
         ObjectName query = new ObjectName(oldName+",*");
         QueryExp queryExp = null;
         Set mbeanSet = this.mbeanServer.queryMBeans(query, queryExp);
         if (mbeanSet.size() == 0) {
            log.debug("JMX rename registration from '" + oldName + "' to '" + newRootName + "', nothing found for '" + query + "'");
            return count;
         }
         Iterator it = mbeanSet.iterator();
         while (it.hasNext()) {
            ObjectInstance instance = (ObjectInstance)it.next();
            ObjectName tmp = instance.getObjectName();
            JmxMBeanHandle mbeanHandle = (JmxMBeanHandle)this.mbeanMap.get(tmp.toString());
            if (mbeanHandle == null) {
               log.error("Internal problem: Can't find registration of MBean '" + tmp.toString() + "'");
               continue;
            }
            // /node/heron/connection/joe/session/2
            ContextNode newRoot = ContextNode.valueOf(newRootName);

            // /node/clientjoe1/service/Pop3Driver
            ContextNode current = ContextNode.valueOf(tmp.toString());
            if (current == null) continue;

            // /node/clientjoe1
            ContextNode parent = current.getParent(oldRootClassname);
            if (parent == null) continue;

            // service/Pop3Driver
            ContextNode[] childs = parent.getChildren();
            if (childs.length != 1) continue;

            this.mbeanServer.unregisterMBean(tmp);
            this.mbeanMap.remove(tmp.toString());

            // take this leg and attach it to the newRoot
            ContextNode result = newRoot.mergeChildTree(childs[0]);
             log.debug("Renamed '" + oldName + "' to '" + result.getAbsoluteName(ContextNode.SCHEMA_JMX) + "'");
            if (result != null) {
               registerMBean(result, mbeanHandle.getMBean(), mbeanHandle, true);
               this.mbeanMap.put(mbeanHandle.getObjectInstance().getObjectName().toString(), mbeanHandle);
            }
            else {
               //Thread.dumpStack();
               log.warn("Renamed '" + current.getAbsoluteName(ContextNode.SCHEMA_JMX) + "' to '" + newRoot.getAbsoluteName(ContextNode.SCHEMA_JMX) + "' failed, JMX bean is not registered");
            }
            count++;
         }
         return count;
      }
      catch (Exception e) {
         log.error("JMX rename registration problem from '" + oldName + "' to '" + newRootName + "': " + e.toString());
         e.printStackTrace();
         return count;
      }
   }

   /**
    * Reorganize the registration for a new parent node.
    * Can we set this to deprecated?
    * @param oldName The existing registry, is used to lookup all matching sub-entries.
    *        For example "org.ehrserver:nodeClass=node,node=clientSUB1"
    * @param newNodeClass The new parent node like
    *        "org.ehrserver:nodeClass=node,node=heron"
    *        Note: Instance names may not contain commas "," for example "joe,Smith" is not valid
    * @return The resulting registration like
    *        "org.ehrserver:nodeClass=node,node=heron,clientClass=connection,connection=jack,queueClass=queue,queue=connection-99"
    */
   public synchronized int renameMBean(String oldName, String classNameToChange, String instanceName) {
      if (this.mbeanServer == null) return 0;
      if (useJmx == 0) return 0;
      if (oldName == null || classNameToChange == null) return 0;
      int count = 0;
     log.debug("JMX rename registration from '" + oldName + "' to '" + classNameToChange + "=" + instanceName + "'");
      try {
         // e.g. "org.ehrserver:nodeClass=node,node=clientSUB1,*"
         ObjectName query = new ObjectName(oldName+",*");
         QueryExp queryExp = null;
         Set mbeanSet = this.mbeanServer.queryMBeans(query, queryExp);
         if (mbeanSet.size() == 0) {
            log.debug("JMX rename registration from '" + oldName + "' to '" + classNameToChange + "=" + instanceName + "', nothing found for '" + query + "'");
            return count;
         }
         Iterator it = mbeanSet.iterator();
         while (it.hasNext()) {
            ObjectInstance instance = (ObjectInstance)it.next();
            ObjectName tmp = instance.getObjectName();
            JmxMBeanHandle mbeanHandle = (JmxMBeanHandle)this.mbeanMap.get(tmp.toString());
            if (mbeanHandle == null) {
               log.error("Internal problem: Can't find registration of MBean '" + tmp.toString() + "'");
               continue;
            }
            this.mbeanServer.unregisterMBean(tmp);
            this.mbeanMap.remove(tmp.toString());
            ContextNode renamed = ContextNode.valueOf(tmp.toString());
            renamed.changeParentName(classNameToChange, instanceName);
            log.debug("Renamed '" + oldName + "' to '" + renamed.getAbsoluteName(ContextNode.SCHEMA_JMX) + "'");
            registerMBean(renamed, mbeanHandle.getMBean(), mbeanHandle, true);
            this.mbeanMap.put(mbeanHandle.getObjectInstance().getObjectName().toString(), mbeanHandle);
            count++;
         }
         return count;
      }
      catch (Exception e) {
         log.error("JMX rename registration problem from '" + oldName + "' to '" + classNameToChange + "=" + instanceName + "': " + e.toString());
         e.printStackTrace();
         return count;
      }
   }

   /**
    * Registers the specified mbean into the mbean server.
    * A typical registration string is
    *   <tt>org.ehrserver:nodeClass=node,node="heron",clientClass=client,client="joe",queueClass=queue,queue="subject665",entryClass=entry,entry="1002"</tt>
    * which doesn't conform to the ObjectName recommendation logonservice it does not
    * contains the 'type=...' property.
    * We have chosen this logonservice it creates a nice hierarchy in the jconsole GUI tool.
    * @param contextNode The unique name for JMX observation
    *        Note: Instance names may not contain commas "," for example "joe,Smith" is not valid
    * @param mbean the MBean object instance
    *        If mbean implements MBeanRegistration:preRegister() we don't need the type, name
    * @return The JmxMBeanHandle with object name used to register or null on error
    *         Note: You may not take a clone of it logonservice we may change attributes of this instance
    *         during renaming operations.
    */
   public synchronized JmxMBeanHandle registerMBean(ContextNode contextNode, Object mbean) {
           return registerMBean(contextNode, mbean, null, false);
   }

   public static String getObjectNameLiteral(RunTimeSingleton global, ContextNode contextNode) {
      String hierarchy = (contextNode == null) ?
            ("org.ehrserver:type=" + global.getId()) :
            contextNode.getAbsoluteName(ContextNode.SCHEMA_JMX);
      return hierarchy;
   }

   private synchronized JmxMBeanHandle registerMBean(ContextNode contextNode, Object mbean, JmxMBeanHandle mbeanHandle, boolean isRename) {
      if (this.mbeanServer == null) return null;
      if (useJmx == 0) return null;
      if (contextNode == null) return null; // Remove? code below handles a null
      log.debug("registerMBean(" + contextNode.getAbsoluteName(ContextNode.SCHEMA_JMX) + ")");

      String hierarchy = getObjectNameLiteral(this.glob, contextNode);
      ObjectName objectName = null;
      try {
         objectName = new ObjectName( getObjectNameLiteral(this.glob, contextNode));
         ObjectInstance objectInstance = this.mbeanServer.registerMBean(mbean, objectName);
         if (mbeanHandle == null) {
            mbeanHandle = new JmxMBeanHandle(objectInstance, contextNode, mbean);
            this.mbeanMap.put(objectName.toString(), mbeanHandle);
         }
         else {
             // Update the mbeanHandle of the registrar wit the new ObjectName:
                 mbeanHandle.setObjectInstance(objectInstance);
         }
         log.debug("Registered MBean '" + objectName.toString() +
                                      "' for JMX monitoring and control");
         return mbeanHandle;
      }
      catch (javax.management.InstanceAlreadyExistsException e) {
         if (objectName != null) {
            if (isRename) {
               log.debug("JMX entry exists already, we replace it with new one: " + e.toString());
            }
            else {
               log.warn("JMX entry exists already, we replace it with new one: " + e.toString());
            }
            // this.mbeanMap.remove(objectName.toString()); is done in unregisterMBean
            unregisterMBean(objectName);
            return registerMBean(contextNode, mbean);
         }
         log.warn("Ignoring JMX registration problem for '" + ((objectName==null)?hierarchy:objectName.toString()) + "': " + e.toString());
         return null;
      }
      catch (Exception e) {
         log.error("JMX registration problem for '" + ((objectName==null)?hierarchy:objectName.toString()) + "': " + e.toString());
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Similat to invokeAction but the command does not need to start with
    * "/InvokeAction//", additionally the "set" feature is activated.
    * @param command for example "org.ehrserver:nodeClass=node,node="heron"/action=getFreeMemStr"
    * @return The methods return value
    * @see #invokeAction(String)
    */
   public Object invokeCommand(final String command) {
      String tmp = "/InvokeAction//"+command.trim();
      System.setProperty("jmx.invoke.getters", "set");
      System.out.println("Invoking: " + tmp);
      Object obj = invokeAction(tmp);
      System.getProperties().remove("jmx.invoke.getters");
      return obj;
   }

   /**
    * Invoke ehrserver core JMX bean operations and getter/setter.
    * To invoke getter/seeter you need to start with<br />
    * <code>
    *  java  -Djmx.invoke.getters=set ... org.ehrserver.Main
    * </code>
    * @param args could be somethig like:
    * '/InvokeAction//org.ehrserver:nodeClass=node,node="izar",contribClass=contrib,contrib="Heini"
    *  /action=myNiceMethodName?action=myNiceMethodName&p1+java.lang.String=arg1&p2+java.lang.String=arg1'
    *  or
    *  'org.ehrserver:nodeClass=node,node="heron",topicClass=topic,topic="watchDog"/action=getNumOfHistoryEntries'
    *  or
    *  'org.ehrserver:nodeClass=node,node="heron",topicClass=topic,topic="watchDog"/action=usage'
    *  or
    *  'j org.ehrserver:nodeClass=node,node="heron",topicClass=topic,topic="watchDog"/action=peekHistoryMessages?p1+int=1
    *
    *  or much simpler (will return the usage() string):
    *
    *  org.ehrserver:nodeClass=node,node="heron"/action=usage
    */
   public Object invokeAction(final String args) {
/*
Eamonn, Piyush:
    Maybe I am confused. Whereas what Eamonn says is right, (with
regards to the invoke.getters property etc.) in your original code
didn't you have "state" logonservice the name of the attribute?
    If that is the case, the method to invoke is "setstate" and not
"setState".

    Right?

- Kedar

> Although the JMX spec refers to the property jmx.invoke.getters, this is
> an editing mistake (see
> <http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4949203>).  The
> Reference Implementation used to allow getters and setters to be invoked
> logonservice operations always.  As of version 1.2 (included in JDK 5.0), this is
> no longer allowed.  The jmx.invoke.getters property is intended for
> pre-1.2 code that depended on the old behaviour.  It should not be used
> in new code.
>
> Having said that, the problem with your code is probably that you are
> not specifying the signature array correctly.  Your call to invoke
> should look something like this:
>
> mbsc.invoke(mbeanName,
>             "setState",
>             new Object[] {"new state"},
>             new String[] {"java.lang.String"});
>
> A much better alternative than coding explicit calls to getAttribute and
> invoke is to use MBeanServerInvocationHandler
> <http://java.sun.com/j2se/1.5.0/docs/api/javax/management/MBeanServerInvocationHandler.html>
>
>
> For example:
>
> SimpleStandardMBean proxy = (SimpleStandardMBean)
>     MBeanServerInvocationHandler.newProxyInstance(mbsc,
>                                                   mbeanName,
>
> SimpleStandardMBean.class,
>                                                   false);
>
> String state = proxy.getState();
> proxy.setState("new state");
> proxy.reset();
>
> This will correctly use the getAttribute, setAttribute, and invoke
> methods of MBeanServerConnection, respectively.
>
> Regards,
> �amonn McManus   JMX Spec Lead
*/
       log.debug("invoke with: '" + args);
      if (this.mbeanServer == null) return null;
      if (useJmx == 0) return null;
      if (args == null || args.length() < 1) return null;


      String callString = args;
      if (args.startsWith("/InvokeAction")) {
         callString = args.substring("/InvokeAction//".length());
      }

      // check for actionName (method)
      int j = callString.indexOf("/action=", 1);

      // get action, between '/action=' and '?'
      String action = getAction(callString.substring(j + 1));

//    for (Iterator mbeanIt = mbeanMap.entrySet().iterator() ; mbeanIt.hasNext();) {
//    Object key = ((Entry)mbeanIt.next()).getKey();
//    Object element = mbeanMap.get(key);
//    if (log.isLoggable(Level.dummy)) log.trace(ME, "key: " + key + " element: '" + element);
// }

      // scan for arguments, starting with '&' (i don't know who has introduced the double noting of action=...)
      int k = callString.indexOf("&"); // '/action=myNiceMethodName?action=myNiceMethodName&p1+java.lang.String=arg1&p2+java.lang.String=arg1'
      if (k == -1) k = callString.indexOf("?"); // '...action=myNiceMethodName?p1+java.lang.String=arg1&p2+java.lang.String=arg1'

      ArrayList s = new ArrayList();

      Object[] params = getParams(s, callString.substring(k+1));
      String[] signature = (String[]) s.toArray(new String[s.size()]);

      ObjectName objectName = null;
      Object returnObject = null;

      try {
         log.debug("get object: '" + callString.substring(0, j));
         objectName = new ObjectName(callString.substring(0, j));
         Set set = mbeanServer.queryNames(objectName, null);
         if(set.size() <= 0)
         {
             log.error("Instance Not Found");
             return returnObject;
         }

         log.debug("invoke: '" + action + "@" + objectName);

//         params = new Object[]{"arg1"};
//         signature = new String[]{"java.lang.String"};

         returnObject = mbeanServer.invoke(objectName, action, params,
               signature);
      } catch (javax.management.ReflectionException e) {
         try { // If invoke() fails with access to operations method, we try with getter/setter access
            if (action.startsWith("get")) {
               action = action.substring(3);
               log.debug("invoke: '" + action + "@" + objectName);
               returnObject = mbeanServer.getAttribute(objectName, action);
            }
            else if (action.startsWith("set")) {
               action = action.substring(3);
               Object value = (params.length>0) ? params[0] : "";
               Attribute attribute = new Attribute(action, value);
               log.debug("invoke: '" + attribute.toString() + "@" + objectName);
               mbeanServer.setAttribute(objectName, attribute);
               returnObject = "";
            }
            else {
               log.debug("invoke: '" + action + "@" + objectName);
               returnObject = mbeanServer.getAttribute(objectName, action);
            }
         } catch (Throwable e2) {
            log.warn("args: '" + args + "' invoke: '" + action + "@" + objectName + " failed: " + e2.toString());
         }
      } catch (Throwable e) {
         log.warn("args: '" + args + "' invoke: '" + action + "@" + objectName + " failed: " + e.toString());
         e.printStackTrace();
      }

      return returnObject;


   }


   /**
    * Unregisters the specified mbean from the mbean server.
    * @param objectName The object you got from registerMBean() of type ObjectName,
    *                   if null nothing happens
    */
   public synchronized void unregisterMBean(ObjectName objectName)// throws ServiceManagerException
   {
      if (objectName == null) return;
      if (this.mbeanServer == null) return;
      if (useJmx == 0) return;
      log.debug("Unregister MBean '" + objectName.toString() + "'");
      try {
         Object removed = this.mbeanMap.remove(objectName.toString());
         this.mbeanServer.unregisterMBean(objectName);
         if (removed == null)
            log.error("No JMX MBean instance of " + objectName.toString() + " removed");
      }
      catch (Exception e) {
         log.error("JMX unregistration problems: " , e);
         e.printStackTrace();
      }
   }

   public void unregisterMBean(JmxMBeanHandle jmxMBeanHandle) {
      unregisterMBean(jmxMBeanHandle.getObjectInstance().getObjectName());
   }

   /**
    * Starts ehrserverConnector on mbeanServer-Instance
    * This is a small embedded ehrserver server instance which is started.
    * You can than access this adaptor using the SWING GUI "java org.ehrserver.jmxgui.Main"
    */
   public void startServiceManagerConnector(MBeanServer mbeanServer) throws ServiceManagerException {
      try {
         log.debug("Registering embedded ServiceManagerConnector for JMX...");

         int port = glob.getProperty().get("ehrserver/jmx/SoaBaseAdaptor/port", 3424);
         ObjectName ServiceManagerConnector_name = new ObjectName("Adaptor:transport=ehrserver,port="+port);
         mbeanServer.createMBean("org.ehrserver.common.admin.extern.ServiceManagerConnector", ServiceManagerConnector_name, null);
         log.info("Registered JMX ehrserver adaptor on port " + port + ", try to start swing GUI 'org.ehrserver.jmxgui.Main'");

         // Start the adaptor:
         try {
            mbeanServer.invoke(ServiceManagerConnector_name, "startInternal", new Object[] {this.mbeanServer}, new String[]{MBeanServer.class.getName()});
         }
         catch (Exception ex) {
            throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_CONFIGURATION, ME, " could not register  connector into MBean Server.", ex);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         log.error("Error when registering new Connector >>  " + e.toString() );
         throw new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_UNAVAILABLE, ME, " could not start embedded SoaBaseConnector", e);
      }
   }

   public void shutdown() {
      if (this.mbeanServer == null) return;

      if (this.jmxPropertiesHandle != null) {
         unregisterMBean(this.jmxPropertiesHandle);
         this.jmxPropertiesHandle = null;
      }
      if (this.jmxLogLevelHandle != null) {
         unregisterMBean(this.jmxLogLevelHandle);
         this.jmxLogLevelHandle = null;
      }
      // TODO: Implement complete shutdown
      //javax.management.MBeanServerFactory.releaseMBeanServer(this.mbeanServer);
   }

   private final String getAction(final String s) {
      int i = s.indexOf("action=");
      if (i < 0)
         return null;
      int j = s.indexOf("?", i);
      if (j < 0)
         return s.substring("action=".length());
      else
         return s.substring(i + 7, j);
   }

   private Object[] getParams(ArrayList sigs, String callArgs) {
      log.debug("getParams from: '" + callArgs);
      Map param = StringPairTokenizer.parseToStringStringPairs(callArgs, "&", "+");
      log.debug("params: '" + param);
      ArrayList parms = new ArrayList();
      for (int p = 1; p <= param.size(); p++) {
         String key = "p" + p;
         if (param.containsKey(key)) {
            String argument = (String) param.get(key);
            String value = argument.substring(argument.indexOf('=') + 1, argument.length());
            String signature = argument.substring(0, argument.indexOf('='));
            parms.add(value);
            sigs.add(signature);
         }
      }
      Object[] ret = new Object[parms.size()];
      for (int i=0; i<ret.length; i++) {
         String sig = (String)sigs.get(i);
         String par = (String)parms.get(i);
         if (sig.equals("java.lang.Integer") || sig.equals("int"))
            ret[i] = new Integer(par);
         else if (sig.equals("java.lang.Long") || sig.equals("long"))
            ret[i] = new Long(par);
         else if (sig.equals("java.lang.Short") || sig.equals("short"))
            ret[i] = new Short(par);
         else if (sig.equals("java.lang.Boolean") || sig.equals("boolean"))
            ret[i] = new Boolean(par);
         else
            ret[i] = par;
      }
      return ret;
   }
   
   public boolean isRegistered(ContextNode contextNode) throws ServiceManagerException {
      if (contextNode == null) {
         throw new ServiceManagerException(this.glob, SysErrorCode.USER_ILLEGALARGUMENT, "JmxWrapper.isRegistered", "The context node was null");
      }
      try {
         ObjectName objectName = new ObjectName( getObjectNameLiteral(this.glob, contextNode));
         if (this.mbeanServer == null)
            return false;
         return this.mbeanServer.isRegistered(objectName); 
      }
      catch (MalformedObjectNameException ex) {
         String txt = "The context node '" + contextNode.getAbsoluteName() + "' was malformed";
         throw new ServiceManagerException(this.glob, SysErrorCode.USER_ILLEGALARGUMENT, "JmxWrapper.isRegistered", txt);
      }
   }
}

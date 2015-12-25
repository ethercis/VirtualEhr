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


package com.ethercis.servicemanager.main;

import com.ethercis.servicemanager.cluster.I_SignalListener;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.SignalCatcher;
import com.ethercis.servicemanager.common.FileLocator;
import com.ethercis.servicemanager.common.ReplaceVariable;
import com.ethercis.servicemanager.common.ThreadLister;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.log.SoaLogFormatter;
import com.ethercis.servicemanager.common.property.Property;
import com.ethercis.servicemanager.exceptions.I_ServiceManagerExceptionHandler;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxWrapper;
import com.ethercis.servicemanager.runlevel.I_RunlevelListener;
import com.ethercis.servicemanager.runlevel.RunlevelManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Main class to invoke the ehrserver server.
 * <p>
 * There are many command line parameters supported please invoke with "-?" to
 * get a complete list of the supported parameters. <br>
 * Every parameter may be set in the ehrserver.property file logonservice a system
 * property or at the command line, the command line is strongest,
 * ehrserver.properties weakest. The leading "-" from the command line key
 * parameters are stripped (see Property.java).
 * <p>
 * Examples how to start the ehrserver server:
 * <p>
 * <code>   java org.ehrserver.Main -bootstrapPort 3412</code>
 * <p>
 * <code>   java org.ehrserver.Main -service/ior/iorFile /tmp/EhrServer_Ref</code>
 * <p>
 * <code>   java org.ehrserver.Main -logging FINEST</code>
 * <p>
 * <code>   java org.ehrserver.Main -service/xmlrpc/hostname 102.24.64.60 -service/xmlrpc/port 8081</code>
 * <p>
 * <code>   java org.ehrserver.Main -?</code>
 * 
 */
public class Main implements I_RunlevelListener, I_Main, I_SignalListener, I_ServiceManagerExceptionHandler {
	private String ME = "Main";

	private RunTimeSingleton glob = null;

	private static Logger log = Logger.getLogger(Main.class);
	/**
	 * true: If instance created by control panel<br />
	 * false: running without GUI
	 */
	static Object controlPanel = null;
	/** Starts/stops ehrserver */
	private RunlevelManager runlevelManager = null;

	private boolean showUsage = false;

	/** Incarnation time of this object instance in millis */
	private long startupTime;

	private boolean inShutdownProcess = false;
	private SignalCatcher signalCatcher;
	/**
	 * Comma separate list of errorCodes which to an immediate System.exit(1);
	 * Used by our default implementation of I_ServiceManagerExceptionHandler TODO: If
	 * you use JdbcManagerCommonTableDelegate.java you may NOT use
	 * SysErrorCode.RESOURCE_DB_UNKNOWN logonservice this will retry one time the
	 * operation! How to assure this if configured different???
	 */
	// private String panicErrorCodes =
	// SysErrorCode.RESOURCE_DB_UNAVAILABLE.getErrorCode();
	private String panicErrorCodes = SysErrorCode.RESOURCE_DB_UNAVAILABLE
			.getErrorCode()
			+ ","
			+ SysErrorCode.RESOURCE_DB_UNAVAILABLE.getErrorCode();

	/**
	 * Start ehrserver using the properties from utilGlob without loading
	 * ehrserver.properties again
	 * 
	 * @param utilGlob
	 *            The environment for this server instance
	 */
	public Main(RunTimeSingleton utilGlob) {
		if (utilGlob instanceof RunTimeSingleton)
			init(utilGlob);
		else {
            RunTimeSingleton singleton = RunTimeSingleton.instance();
            singleton = singleton.getClone(Property.propsToArgs(utilGlob.getProperty().getProperties()));
            init(singleton);
        }
	}

	/**
	 * Start ehrserver using the given properties and load ehrserver.properties.
	 * 
	 * @param args
	 *            The command line parameters
	 */
	public Main(String[] args) {
		// The setting 'java -Dehrserver/initClassName=mypackage.MyClass ...'
		// allows to load an initial class instance
		String initClass = System.getProperty("ehrserver/initClassName", "");
		if (initClass.length() > 0) {
			try {
				this.getClass().getClassLoader().loadClass(initClass)
						.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        RunTimeSingleton singleton = RunTimeSingleton.instance();
        singleton = singleton.getClone(args);
		init(singleton);
	}

	public RunTimeSingleton getRunTimeSingleton() {
		return this.glob;
	}

	// /*
	// * Start ehrserver using the properties from utilGlob
	// * without loading <tt>ehrserver.properties</tt> again
	// * @param utilGlob The environment for this server instance
	// */
	// public void initializeSession(ClusterController utilGlob) {
	// ClusterController gg =
	// new ClusterController(utilGlob.getProperty().getProperties(), false);
	// utilGlob.setId(gg.getId()); // Inherit backwards the cluster node id
	// initializeSession(gg);
	// }

	/*
	 * Start ehrserver using the given properties and load
	 * <tt>ehrserver.properties</tt>.
	 * 
	 * @param props The environment for this server instance
	 */
	public void init(Properties props) {
        RunTimeSingleton singleton = RunTimeSingleton.instance();
        singleton = singleton.getClone(Property.propsToArgs(props));
        this.init(singleton);
	}

	public final void init(RunTimeSingleton glob) {
		this.startupTime = System.currentTimeMillis();

		this.glob = glob;

		this.ME = "Main" + glob.getLogPrefixDashed();
		// try { log.info(ME, glob.getDump()); } catch (Throwable e) {
		// System.out.println(ME + ": " + e.toString()); e.printStackTrace(); }

		showUsage = glob.wantsHelp();
		Thread.currentThread().setName("EhrServer.MainThread");

		if (glob.wantsHelp())
			showUsage = true;
		else if (glob.getErrorText() != null) {
			usage();
			log.error(glob.getErrorText());
			System.exit(0);
		}

		long sleepOnStartup = glob.getProperty().get(
				"ehrserver/sleepOnStartup", 0L);
		if (sleepOnStartup > 0L) {
			log.info("Going to sleep logonservice configured ehrserver/sleepOnStartup="
					+ sleepOnStartup);
			try {
				Thread.sleep(sleepOnStartup);
			} catch (InterruptedException e) {
				log.warn("Caught exception during ehrserver/sleepOnStartup="
						+ sleepOnStartup + ": " + e.toString());
			}
		}

		this.panicErrorCodes = glob.getProperty().get(
				"ehrserver/panicErrorCodes", this.panicErrorCodes);
		log.debug("Following errorCodes do an immediate exit: "
				+ this.panicErrorCodes);

		// Add us logonservice an I_ServiceManagerExceptionHandler ... (done again in
		// changeRunlevel() below, but this is too late logonservice first JDBC access can
		// be in RL0
		if (ServiceManagerException.getExceptionHandler() == null)
			ServiceManagerException.setExceptionHandler(this); // see public void
														// newException(ServiceManagerException
														// e);

		int runlevel = glob.getProperty().get("runlevel",
				RunlevelManager.RUNLEVEL_RUNNING);
		try {
			runlevelManager = glob.getRunlevelManager();
			runlevelManager.addRunlevelListener(this);
			runlevelManager.initServiceManagers();
			runlevelManager.changeRunlevel(runlevel, false);
		} catch (Throwable e) {
			if (e instanceof ServiceManagerException) {
				log.error(e.getMessage());
			} else {
				e.printStackTrace();
				log.error(e.toString());
			}

			log.error("Changing runlevel to '"
					+ RunlevelManager.toRunlevelStr(runlevel)
					+ "' failed, good bye");
			System.exit(1);
		}

		boolean useKeyboard = glob.getProperty().get("useKeyboard", true);
		if (!useKeyboard) {
			blockThread();
		}

		// Used by testsuite to switch off blocking, this Main method is by
		// default never returning:
		boolean doBlocking = glob.getProperty().get("doBlocking", true);

		if (doBlocking) {
			checkForKeyboardInput();
		}
	}

	public void blockThread() {
		while (true) {
			try {
				Thread.sleep(100000000L);
			} catch (InterruptedException e) {
				log.warn("Caught exception: " + e.toString());
			}
		}
		/*
		 * // Exception in thread "main" java.lang.IllegalMonitorStateException:
		 * try { Thread.currentThread().wait(); } catch(InterruptedException e)
		 * { log.warn(ME, "Caught exception: " + e.toString()); }
		 */
		// orb.run();
	}

	/** Same logonservice shutdown() but does additionally an engine.global.shutdown() */
	public synchronized void destroy() {
		shutdown();
		if (this.glob != null) {
			this.glob.shutdown();
			this.glob = null;
		}
	}

	/**
	 * Instructs the RunlevelManager to shut down, which causes all object
	 * adapters to shut down.
	 * <p />
	 * The drivers are removed.
	 */
	public synchronized void shutdown() {
		if (inShutdownProcess)
			return;

		inShutdownProcess = true;

		int errors = 0;
		try {
			errors = runlevelManager.changeRunlevel(
					RunlevelManager.RUNLEVEL_HALTED, true);
		} catch (ServiceManagerException e) {
			log.error("Problem during shutdown: " + e.toString());
		}
		if (errors > 0) {
			log.warn("There were " + errors + " errors during shutdown.");
		} else {
			log.debug("shutdown() done");
		}
	}

	// /**
	// * Access the authentication singleton.
	// */
	// public I_Authenticate getAuthenticate() {
	// return glob.getAuthenticate();
	// }

	// /**
	// * Access the ehrserver singleton.
	// */
	// public I_EhrServer getEhrServer() {
	// return getAuthenticate().getEhrServer();
	// }

	/**
	 * Check for keyboard entries from console.
	 * <p />
	 * Supported input is: &lt;ul> &lt;li>'g' to pop up the control panel
	 * GUI&lt;/li> &lt;li>'d' to dump the internal state of ehrserver&lt;/li>
	 * &lt;li>'q' to quit ehrserver&lt;/li> &lt;/ul>
	 * <p />
	 * NOTE: This method never returns, only on exit for 'q'
	 */
	private void checkForKeyboardInput() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			// orbacus needs this !!! Michele?
			// if (orb.work_pending()) orb.perform_work();
			try {
				String line = in.readLine(); // Blocking in I/O
				if (line == null)
					continue;
				line = line.trim();
				if (line.toLowerCase().equals("g")) {
					if (controlPanel == null) {
						log.info("Invoking control panel GUI ...");
						// controlPanel = new MainGUI(glob, this); // the
						// constructor sets the variable controlPanel
						// controlPanel.run();
						// }
						// else
						// controlPanel.showWindow();
					} else if (line.toLowerCase().equals("gc")) {
						long totalMem = Runtime.getRuntime().totalMemory();
						long freeMem = (Runtime.getRuntime().totalMemory() - Runtime
								.getRuntime().freeMemory());
						System.gc();
						log.info("Garbage collector has run, total/free bytes before="
								+ totalMem
								+ "/"
								+ freeMem
								+ ", after="
								+ Runtime.getRuntime().totalMemory()
								+ "/"
								+ (Runtime.getRuntime().totalMemory() - Runtime
										.getRuntime().freeMemory()));
					} else if (line.toLowerCase().startsWith("r")) {
						if (line.length() > 1) {
							String tmp = line.substring(1).trim();
							int runlevel = -10;
							try {
								runlevel = Integer.parseInt(tmp.trim());
							} catch (NumberFormatException e) {
								log.error("Invalid run level '" + tmp
										+ "', it should be a number.");
							}
							;
							try {
								runlevelManager.changeRunlevel(runlevel, true);
							} catch (ServiceManagerException e) {
								log.error(e.toString());
							}
						} else
							log.info("Current runlevel is "
									+ RunlevelManager
											.toRunlevelStr(runlevelManager
													.getCurrentRunlevel())
									+ "="
									+ runlevelManager.getCurrentRunlevel() + "");
					} else if (line.toLowerCase().startsWith("j")) {
						if (line.length() > 1) {
							// ObjectName =
							// org.ehrserver:nodeClass=node,node="heron"
							// j
							// org.ehrserver:nodeClass=node,node="heron"/action=getFreeMemStr
							// j
							// org.ehrserver:nodeClass=node,node="heron"/action=usage?action=usage

							// java -Djmx.invoke.getters=set ...
							// org.ehrserver.Main
							// j
							// org.ehrserver:nodeClass=node,node="heron"/action=getLastWarning?action=getLastWarning
							// j
							// org.ehrserver:nodeClass=node,node="heron"/action=getLastWarning
							// j
							// org.ehrserver:nodeClass=node,node="avalon_mycomp_com",clientClass=client,client="heron.mycomp.com",sessionClass=session,session="1"/action=getConnectionState
							String tmp = line.substring(1).trim();
							try {
								System.out.println("Invoking: " + tmp);
								Object obj = JmxWrapper.getInstance(this.glob)
										.invokeCommand(tmp);
								if (obj instanceof String[]) {
									String[] str = (String[]) obj;
									for (int i = 0; i < str.length; i++)
										System.out.println(str[i]);
								} else {
									System.out.println(obj);
								}
							} catch (ServiceManagerException e) {
								log.error(e.toString());
							}
						} else
							log.info("Please pass a JMX object name to query");
					} else if (line.toLowerCase().startsWith("d")) {
						try {
							String fileName = null;
							if (line.length() > 1)
								fileName = line.substring(1).trim();

							if (fileName == null) {
								System.out.println(glob.getDump());
								log.info("Dump done");
							} else {
								FileLocator.writeFile(fileName, glob.getDump());
								log.info("Dumped internal state to '"
										+ fileName + "'");
							}
						} catch (ServiceManagerException e) {
							log.error("Sorry, dump failed: " + e.getMessage());
						} catch (Throwable e) {
							log.error("Sorry, dump failed: " + e.toString());
						}
					} else if (line.toLowerCase().equals("q")) {
						shutdown();

						System.exit(0);
					} else {// if (keyChar == '?' || Character.isLetter(keyChar)
							// || Character.isDigit(keyChar))
						keyboardUsage();
					}
				}
			} catch (IOException e) {
				log.warn(e.toString()
						+ " Keyboard input is disabled, we block this thread now");
				break;
			}
		}
		blockThread();
	}

	public boolean isHalted() {
		if (runlevelManager != null)
			return runlevelManager.isHalted();
		else
			return true;
	}

	/**
	 * A human readable name of the listener for logging.
	 * <p />
	 * Enforced by I_RunlevelListener
	 */
	public String getName() {
		return ME;
	}

	/**
	 * Invoked on run level change, see RunlevelManager.RUNLEVEL_HALTED and
	 * RunlevelManager.RUNLEVEL_RUNNING
	 * <p />
	 * Enforced by I_RunlevelListener
	 * 
	 * @see I_RunlevelListener
	 */
	public void runlevelChange(int from, int to, boolean force)
			throws ServiceManagerException {
		// if (log.isLoggable(Level.debugR)) log.call(ME,
		// "Changing from run level=" + from + " to level=" + to +
		// " with force=" + force);
		if (to == from)
			return;

		if (to > from) { // startup
			// if (to == RunlevelManager.RUNLEVEL_HALTED) {
			// log.error(ME, "DEBUG ONLY ........");
			// if (glob.getNodeId() == null)
			// glob.setUniqueNodeIdName(createNodeId());
			// }
			if (to == RunlevelManager.RUNLEVEL_HALTED_POST) {
				this.startupTime = System.currentTimeMillis();
				boolean useSignalCatcher = glob.getProperty().get(
						"useSignalCatcher", true);
				if (useSignalCatcher) {
					try {
						this.signalCatcher = SignalCatcher.instance();
						this.signalCatcher.register(this);
						this.signalCatcher.catchSignals();
					} catch (Throwable e) {
						log.warn("Can't register signal catcher: "
								+ e.toString());
					}
				}
				// Add us logonservice an I_ServiceManagerExceptionHandler ...
				if (ServiceManagerException.getExceptionHandler() == null)
					ServiceManagerException.setExceptionHandler(this); // see public
																// void
																// newException(ServiceManagerException
																// e);
			}
			if (to == RunlevelManager.RUNLEVEL_STANDBY) {
			}
			if (to == RunlevelManager.RUNLEVEL_STANDBY_POST) {
				if (showUsage) {
					usage(); // Now we can display the complete usage of all
								// loaded drivers
					shutdown();

					System.exit(0);
				}
			}
			if (to == RunlevelManager.RUNLEVEL_CLEANUP) {
			}
			if (to == RunlevelManager.RUNLEVEL_RUNNING) {
			}
			if (to == RunlevelManager.RUNLEVEL_RUNNING_POST) {
				log.info(RunTimeSingleton.getMemoryStatistic());
				String duration = TimeStamp.millisToNice(System
						.currentTimeMillis() - this.startupTime);
				// TEST
				// new ServiceManagerException(this.glob,
				// SysErrorCode.RESOURCE_DB_UNAVAILABLE, ME + ".getXBStore", "",
				// null);
				if (controlPanel == null) {
					if (SoaLogFormatter.withXtermColors())
						System.out.println(SoaLogFormatter.BLACK_GREEN);
					final String bound = "|";
					String ver = bound + " EhrServer cluster node <"
							+ glob.getId() + "> v" + glob.getReleaseId() + " "
							+ glob.getBuildTimestamp();
					int width = ver.length() + 6;
					if (width < 48)
						width = 48;
					ReplaceVariable sh = new ReplaceVariable();
					String line = sh.charChain('-', width - 2);
					System.out.println("");
					System.out.println(" " + line + " ");
					System.out.println(ver
							+ sh.charChain(' ', width - ver.length() - 1)
							+ bound);
					boolean useKeyboard = glob.getProperty().get("useKeyboard",
							true);
					if (useKeyboard) {
						String help = bound + " READY " + duration
								+ " - press <enter> for options";
						System.out.println(help
								+ sh.charChain(' ', width - help.length() - 1)
								+ bound);
					} else {
						String help = bound + " READY " + duration
								+ " - no keyboard input available";
						System.out.println(help
								+ sh.charChain(' ', width - help.length() - 1)
								+ bound);
					}
					System.out.println(" " + line + " ");
					if (SoaLogFormatter.withXtermColors())
						System.out.println(SoaLogFormatter.ESC);
				} else
					log.info("ehrserver is ready for requests " + duration);
			}
		}
		if (to <= from) { // shutdown
			if (to == RunlevelManager.RUNLEVEL_RUNNING_PRE) {
				log.debug("Shutting down ehrserver to runlevel "
						+ RunlevelManager.toRunlevelStr(to) + " ...");
			}
			if (to == RunlevelManager.RUNLEVEL_HALTED_PRE) {
				synchronized (this) {
					if (this.glob != null) {
						this.glob.shutdown();
					}
				}
				log.info("EhrServer halted.");
			}

			if (to == RunlevelManager.RUNLEVEL_HALTED) {
				synchronized (this) {
					if (this.signalCatcher != null) {
						this.signalCatcher.removeSignalCatcher();
						this.signalCatcher = null;
					}
				}
			}
		}
	}

	public void newException(ServiceManagerException e) {
		boolean serverScope = (e.getRunTimeSingleton() != null && e
				.getRunTimeSingleton().getObjectEntry(
						"org.ehrserver.engine.ClusterController") != null);
		if (!e.isServerSide() && !serverScope) // isServerSide checks if we are
												// ClusterController
												// implementation, serverScope
												// checks if we are a
												// common.ClusterController in the
												// context of a server
			return;
		// Typically if the DB is lost: SysErrorCode.RESOURCE_DB_UNKNOWN
		if (this.panicErrorCodes.indexOf(e.getErrorCodeStr()) != -1) {
			log.error("PANIC: Doing immediate shutdown caused by exception: "
					+ e.getMessage());
			e.printStackTrace();
			log.error(RunTimeSingleton.getStackTraceAsString(e));
			log.error("Complete stack trace (all threads at the time of shutdown: "
					+ ThreadLister.getAllStackTraces());
			SignalCatcher sc = this.signalCatcher;
			if (sc != null) {
				sc.removeSignalCatcher();
			}
			System.exit(1);
		}
	}

	/**
	 * You will be notified when the runtime exits.
	 * 
	 * @see I_SignalListener#shutdownHook()
	 */
	public void shutdownHook() {
		destroy();
	}

	/**
	 * Keyboard input usage.
	 */
	private void keyboardUsage() {
		if (SoaLogFormatter.withXtermColors())
			System.out.println(SoaLogFormatter.BLACK_LTGREEN);
		System.out.println("");
		System.out
				.println("----------------------------------------------------------");
		System.out
				.println("EhrServer "
						+ ((glob != null) ? glob.getVersion() : "")
						+ ((glob != null) ? (" build " + glob
								.getBuildTimestamp()) : ""));
		System.out
				.println("Following interactive keyboard input is recognized:");
		System.out.println("Key:");
		System.out.println("   g             Popup the control panel GUI.");
		System.out.println("   r <run level> Change to run level (0,3,6,9).");
		try {
			if (JmxWrapper.getInstance(this.glob).isActivated())
				System.out
						.println("   j <JMX call>  For example 'j org.ehrserver:nodeClass=node,node=\""
								+ this.glob.getStrippedId()
								+ "\"/action=getFreeMemStr'");
		} catch (ServiceManagerException e) {
			e.printStackTrace();
		}
		System.out
				.println("   d <file name> Dump internal state of ehrserver to file.");
		System.out.println("   q             Quit ehrserver.");
		System.out
				.println("----------------------------------------------------------");
		if (SoaLogFormatter.withXtermColors())
			System.out.println(SoaLogFormatter.ESC);
	}

	/**
	 * Command line usage.
	 */
	private void usage() {
		System.out.println("-----------------------" + glob.getVersion()
				+ "-------------------------------");
		System.out.println("java org.ehrserver.Main <options>");
		System.out
				.println("----------------------------------------------------------");
		System.out.println("   -h                  Show the complete usage.");
		System.out.println("");
		// try { System.out.println(glob.getProtocolManager().usage()); } catch
		// (ServiceManagerException e) { log.warn(ME, "No usage: " + e.toString()); }
		// Depending on the current run level not all drivers may be visible:

		System.out.println("");
		System.out.println(glob.usage());
		System.out.println("");
		System.out.println("Other stuff:");
		System.out
				.println("   -ehrserver/acceptWrongSenderAddress/<subjectId>  <subjectId> is for example 'joe' [false]");
		System.out
				.println("                              true: Allows user 'joe' to send wrong sender address in PublishQos");
		System.out
				.println("   -ehrserver/sleepOnStartup Number of milli seconds to sleep before startup [0]");
		System.out
				.println("   -useKeyboard false         Switch off keyboard input, to allow ehrserver running in background [true]");
		System.out
				.println("   -doBlocking  false         Switch off blocking, the main method is by default never returning [true]");
		System.out
				.println("   -admin.remoteconsole.port  If port > 1000 a server is started which is available with telnet [2702]");
		System.out
				.println("   -ehrserver.isEmbedded     If set to true no System.exit() is possible [false]");
		System.out
				.println("   -ehrserver/jmx/HtmlAdaptor       Set to true to enable JMX HTTP access on 'http://localhost:8082' [false]");
		System.out
				.println("   -ehrserver/jmx/EhrServerAdaptor Set to true to enable JMX ehrserver adaptor access for swing GUI 'org.ehrserver.jmxgui.Main' [false].");
		System.out
				.println("   java -Dcom.sun.management.jmxremote ...  Switch on JMX support with jconsole (JDK >= 1.5).");
		System.out
				.println("   -ehrserver/jmx/observeLowMemory      Write a log error when 90% of the JVM memory is used (JDK >= 1.5) [true]");
		System.out
				.println("   -ehrserver/jmx/memoryThresholdFactor Configure the log error memory threshhold (defaults to 90%) (JDK >= 1.5) [0.9]");
		System.out
				.println("   -ehrserver/jmx/exitOnMemoryThreshold If true ehrserver stops if the memoryThresholdFactor is reached (JDK >= 1.5) [false]");
		System.out
				.println("----------------------------------------------------------");
		System.out.println("Example:");
		System.out.println("   java org.ehrserver.Main -cluster false");
		System.out.println("   java org.ehrserver.Main -cluster.node.id heron");
		System.out
				.println("   java org.ehrserver.Main -propertyFile somewhere/ehrserver.properties -servicesFile somewhere/services.xml");
		System.out.println("   java org.ehrserver.Main -bootstrapPort 3412");
		System.out
				.println("   java org.ehrserver.Main -service/ior/iorFile /tmp/EhrServer_Ref.ior");
		System.out
				.println("   java org.ehrserver.Main -logging/org.ehrserver.engine FINE");
		System.out
				.println("   java org.ehrserver.Main -logging/org.ehrserver.common.protocol.RequestReplyExecutor FINEST   (dumps SOCKET messages)");
		System.out
				.println("   java org.ehrserver.Main -service/xmlrpc/hostname 102.24.64.60 -service/xmlrpc/port 8081");
		System.out
				.println("   java -Dcom.sun.management.jmxremote org.ehrserver.Main");
		System.out
				.println("   java org.ehrserver.Main");
		System.out.println("   java org.ehrserver.Main -?");
		System.out.println("See ehrserver.properties for more options");
		System.out.println("");
	}

	/**
	 * Invoke: java org.ehrserver.Main
	 */
	public static void main(String[] args) {
		new Main(args);
	}
}

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
/**
 * This is the main class to load the services logonservice defined by the configuration file.
 */
import com.ethercis.servicemanager.common.ThreadLister;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.I_ServiceManagerExceptionHandler;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_RunlevelListener;
import com.ethercis.servicemanager.runlevel.RunlevelManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ClusterLoader implements I_SignalListener, I_RunlevelListener, I_ServiceManagerExceptionHandler {
	private String ME = "ClusterLoader";

	private RunTimeSingleton glob = null;

	private static Logger log = LogManager.getLogger(ClusterLoader.class);

	/** Starts/stops ehrserver */
	private RunlevelManager runlevelManager = null;

	private boolean showUsage = false;

	/** Incarnation time of this object instance in millis */
	private long startupTime;

	private boolean inShutdownProcess = false;
	private SignalCatcher signalCatcher;
	private String panicErrorCodes = SysErrorCode.RESOURCE_UNAVAILABLE.getErrorCode()+","+SysErrorCode.RESOURCE_DB_UNAVAILABLE.getErrorCode();

	/**
	 * Start ehrserver using the properties from utilGlob
	 * without loading services.properties again
	 * @param utilGlob The environment for this server instance
	 */
	public ClusterLoader(RunTimeSingleton controller) {
		init(controller);
	}

	/**
	 * for compatibility purpose
	 * @param args command line arguments
	 */
	public ClusterLoader(String[] args) {
		RunTimeSingleton controller = new RunTimeSingleton(args);
		init(controller);
	}

	
	public final void init(RunTimeSingleton glob)
	{
		this.startupTime = System.currentTimeMillis();

		this.glob = glob;

		this.ME = "ClusterController" + glob.getLogPrefixDashed();
		//try { log.info(ME, glob.getDump()); } catch (Throwable e) { System.out.println(ME + ": " + e.toString()); e.printStackTrace(); }

		showUsage = glob.wantsHelp();
		Thread.currentThread().setName("ClusterController.MainThread");

		if (glob.wantsHelp())
			showUsage = true;
		else if (glob.getErrorText() != null) {
			usage();
			log.error(glob.getErrorText());
		}

		long sleepOnStartup = glob.getProperty().get("ehrserver/sleepOnStartup", 0L);
		if (sleepOnStartup > 0L) {
			log.info("Going to sleep logonservice configured ehrserver/sleepOnStartup=" + sleepOnStartup);
			try { Thread.sleep(sleepOnStartup);
			} catch(InterruptedException e) { log.warn("Caught exception during ehrserver/sleepOnStartup=" + sleepOnStartup + ": " + e.toString()); }
		}

		this.panicErrorCodes = glob.getProperty().get("ehrserver/panicErrorCodes", this.panicErrorCodes);
		log.debug("Following errorCodes do an immediate exit: " + this.panicErrorCodes);

		if (ServiceManagerException.getExceptionHandler() == null)
			ServiceManagerException.setExceptionHandler(this); // see public void newException(ServiceManagerException e);

		int runlevel = glob.getProperty().get("runlevel", RunlevelManager.RUNLEVEL_RUNNING);

		try {
			runlevelManager = glob.getRunlevelManager();
			runlevelManager.addRunlevelListener(this);
			runlevelManager.initServiceManagers();
			runlevelManager.changeRunlevel(runlevel, false);
		} catch (Throwable e) {
			if (e instanceof ServiceManagerException) {
				log.error(e.getMessage());
			}
			else {
				e.printStackTrace();
				log.error(e.toString());
			}
			System.exit(0);
		}
	}

	private void usage() {
		// TODO Auto-generated method stub

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
	 * Instructs the RunlevelManager to shut down, which causes all object adapters to shut down.
	 * <p />
	 * The drivers are removed.
	 */
	public synchronized void shutdown()
	{
		if (inShutdownProcess)
			return;

		inShutdownProcess = true;

		int errors = 0;
		try {
			errors = runlevelManager.changeRunlevel(RunlevelManager.RUNLEVEL_HALTED, true);
		}
		catch(ServiceManagerException e) {
			log.error("Problem during shutdown: " + e.toString());
		}
		if (errors > 0) {
			log.warn("There were " + errors + " errors during shutdown.");
		}
		else {
			log.debug("shutdown() done");
		}
	}

	public boolean isHalted() {
		if( runlevelManager != null )
			return runlevelManager.isHalted();
		else return true;
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
	 * Invoked on run level change, see RunlevelManager.RUNLEVEL_HALTED and RunlevelManager.RUNLEVEL_RUNNING
	 * <p />
	 * Enforced by I_RunlevelListener
	 */
	public void runlevelChange(int from, int to, boolean force) throws ServiceManagerException {
		//if (log.isLoggable(Level.debugR)) log.call(ME, "Changing from run level=" + from + " to level=" + to + " with force=" + force);
		if (to == from)
			return;

		if (to > from) { // startup
			//if (to == RunlevelManager.RUNLEVEL_HALTED) {
			//   log.error(ME, "DEBUG ONLY ........");
			//   if (glob.getNodeId() == null)
			//      glob.setUniqueNodeIdName(createNodeId());
			//}
			if (to == RunlevelManager.RUNLEVEL_HALTED_POST) {
				this.startupTime = System.currentTimeMillis();
				boolean useSignalCatcher = glob.getProperty().get("useSignalCatcher", true);
				if (useSignalCatcher) {
					try {
						this.signalCatcher = SignalCatcher.instance();
						this.signalCatcher.register(this);
						this.signalCatcher.catchSignals();
					}
					catch (Throwable e) {
						log.warn("Can't register signal catcher: " + e.toString());
					}
				}
				// Add us logonservice an I_ServiceManagerExceptionHandler ...
				if (ServiceManagerException.getExceptionHandler() == null)
					ServiceManagerException.setExceptionHandler(this); // see public void newException(ServiceManagerException e);
			}
			if (to == RunlevelManager.RUNLEVEL_STANDBY) {
			}
			if (to == RunlevelManager.RUNLEVEL_STANDBY_POST) {
				if (showUsage) {
					usage();  // Now we can display the complete usage of all loaded drivers
					shutdown();
				}
			}
			if (to == RunlevelManager.RUNLEVEL_CLEANUP) {
			}
			if (to == RunlevelManager.RUNLEVEL_RUNNING) {
			}
			if (to == RunlevelManager.RUNLEVEL_RUNNING_POST) {
				log.info(RunTimeSingleton.getMemoryStatistic());
				String duration = TimeStamp.millisToNice(System.currentTimeMillis() - this.startupTime);
				// TEST
				log.info("ehrserver is ready for requests " + duration);
			}
		}
		if (to <= from) { // shutdown
			if (to == RunlevelManager.RUNLEVEL_RUNNING_PRE) {
				 log.debug("Shutting down ehrserver to runlevel " + RunlevelManager.toRunlevelStr(to) + " ...");
			}
			if (to == RunlevelManager.RUNLEVEL_HALTED_PRE) {
				synchronized (this) {
					if (this.glob != null) {
						this.glob.shutdown();
					}
				}
				log.info("ehrserver halted.");
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
		boolean serverScope = (e.getRunTimeSingleton() != null && e.getRunTimeSingleton().getObjectEntry("com.ethercis.servicemanager.cluster.ClusterController") != null);
		if (!e.isServerSide() && !serverScope) // isServerSide checks if we are ServerScope implementation, serverScope checks if we are a common.Global in the context of a server
			return;
		// Typically if the DB is lost: ErrorCode.RESOURCE_DB_UNKNOWN
		if (this.panicErrorCodes.indexOf(e.getErrorCodeStr()) != -1) {
			log.error("PANIC: Doing immediate shutdown caused by exception: " + e.getMessage());
			e.printStackTrace();
			log.error(RunTimeSingleton.getStackTraceAsString(e));
			log.error("Complete stack trace (all threads at the time of shutdown: " + ThreadLister.getAllStackTraces());
			SignalCatcher sc = this.signalCatcher;
			if (sc != null) {
				sc.removeSignalCatcher();
			}
			System.exit(1);
		}
	}

	/**
	 * You will be notified when the runtime exits.
	 * @see I_SignalListener#shutdownHook()
	 */
	public void shutdownHook() {
		destroy();
	}

}

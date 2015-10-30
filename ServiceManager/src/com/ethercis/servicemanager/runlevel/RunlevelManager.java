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

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.runlevel;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.common.TimeStamp;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.JmxMBeanHandle;
import com.ethercis.servicemanager.service.I_Service;
import com.ethercis.servicemanager.service.ServiceHolder;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * This starts/stops ehrserver with different run levels.
 * <p>
 */
public final class RunlevelManager implements RunlevelManagerMBean {
	private String ME = "RunlevelManager";
	private final RunTimeSingleton glob;
	private static Logger log = Logger.getLogger(RunlevelManager.class
			.getName());
	private int currRunlevel = 0;

	public static final int RUNLEVEL_HALTED_PRE = -1;
	public static final int RUNLEVEL_HALTED = 0;
	public static final int RUNLEVEL_HALTED_POST = 1;

	public static final int RUNLEVEL_STANDBY_PRE = 2;
	public static final int RUNLEVEL_STANDBY = 3;
	public static final int RUNLEVEL_STANDBY_POST = 4;

	public static final int RUNLEVEL_CLEANUP_PRE = 5;
	public static final int RUNLEVEL_CLEANUP = 6;
	public static final int RUNLEVEL_CLEANUP_POST = 7;

	public static final int RUNLEVEL_RUNNING_PRE = 8;
	public static final int RUNLEVEL_RUNNING = 9;
	public static final int RUNLEVEL_RUNNING_POST = 10;

	private final I_RunlevelListener[] DUMMY_ARR = new I_RunlevelListener[0];

	/** My JMX registration */
	private JmxMBeanHandle mbeanHandle;
	private ContextNode contextNode;

	private boolean allowDynamicServices;

	/**
	 * For listeners who want to be informed about runlevel changes.
	 */
	private final Set<I_RunlevelListener> runlevelListenerSet = Collections
			.synchronizedSet(new HashSet<I_RunlevelListener>());

	/**
	 * One instance of this represents one ehrserver server.
	 * <p />
	 * You need to call initServiceManagers() after creation.
	 */
	public RunlevelManager(RunTimeSingleton glob) {
		this.glob = glob;

		this.ME = "RunlevelManager" + this.glob.getLogPrefixDashed();
		log.debug("Incarnated run level manager");
		// For JMX instanceName may not contain ","
		this.contextNode = new ContextNode(ContextNode.SERVICE_MARKER_TAG,
				"RunlevelManager", this.glob.getScopeContextNode());

		this.allowDynamicServices = this.glob.getProperty().get(
				"ehrserver/allowDynamicServices", false);
		if (allowDynamicServices) {
			String text = "ehrserver/allowDynamicServices=true: Please protect this feature logonservice any code can be injected";
			log.warn(text);
		}
	}

	public void initJmx() {
		try {
			this.mbeanHandle = this.glob.registerMBean(this.contextNode, this);
		} catch (ServiceManagerException e) {
			log.error(e.getMessage());
		}
	}

	public ContextNode getContextNode() {
		return this.contextNode;
	}

	/**
	 * Sets the cluster node ID logonservice soon logonservice it is known.
	 */
	public void setId(String id) {
		this.ME = "RunlevelManager" + this.glob.getLogPrefixDashed();
	}

	/**
	 * Incarnate the different managers which handle run levels.
	 */
	public void initServiceManagers() throws ServiceManagerException {
		// TODO: This should be configurable
		// new Authenticate(glob);
		// glob.getProtocolManager(); // force incarnation
		log.debug("Initialized run level manager");
	}

	/**
	 * Adds the specified runlevel listener to receive runlevel change events.
	 * Multiple registrations for the same listener will overwrite the old one.
	 */
	public void addRunlevelListener(I_RunlevelListener l) {
		if (l == null) {
			return;
		}
		synchronized (runlevelListenerSet) {
			runlevelListenerSet.add(l);
		}
	}

	/**
	 * Removes the specified listener.
	 */
	public void removeRunlevelListener(I_RunlevelListener l) {
		if (l == null) {
			return;
		}
		synchronized (runlevelListenerSet) {
			runlevelListenerSet.remove(l);
		}
	}

	/**
	 * Allows to pass the newRunlevel logonservice a String like "RUNLEVEL_STANDBY" or "6"
	 * 
	 * @see #changeRunlevel(int, boolean)
	 */
	public final int changeRunlevel(String newRunlevel, boolean force)
			throws ServiceManagerException {
		if (newRunlevel == null || newRunlevel.length() < 1) {
			String text = "Runlevel " + newRunlevel
					+ " is not allowed, please choose one of "
					+ RUNLEVEL_HALTED + "|" + RUNLEVEL_STANDBY + "|"
					+ RUNLEVEL_CLEANUP + "|" + RUNLEVEL_RUNNING;
			log.debug(text);
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME, text);
		}

		int level = 0;
		try {
			level = Integer.parseInt(newRunlevel.trim());
			return glob.getRunlevelManager().changeRunlevel(level, true);
		} catch (NumberFormatException e) {
			level = toRunlevelInt(newRunlevel);
			return glob.getRunlevelManager().changeRunlevel(level, true);
		}
	}

	/**
	 * JMX: Change the run level of ehrserver.
	 * 
	 * @param 0 is halted and 9 is fully operational
	 */
	public String setRunlevel(String level) throws Exception {
		try {
			int numErrors = changeRunlevel(level, true);
			return "Changed to run level "
					+ toRunlevelStr(getCurrentRunlevel())
					+ " '"
					+ level
					+ "'"
					+ ((numErrors > 0) ? (" with " + numErrors + " errors")
							: "");
		} catch (ServiceManagerException e) {
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Change the run level to the given newRunlevel.
	 * <p />
	 * See RUNLEVEL_HALTED etc.
	 * <p />
	 * Note that there are four main run levels:
	 * <ul>
	 * <li>RUNLEVEL_HALTED</li>
	 * <li>RUNLEVEL_STANDBY</li>
	 * <li>RUNLEVEL_CLEANUP</li>
	 * <li>RUNLEVEL_RUNNING</li>
	 * </ul>
	 * and every RUNLEVEL sends a pre and a post run level event, to allow the
	 * listeners to prepare or log before or after successfully changing levels.
	 * <br />
	 * NOTE that the pre/post events are <b>no</b> run level states - they are
	 * just events.
	 * 
	 * @param newRunlevel
	 *            The new run level we want to switch to
	 * @param force
	 *            Ignore exceptions during change, currently only force == true
	 *            is supported
	 * @return numErrors
	 * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException
	 *                for invalid run level
	 */
	public final int changeRunlevel(int newRunlevel, boolean force)
			throws ServiceManagerException {
		log.debug("Changing from run level " + currRunlevel + " to run level "
				+ newRunlevel + " with force=" + force);
		long start = System.currentTimeMillis();
		int numErrors = 0;
		if (currRunlevel == newRunlevel) {
			return numErrors;
		}
		int from = currRunlevel;
		int to = newRunlevel;

		log.info("Change request from run level " + toRunlevelStr(from)
				+ " to run level " + toRunlevelStr(to) + " ...");

		if (!isMajorLevel(to)) {
			String text = "Runlevel " + to
					+ " is not allowed, please choose one of "
					+ RUNLEVEL_HALTED + "|" + RUNLEVEL_STANDBY + "|"
					+ RUNLEVEL_CLEANUP + "|" + RUNLEVEL_RUNNING;
			log.debug(text);
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME, text);
		}

		if (from < to) { // startup
			for (int ii = from; ii < to; ii++) {
				int dest = ii + 1;
				try {
					startupServices(ii, dest);
					fireRunlevelEvent(ii, dest, force);
				} finally {
					currRunlevel = dest; // pre/post events are not marked logonservice
											// run levels
					if (dest > from && isMajorLevel(dest)) {
						long elapsed = System.currentTimeMillis() - start;
						if (numErrors == 0)
							log.debug("Successful startup to run level "
									+ toRunlevelStr(dest)
									+ TimeStamp.millisToNice(elapsed));
						else
							log.info("Startup to run level "
									+ toRunlevelStr(dest) + " done with "
									+ numErrors + " errors.");
					}
				}
			}
			if (to == RUNLEVEL_RUNNING) { // Main.java to display banner
				fireRunlevelEvent(RUNLEVEL_RUNNING, RUNLEVEL_RUNNING_POST,
						force);
			}
		} else if (from > to) { // shutdown
			for (int ii = from; ii > to; ii--) {
				int dest = ii - 1;
				try {
					shutdownServices(ii, dest);
					fireRunlevelEvent(ii, dest, force);
				} finally {
					currRunlevel = dest;
					if (dest < from && isMajorLevel(dest)) {
						long elapsed = System.currentTimeMillis() - start;
						if (numErrors == 0)
							log.debug("Successful shutdown to run level="
									+ toRunlevelStr(dest)
									+ TimeStamp.millisToNice(elapsed));
						else
							log.info("Shutdown to run level="
									+ toRunlevelStr(dest) + " done with "
									+ numErrors + " errors.");
					}
				}
			}
		}

		log.debug("Leaving changeRunlevel with runlevel = "
				+ toRunlevelStr(currRunlevel));
		return numErrors;
	}

	/**
    *
    */
	private void startupServices(int from, int to) throws ServiceManagerException {
		TreeSet<?> pluginSet = this.glob.getServiceHolder().getStartupSequence(
				this.glob.getStrippedId(), from + 1, to);
		log.debug("startupServices. the size of the plugin set is '"
				+ pluginSet.size() + "'");
		Iterator<?> iter = pluginSet.iterator();
		while (iter.hasNext()) {
			ServiceConfig serviceConfig = (ServiceConfig) iter.next();
			if (serviceConfig == null) {
				log.warn("startupServices. the serviceConfig object is null");
				continue;
			}
			if (!serviceConfig.isCreate()) {
				log.debug("startupServices. the plugin + "
						+ serviceConfig.getId() + " is ignored, create='false'");
				continue;
			}
			log.debug("startupServices " + serviceConfig.toXml());
			try {
				long startTime = System.currentTimeMillis();
				ServiceInfo pluginInfo = serviceConfig.getServiceInfo();

				if (pluginInfo != null) {
					log.debug("startupServices pluginInfo object: "
							+ pluginInfo.getId() + " classname: "
							+ pluginInfo.getClassName());
				} else
					log.debug("startupServices: the pluginInfo is null");

				this.glob.getServiceManager().getServiceObject(pluginInfo);
				long deltaTime = System.currentTimeMillis() - startTime;
				log.debug("Run level '" + from + "' to '" + to + "' plugin '"
                        + serviceConfig.getId() + "' successful loaded in '"
                        + deltaTime + "' ms");
			} catch (Throwable ex) {
				SysErrorCode code = serviceConfig.getUpAction().getOnFail();
				if (code == null) {
					log.warn("Exception when loading the plugin '"
							+ serviceConfig.getId() + "' reason: "
							+ ex.toString());
					Throwable cause = ex.getCause();
					if (ex instanceof ServiceManagerException)
						cause = ((ServiceManagerException) ex).getEmbeddedException();
					if (cause != null)
						cause.printStackTrace();
					else
						ex.printStackTrace();
				} else {
					throw new ServiceManagerException(this.glob, code, ME
							+ ".startupServices", "Can't load plugin '"
							+ serviceConfig.getId() + "'", ex);
				}
			}
		}
	}

	/**
	 * Called by JMX, throws IllegalArgumentExcetion instead of
	 * ServiceManagerException.
	 * 
	 * @param pluginConfig
	 * @param create
	 */
	void toggleCreate(ServiceConfig pluginConfig, boolean create) {
		log.info("Changing plugin '" + pluginConfig.getId() + "' create="
				+ pluginConfig.isCreate() + " to " + create);
		if (pluginConfig.isCreate() != create) {
			if (create) {
				try {
					this.glob.getServiceManager().getServiceObject(
							pluginConfig.getServiceInfo());
				} catch (ServiceManagerException e) {
					log.warn("Failed to create plugin: " + e.toString());
					throw new IllegalArgumentException(
							"Failed to create plugin: " + e.toString());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} else {
				try {
					I_Service plugin = this.glob.getServiceManager()
							.getServiceObject(pluginConfig.getServiceInfo());
					plugin.shutdown();
					this.glob.getServiceManager().removeFromServiceCache(
							pluginConfig.getServiceInfo().getId());
				} catch (ServiceManagerException e) {
					log.warn("Failed to remove plugin: " + e.toString());
					throw new IllegalArgumentException(
							"Failed to create plugin: " + e.toString());
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Add a new service, if it exists remove the old first.
     * Intended to be used to load new service version at runtime.
	 * 
	 * @param serviceConfig
	 */
	private void addService(ServiceConfig serviceConfig) throws ServiceManagerException {
		log.info("New runlevel plugin configuration arrived: "
                + serviceConfig.getServiceInfo().getId());
		I_Service oldService = this.glob.getServiceManager()
				.removeFromServiceCache(serviceConfig.getServiceInfo().getId());

		ServiceHolder holder = this.glob.getServiceHolder();
		ServiceConfig oldConfig = holder.removeServiceConfig(null,
				serviceConfig.getId());
		if (oldConfig != null)
			log.info("Removed old plugin " + oldConfig.getId());

		if (oldService != null && oldConfig == null)
			log.error("Unexpected plugin cache entry:" + oldService.getType());

		holder.addDefaultServiceConfig(serviceConfig);

		serviceConfig.registerMBean();
		if (serviceConfig.isCreate())
			this.glob.getServiceManager().getServiceObject(
                    serviceConfig.getServiceInfo());
	}

	/**
    *
    */
	private void shutdownServices(int from, int to) throws ServiceManagerException {
		TreeSet<?> serviceSet = this.glob.getServiceHolder().getShutdownSequence(this.glob.getStrippedId(), to, from - 1);
		Iterator<?> iter = serviceSet.iterator();
		while (iter.hasNext()) {
			ServiceConfig serviceConfig = (ServiceConfig) iter.next();
			if (serviceConfig == null || !serviceConfig.isCreate())
				continue;

			try {
				ServiceInfo serviceInfo = serviceConfig.getServiceInfo();
				I_Service service = this.glob.getServiceManager().getServiceObject(serviceInfo);
				service.shutdown();
				this.glob.getServiceManager().removeFromServiceCache(serviceInfo.getId());
				log.debug("fireRunlevelEvent: run level '" + from + "' to '"
						+ to + "' service '" + serviceConfig.getId()
						+ "' shutdown");
			} catch (Throwable ex) {
				SysErrorCode code = serviceConfig.getDownAction().getOnFail();
				if (code == null) {
					log.warn(".fireRunlevelEvent. Exception when shutting down the service '"
							+ serviceConfig.getId()
							+ "' reason: "
							+ ex.toString());
				} else {
					throw new ServiceManagerException(this.glob, code, ME
							+ ".fireRunlevelEvent",
							".fireRunlevelEvent. Exception when shutting down the service '"
									+ serviceConfig.getId() + "'", ex);
				}
			}
		}
	}

	/**
	 * The static plugins are loaded from (exclusive) to (inclusive) when
	 * startup and the same when shutting down. For example if you define LOAD
	 * on r 3, and STOP on r 2, then LOAD is fired when from=2,to=3 and STOP
	 * when from=3,to=2
	 */
	private final int fireRunlevelEvent(int from, int to, boolean force)
			throws ServiceManagerException {
		int numErrors = 0;

		// Take a snapshot of current listeners (to avoid
		// ConcurrentModificationException in iterator)
		I_RunlevelListener[] listeners;
		synchronized (runlevelListenerSet) {
			if (runlevelListenerSet.size() == 0)
				return numErrors;
			listeners = (I_RunlevelListener[]) runlevelListenerSet
					.toArray(DUMMY_ARR);
		}

		for (int ii = 0; ii < listeners.length; ii++) {
			I_RunlevelListener li = listeners[ii];
			try {
				li.runlevelChange(from, to, force);

				if (isMajorLevel(to)) {
					if (from < to)
						log.debug(li.getName()
								+ " successful startup to run level=" + to
								+ ", errors=" + numErrors + ".");
					else
						log.debug(li.getName()
								+ " successful shutdown to run level=" + to
								+ ", errors=" + numErrors + ".");
				}

			} catch (ServiceManagerException e) {
				if (e.isInternal()) {
					log.error("Changing from run level=" + from + " to level="
							+ to + " failed for component " + li.getName()
							+ ": " + e.getMessage());
				} else {
					log.warn("Changing from run level=" + from + " to level="
							+ to + " failed for component " + li.getName()
							+ ": " + e.getMessage());
				}
				numErrors++;
			}
		}
		return numErrors;
	}

	/**
	 * See java for runlevels
	 */
	public final int getCurrentRunlevel() {
		return currRunlevel;
	}

	public boolean isHalted() {
		return currRunlevel <= RUNLEVEL_HALTED;
	}

	public boolean isStandby() {
		return currRunlevel == RUNLEVEL_STANDBY;
	}

	public boolean isCleanup() {
		return currRunlevel == RUNLEVEL_CLEANUP;
	}

	public boolean isRunning() {
		return currRunlevel == RUNLEVEL_RUNNING;
	}

	/**
	 * @return true if one of the major run levels. false if pre or post event
	 *         level
	 */
	public boolean isMajorLevel() {
		return isMajorLevel(currRunlevel);
	}

	// ======== static methods ============

	private static final boolean isMajorLevel(int level) {
		if (level == RUNLEVEL_HALTED || level == RUNLEVEL_STANDBY
				|| level == RUNLEVEL_CLEANUP || level == RUNLEVEL_RUNNING)
			return true;
		return false;
	}

	/**
	 * @return true if one of the major levels
	 */
	public static final boolean checkRunlevel(int level) {
		return isMajorLevel(level);
	}

	/**
	 * Parses given string to extract the priority of a message
	 * 
	 * @param level
	 *            For example 7
	 * @return "RUNLEVEL_UNKNOWN" if no valid run level, else for example
	 *         "STANDBY_POST"
	 */
	public final static String toRunlevelStr(int level) {
		if (level == RUNLEVEL_HALTED_PRE)
			return "HALTED_PRE";
		else if (level == RUNLEVEL_HALTED)
			return "HALTED";
		else if (level == RUNLEVEL_HALTED_POST)
			return "HALTED_POST";
		else if (level == RUNLEVEL_STANDBY_PRE)
			return "STANDBY_PRE";
		else if (level == RUNLEVEL_STANDBY)
			return "STANDBY";
		else if (level == RUNLEVEL_STANDBY_POST)
			return "STANDBY_POST";
		else if (level == RUNLEVEL_CLEANUP_PRE)
			return "CLEANUP_PRE";
		else if (level == RUNLEVEL_CLEANUP)
			return "CLEANUP";
		else if (level == RUNLEVEL_CLEANUP_POST)
			return "CLEANUP_POST";
		else if (level == RUNLEVEL_RUNNING_PRE)
			return "RUNNING_PRE";
		else if (level == RUNLEVEL_RUNNING)
			return "RUNNING";
		else if (level == RUNLEVEL_RUNNING_POST)
			return "RUNNING_POST";
		else
			return "RUNLEVEL_UNKNOWN(" + level + ")";
	}

	/**
	 * Parses given string to extract the priority of a message
	 * 
	 * @param level
	 *            For example "STANDBY" or 7
	 * @param defaultPriority
	 *            Value to use if not parseable
	 * @return -10 if no valid run level
	 */
	public final static int toRunlevelInt(String level) {
		if (level == null)
			return -10;
		level = level.trim();
		try {
			return Integer.parseInt(level);
		} catch (NumberFormatException e) {
		}

		if (level.equalsIgnoreCase("HALTED_PRE"))
			return RUNLEVEL_HALTED_PRE;
		else if (level.equalsIgnoreCase("HALTED"))
			return RUNLEVEL_HALTED;
		else if (level.equalsIgnoreCase("HALTED_POST"))
			return RUNLEVEL_HALTED_POST;
		else if (level.equalsIgnoreCase("STANDBY_PRE"))
			return RUNLEVEL_STANDBY_PRE;
		else if (level.equalsIgnoreCase("STANDBY"))
			return RUNLEVEL_STANDBY;
		else if (level.equalsIgnoreCase("STANDBY_POST"))
			return RUNLEVEL_STANDBY_POST;
		else if (level.equalsIgnoreCase("CLEANUP_PRE"))
			return RUNLEVEL_CLEANUP_PRE;
		else if (level.equalsIgnoreCase("CLEANUP"))
			return RUNLEVEL_CLEANUP;
		else if (level.equalsIgnoreCase("CLEANUP_POST"))
			return RUNLEVEL_CLEANUP_POST;
		else if (level.equalsIgnoreCase("RUNNING_PRE"))
			return RUNLEVEL_RUNNING_PRE;
		else if (level.equalsIgnoreCase("RUNNING"))
			return RUNLEVEL_RUNNING;
		else if (level.equalsIgnoreCase("RUNNING_POST"))
			return RUNLEVEL_RUNNING_POST;
		else
			return -10;
	}

	public void shutdown() {
		if (this.mbeanHandle != null)
			this.glob.unregisterMBean(this.mbeanHandle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ehrserver.common.admin.I_AdminUsage#usage()
	 */
	public java.lang.String usage() {
		return "not implemented";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ehrserver.common.admin.I_AdminUsage#getUsageUrl()
	 */
	public java.lang.String getUsageUrl() {
		return "not implemented";
	}

	/*
	 * (non-Javadoc) JMX dummy to have a copy/paste functionality in jconsole
	 * 
	 * @see org.ehrserver.common.admin.I_AdminUsage#setUsageUrl(java.lang.String)
	 */
	public void setUsageUrl(java.lang.String url) {
	}
}

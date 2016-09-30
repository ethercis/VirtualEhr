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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * SignalCatcher catches Ctrl-C and does the desired action.
 * <p />
 * Works only with JDK 1.3 and above, we check with reflection the availability
 * to still be JDK 1.2 compatible.
 */
public class SignalCatcher implements Runnable {
	private static Logger log = LogManager.getLogger(SignalCatcher.class);
	private static SignalCatcher theSignalCatcher;
	private Thread thread;
	private I_SignalListener listener;
	private boolean runDummy = false;

	public static SignalCatcher instance() {
		if (theSignalCatcher == null) {
			synchronized (SignalCatcher.class) {
				if (theSignalCatcher == null) {
					theSignalCatcher = new SignalCatcher();
				}
			}
		}
		return theSignalCatcher;
	}

	public boolean hasListener() {
		return this.listener != null;
	}

	/**
	 * You need to call initializeSession() after construction. Example for a hook:
	 * 
	 * <pre>
	 *  class Shutdown extends Thread {
	 *    public void run() {
	 *       System.exit(0);
	 *    }
	 *  }
	 *  SignalCatcher c = SignalCatcher.instance();
	 *  c.register(new Shutdown());
	 *  c.catchSignals();
	 *  ...
	 *  c.removeSignalCatcher();
	 * </pre>
	 * 
	 * @throws IllegalArgumentException
	 *             if other listener exists
	 */
	public synchronized void register(I_SignalListener listener) {
		if (this.listener != null) // Should we implement multiple listeners?
			throw new IllegalArgumentException(
					"SignalCatcher.register: There is already a listener");
		this.listener = listener;
		this.thread = new Thread(this,
				"Signal catcher thread for controlled shudown");
		this.thread.setDaemon(true);
	}

	/**
	 * Add shutdown hook.
	 * <p />
	 * Catch signals, e.g. Ctrl C to stop xmlBlaster.<br />
	 * Uses reflection since only JDK 1.3 supports it.
	 * <p />
	 * NOTE: On Linux build 1.3.0, J2RE 1.3.0 IBM build cx130-20000815 (JIT
	 * enabled: jitc) fails with Ctrl-C
	 * 
	 * @return true: Shutdown hook is established
	 */
	public boolean catchSignals() {
		Method method;
		try {
			Class cls = Runtime.getRuntime().getClass();
			Class[] paramCls = new Class[1];
			paramCls[0] = Class.forName("java.lang.Thread");
			method = cls.getDeclaredMethod("addShutdownHook", paramCls);
		} catch (java.lang.ClassNotFoundException e) {
			return false;
		} catch (java.lang.NoSuchMethodException e) {
			log.debug("No shutdown hook established");
			return false;
		}

		try {
			if (method != null) {
				Object[] params = new Object[1];
				params[0] = this.thread;
				method.invoke(Runtime.getRuntime(), params);
			}
		} catch (java.lang.reflect.InvocationTargetException e) {
			return false;
		} catch (java.lang.IllegalAccessException e) {
			return false;
		}
		return true;
	}

	/**
	 * @return true on success
	 */
	public boolean removeSignalCatcher() {

		if (this.thread == null) {
			return false;
		}

		// boolean removed =
		// Runtime.getRuntime().removeShutdownHook(this.thread);
		boolean removed = false;
		try {
			Method method;
			try {
				Class cls = Runtime.getRuntime().getClass();
				Class[] paramCls = new Class[1];
				paramCls[0] = Class.forName("java.lang.Thread");
				method = cls.getDeclaredMethod("removeShutdownHook", paramCls);
			} catch (java.lang.ClassNotFoundException e) {
				log.debug("Shutdown hook not removed: " + e.toString());
				return false;
			} catch (java.lang.NoSuchMethodException e) {
				log.debug("No shutdown hook removed");
				return false;
			}

			try {
				if (method != null) {
					Object[] params = new Object[1];
					params[0] = this.thread;
					method.invoke(Runtime.getRuntime(), params);
					removed = true; // TODO: check the real return value
					return removed;
				}
			} catch (java.lang.reflect.InvocationTargetException e) {
				log.debug("Shutdown hook not removed which is OK when we are in shutdown process already: "
						+ e.toString());
				return false;
			} catch (java.lang.IllegalAccessException e) {
				log.debug("Shutdown hook not removed: " + e.toString());
				return false;
			}
			log.debug("Shutdown hook removed");
		} finally {
			this.listener = null;

			log.debug("Removed = " + removed + " in removeSignalCatcher()");

			// This is a hack to allow the garbage collector to destroy
			// SignalCatcher
			// (An unrun thread can't be garbage collected)
			this.runDummy = true;
			try {
				this.thread.start(); // Run the Thread to allow the garbage
										// collector to clean it up
			} catch (IllegalThreadStateException e) {
				log.debug("Thread has run already: " + e.toString());
			}

			this.thread = null;
		}
		return removed;
	}

	/**
	 * This is invoked on exit
	 */
	public void run() {
		if (runDummy) { // Run the Thread to allow the garbage collector to
						// clean it up
			this.listener = null;
			return;
		}
		if (log != null) {
			log.info("Shutdown forced by user or signal (Ctrl-C).");
		}
		I_SignalListener ll = this.listener;
		if (ll != null)
			ll.shutdownHook();
		this.listener = null;
	}
}

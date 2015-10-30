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


package com.ethercis.servicemanager.service;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import org.apache.log4j.Logger;

import java.util.Hashtable;
import java.util.Iterator;

/**
 * One instance of this class is used to keep track of all cached Services.
 * 
 */
public class ServiceRegistry implements Iterable<I_Service> {
	private static String ME = "ServiceRegistry";
	private static Logger log = Logger.getLogger(ServiceRegistry.class);
	/** key=ServiceId String, value=I_Service */
	private Hashtable Services;
	Iterator<I_Service> it;

	public ServiceRegistry(RunTimeSingleton glob) {

		this.Services = new Hashtable();
		this.it = Services.entrySet().iterator();
	}

	/**
	 * Gets the Service which has previously been registered with the given id.
	 * 
	 * @param id
	 *            the key under which the Service to get has been registered.
	 * @return I_Service the Service associated to the given key. If there is no
	 *         such Service, then null is returned.
	 */
	public I_Service getService(String id) {
		for (Object key : Services.keySet()) {
			log.debug(key + ":" + Services.get(key));
		}
		if (id == null)
			return null;
		synchronized (this) {
			return (I_Service) this.Services.get(id);
		}
	}

	/**
	 * Registers the Service into this registry.
	 * 
	 * @param id
	 *            the key to use to register this Service
	 * @param Service
	 *            the Service to register
	 * @return boolean 'true' if the registration was successful. 'false' if the
	 *         Service was already registered.
	 */
	public boolean register(String id, I_Service Service) {
		log.debug("--------------------------------- "+id + ":" + Service);
		if (id == null)
			return false;
		synchronized (this) {
			if (this.Services.containsKey(id))
				return false;
			this.Services.put(id, Service);
			return false;
		}
	}

	/**
	 * unregisters the specified Service.
	 * 
	 * @param id
	 *            the id under which the Service has been registered.
	 * @return I_Service the Service which has been unregistered or null if none
	 *         was found.
	 */
	public I_Service unRegister(String id) {
		if (id == null)
			return null;
		synchronized (this) {
			return (I_Service) this.Services.remove(id);
		}
	}

	/**
	 * get an iterator to the services
	 */
	public I_Service next() {
		if (it.hasNext())
			return it.next();
		return null;
	}

	/**
	 * get the iterator
	 */
	public Iterator<I_Service> iterator() {
		return it;
	}
}

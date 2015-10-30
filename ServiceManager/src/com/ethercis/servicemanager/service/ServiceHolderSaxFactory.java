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

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.FileLocator;
import com.ethercis.servicemanager.common.SaxHandlerBase;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.ServiceConfig;

import java.io.InputStream;
import java.net.URL;

/**
 * This class parses an xml string to generate a ServiceHolder object.
 * <p>
 * 
 * <pre>
 *  &lt;action do='LOAD'
 *             onStartupRunlevel='3'
 *             sequence='5'
 *             onFail='RESOURCE_CONFIGURATION_SERVICEFAILED'/>
 * </pre>
 */
public class ServiceHolderSaxFactory extends SaxHandlerBase implements ServiceHolderFactory{
	private String ME = "ServiceHolderSaxFactory";
	private final RunTimeSingleton glob;
	private static Logger log = Logger.getLogger(ServiceHolderSaxFactory.class
			.getName());

	private ServiceHolder serviceHolder;
	private ServiceManagerException ex;

	private ServiceConfigSaxFactory serviceFactory;
	private boolean inService = false; // to set when a '<service>' tag has been
										// found (to know when to throw an ex)
	private boolean isehrserver = false;
	private String currentNode;

	/**
	 * Can be used logonservice singleton.
	 */
	public ServiceHolderSaxFactory(RunTimeSingleton glob) {
		super(glob);
		setUseLexicalHandler(true); // to allow CDATA wrapped attributes
		this.glob = glob;

		this.serviceFactory = new ServiceConfigSaxFactory(this.glob);
	}

	/**
	 * resets the factory (to be invoked before parsing)
	 */
	public void reset() {
		this.ex = null; // reset the exceptions
		this.serviceHolder = new ServiceHolder(glob);
		this.inService = false;
		this.currentNode = null;
		this.isehrserver = false;
	}

	/**
	 * returns the parsed object
	 */
	public ServiceHolder getObject() {
		return this.serviceHolder;
	}

	/**
	 * Parses the given services.xml returns a ServiceHolderData holding the
	 * data.
	 * 
	 * @param the
	 *            XML based ASCII string
	 */
	public synchronized ServiceHolder readObject(String xmlTxt)
			throws ServiceManagerException {
		if (xmlTxt == null || xmlTxt.trim().length() < 1)
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"the <ehrserver> element is empty");
		reset();
		try {
			this.init(xmlTxt); // use SAX parser to parse it (is slow)
		} catch (Throwable thr) {

			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"exception occured when parsing the <ehrserver> tag. In fact it was '"
							+ xmlTxt + "'", thr);

		}

		if (this.ex != null)
			throw ex;

		if (!this.isehrserver)
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"the string '" + xmlTxt
							+ "' does not contain the <ehrserver> tag");
		return this.serviceHolder;
	}

	/**
	 * Start element, event from SAX parser.
	 * <p />
	 * 
	 * @param name
	 *            Tag name
	 * @param attrs
	 *            the attributes of the tag
	 */
	public final void startElement(String uri, String localName, String name,
			Attributes attrs) {
		if (this.ex != null)
			return;

		if ("service".equalsIgnoreCase(name)) {
			this.inService = true;
			this.serviceFactory.reset();
		}
		if (this.inService) {
			this.serviceFactory.startElement(uri, localName, name, attrs);
			return;
		}

		if ("node".equalsIgnoreCase(name)) {
			String id = null;
			if (attrs != null)
				id = attrs.getValue("id");
			if (id == null || id.length() < 1)
				this.ex = new ServiceManagerException(this.glob,
						SysErrorCode.RESOURCE_CONFIGURATION, ME
								+ ".startElement",
						"in the <node> tag the 'id' attribute is mandatory:found none");
			this.currentNode = id;
			return;
		}

		if ("ehrserver".equalsIgnoreCase(name)) {
			this.isehrserver = true;
			return;
		}

		log.warn("startElement: unknown tag '" + name + "'");
	}

	/**
	 * The characters to be filled
	 */
	public void characters(char[] ch, int start, int length) {
		if (this.inService)
			this.serviceFactory.characters(ch, start, length);
	}

	public void startCDATA() {
		if (this.inService)
			this.serviceFactory.startCDATA();
	}

	/**
	 * End element, event from SAX parser.
	 * <p />
	 * 
	 * @param name
	 *            Tag name
	 */
	public void endElement(String uri, String localName, String name) {
		if (this.ex != null)
			return;
		if (this.inService) {
			this.serviceFactory.endElement(uri, localName, name);
			if ("service".equalsIgnoreCase(name)) {
				if (this.currentNode != null) {
					this.serviceHolder.addServiceConfig(this.currentNode,
							this.serviceFactory.getObject());
				} else
					this.serviceHolder
							.addDefaultServiceConfig(this.serviceFactory
									.getObject());
				this.inService = false;
			}
			return;
		}
		if ("node".equalsIgnoreCase(name)) {
			this.currentNode = null;
			return;
		}
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *            indenting of tags for nice response
	 * @return internal state of the RequestBroker logonservice a XML ASCII string
	 */
	public final String writeObject(ServiceHolder serviceConfig,
			String extraOffset) {
		return serviceConfig.toXml(extraOffset);
	}

	/**
	 * A human readable name of this factory
	 * 
	 * @return "ServiceHolderSaxFactory"
	 */
	public String getName() {
		return "ServiceHolderSaxFactory";
	}

	/**
	 * Reads the configuration file <code>services.xml</code>. It first searches
	 * the file according to the ehrserver search strategy specified in the
	 * engine.runlevel requirement.
	 * 
	 * @see <a
	 *      href="http://www.ehrserver.org/ehrserver/doc/requirements/engine.runlevel.html">engine.runlevel
	 *      requirement</a>
	 */
	public ServiceHolder readConfigFile() throws ServiceManagerException {
		log.debug("readConfigFile");
		FileLocator fileLocator = new FileLocator(this.glob);
		URL url = fileLocator.findFileInSearchPath("servicesFile",
				"services.xml");

		// null pointer check here ....
		if (url == null) {
			throw new ServiceManagerException(
					this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION,
					ME + ".readConfigFile",
					"the file 'services.xml' has not been found in the search path nor in the property 'servicesFile'");
		}

		log.debug("readConfigFile: the file is '" + url.getFile() + "'");
		try {
			InputStream fis = url.openStream();
			InputSource inSource = new InputSource(fis);
			reset();
			init(url.toString(), inSource);
			ServiceHolder ret = getObject();
			ServiceConfig[] arr = ret.getAllServiceConfig(this.glob.getNodeId()
					.getId());
			for (int i = 0; i < arr.length; i++)
				arr[i].registerMBean();
			log.debug(".readConfigFile. The content: \n" + ret.toXml());
			return ret;
		} catch (java.io.IOException ex) {
			throw new ServiceManagerException(glob,
					SysErrorCode.RESOURCE_CONFIGURATION,
					ME + ".readConfigFile", "the file '" + url.getFile()
							+ "' has not been found", ex);
		}
	}

	@Override
	public ServiceHolder loadServiceHolder() throws ServiceManagerException {
		return readConfigFile();
	}

}

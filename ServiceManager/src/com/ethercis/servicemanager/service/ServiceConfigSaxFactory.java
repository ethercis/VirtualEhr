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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.AttributeSaxFactory;
import com.ethercis.servicemanager.common.SaxHandlerBase;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.RunLevelActionSaxFactory;
import com.ethercis.servicemanager.runlevel.ServiceConfig;

/**
 * This class parses an xml string to generate a ServiceConfig object.
 * <p>
 * 
 * <pre>
 *  &lt;action do='LOAD'
 *             onStartupRunlevel='3'
 *             sequence='5'
 *             onFail='RESOURCE_CONFIGURATION_PLUGINFAILED'/>
 * </pre>
 */
public class ServiceConfigSaxFactory extends SaxHandlerBase {
	private String ME = "ServiceConfigSaxFactory";
	private final RunTimeSingleton glob;
	private static Logger log = Logger.getLogger(ServiceConfigSaxFactory.class);

	private ServiceConfig serviceConfig;
	private boolean isService = false; // to set when an 'action' tag has been
										// found (to know when to throw an ex)
	private ServiceManagerException ex;

	private RunLevelActionSaxFactory actionFactory;
	private boolean inAction = false;

	private AttributeSaxFactory attributeFactory;

	/*
	 * private String attributeKey; private StringBuffer attributeValue; private
	 * boolean inAttribute = false; private boolean wrappedInCDATA = false; //
	 * for example: <attribute id='publishQos'><![CDATA[ bla ]]></attribute>
	 * private boolean embeddedCDATA = false; // for example: <attribute
	 * id='publishQos'><qos><![CDATA[<expiration
	 * lifeTime='4000'/>]]></qos></attribute> private int subTagCounter;
	 */

	/**
	 * Can be used logonservice singleton.
	 */
	public ServiceConfigSaxFactory(RunTimeSingleton glob) {
		super(glob);
		setUseLexicalHandler(true); // to allow CDATA wrapped attributes
		this.glob = glob;

		this.actionFactory = new RunLevelActionSaxFactory(this.glob);
		this.attributeFactory = new AttributeSaxFactory(this.glob, null);
	}

	/**
	 * resets the factory (to be invoked before parsing)
	 */
	public void reset() {
		this.ex = null; // reset the exeptions
		this.serviceConfig = new ServiceConfig(glob);
		this.attributeFactory.reset(this.serviceConfig);
		this.inAction = false;
		this.isService = false;
	}

	/**
	 * returns the parsed object
	 */
	public ServiceConfig getObject() {
		return this.serviceConfig;
	}

	/**
	 * Parses the given xml Qos and returns a ServiceConfigData holding the
	 * data. Parsing of update() and publish() QoS is supported here.
	 * 
	 * @param the
	 *            XML based ASCII string
	 */
	public synchronized ServiceConfig readObject(String xmlTxt)
			throws ServiceManagerException {
		if (xmlTxt == null || xmlTxt.trim().length() < 1)
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"the <service> element is empty");
		reset();
		try {
			this.init(xmlTxt); // use SAX parser to parse it (is slow)
		} catch (Throwable thr) {

			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"exception occured when parsing the <service> tag. In fact it was '"
							+ xmlTxt + "'");

		}

		if (this.ex != null)
			throw ex;

		if (!this.isService)
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"the string '" + xmlTxt
							+ "' does not contain the <service> tag");
		return this.serviceConfig;
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

		if ("action".equalsIgnoreCase(name)) {
			this.inAction = true;
			this.actionFactory.reset();
		}
		if (this.inAction) {
			this.actionFactory.startElement(uri, localName, name, attrs);
			return;
		}

		if ("service".equalsIgnoreCase(name)) {
			this.isService = true;
			if (attrs != null) {
				int len = attrs.getLength();
				for (int i = 0; i < len; i++) {
					String key = attrs.getQName(i);
					String value = attrs.getValue(i).trim();

					if ("id".equalsIgnoreCase(key)) {
						this.serviceConfig.setId(value);
						continue;
					}
					if ("create".equalsIgnoreCase(key)) {
						this.serviceConfig.setCreateInternal(Boolean.valueOf(
								value).booleanValue());
						continue;
					}
					if ("className".equalsIgnoreCase(key)) {
						this.serviceConfig.setClassName(value);
						continue;
					}
					if ("jar".equalsIgnoreCase(key)) {
						this.serviceConfig.setJar(value);
						continue;
					}
					log.warn("startElement: " + key + "='" + value
							+ "' is unknown");
				}

			}
			return;
		}
		if ("attribute".equalsIgnoreCase(name)
				|| this.attributeFactory.isInAttribute())
			this.attributeFactory.startElement(uri, localName, name, attrs);
	}

	public void startCDATA() {
		this.attributeFactory.startCDATA();
	}

	/**
	 * The characters to be filled
	 */
	public void characters(char[] ch, int start, int length) {
		this.attributeFactory.characters(ch, start, length);
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
		if (this.inAction) {
			this.actionFactory.endElement(uri, localName, name);
			if ("action".equalsIgnoreCase(name)) {
				this.serviceConfig.addAction(this.actionFactory.getObject());
				this.inAction = false;
			}
			return;
		}
		if ("attribute".equalsIgnoreCase(name)
				|| this.attributeFactory.isInAttribute())
			this.attributeFactory.endElement(uri, localName, name);
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *            indenting of tags for nice response
	 * @return internal state of the RequestBroker logonservice a XML ASCII string
	 */
	public final String writeObject(ServiceConfig serviceConfig,
			String extraOffset) {
		return serviceConfig.toXml(extraOffset);
	}

	/**
	 * A human readable name of this factory
	 * 
	 * @return "ServiceConfigSaxFactory"
	 */
	public String getName() {
		return "ServiceConfigSaxFactory";
	}
}

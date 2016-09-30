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


package com.ethercis.servicemanager.runlevel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.SaxHandlerBase;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * This class parses an xml string to generate a RunLevelAction object.
 * <p>
 * 
 * <pre>
 *  &lt;action do='LOAD'
 *             onStartupRunlevel='3'
 *             sequence='5'
 *             onFail='RESOURCE_CONFIGURATION_SERVICEFAILED'/>
 * </pre>
 */
public class RunLevelActionSaxFactory extends SaxHandlerBase {
	private String ME = "RunLevelActionSaxFactory";
	private final RunTimeSingleton glob;
	private static Logger log = LogManager.getLogger(RunLevelActionSaxFactory.class
			.getName());

	private RunLevelAction runLevelAction;
	private boolean isAction = false; // to set when an 'action' tag has been
										// found (to know when to throw an ex)
	private ServiceManagerException ex = null;

	/**
	 * Can be used logonservice singleton.
	 */
	public RunLevelActionSaxFactory(RunTimeSingleton glob) {
		super(glob);
		setUseLexicalHandler(true); // to allow CDATA wrapped attributes
		this.glob = glob;

	}

	public void reset() {
		this.isAction = false;
		this.ex = null; // reset the exceptions
		this.runLevelAction = new RunLevelAction(glob);
	}

	public RunLevelAction getObject() {
		return this.runLevelAction;
	}

	/**
	 * Parses the given xml Qos and returns a RunLevelActionData holding the
	 * data. Parsing of update() and publish() QoS is supported here.
	 * 
	 * @param the
	 *            XML based ASCII string
	 */
	public synchronized RunLevelAction readObject(String xmlTxt)
			throws ServiceManagerException {
		if (xmlTxt == null || xmlTxt.trim().length() < 1)
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"the action element is empty");
		reset();
		try {
			this.init(xmlTxt); // use SAX parser to parse it (is slow)
		} catch (Throwable thr) {

			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"exception occured when parsing the <action> tag. In fact it was '"
							+ xmlTxt + "'");

		}

		if (this.ex != null)
			throw ex;

		if (!this.isAction)
			throw new ServiceManagerException(this.glob,
					SysErrorCode.RESOURCE_CONFIGURATION, ME + ".readObject",
					"the string '" + xmlTxt
							+ "' does not contain the <action> tag");
		return this.runLevelAction;
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
			this.isAction = true;
			if (attrs != null) {
				int len = attrs.getLength();
				for (int i = 0; i < len; i++) {
					String key = attrs.getQName(i);
					String value = attrs.getValue(i).trim();

					if ("do".equalsIgnoreCase(key)) {
						this.runLevelAction.setDo(value);
						continue;
					}
					if ("onStartupRunlevel".equalsIgnoreCase(key)) {
						try {
							int level = Integer.parseInt(value);
							this.runLevelAction.setOnStartupRunlevel(level);
						} catch (NumberFormatException ex) {
							log.warn("startElement onStartupRunlevel='" + value
									+ "' is not an integer");
						}
						continue;
					}
					if ("onShutdownRunlevel".equalsIgnoreCase(key)) {
						try {
							int level = Integer.parseInt(value);
							this.runLevelAction.setOnShutdownRunlevel(level);
						} catch (NumberFormatException ex) {
							log.warn("startElement onShutdownRunlevel='"
									+ value + "' is not an integer");
						}
						continue;
					}
					if ("sequence".equalsIgnoreCase(key)) {
						try {
							int sequence = Integer.parseInt(value);
							this.runLevelAction.setSequence(sequence);
						} catch (NumberFormatException ex) {
							log.warn("startElement sequence='" + value
									+ "' is not an integer");
						}
						continue;
					}
					if ("onFail".equalsIgnoreCase(key)) {
						if (value.length() > 1) { // if empty ignore it

							log.debug("startElement: onFail : " + key + "='"
									+ value + "'");
							try {
								SysErrorCode code = SysErrorCode
										.toErrorCode(value);
								this.runLevelAction.setOnFail(code);
							} catch (IllegalArgumentException ex) {
								log.warn("startElement onFail='" + value
										+ "' is an unknown error code");
								this.ex = new ServiceManagerException(
										this.glob,
										SysErrorCode.RESOURCE_CONFIGURATION,
										ME + ".startElement",
										"check the spelling of your error code, it is unknown and probably wrongly spelled",
										ex);
							}
						}
						continue;
					}
					log.warn("startElement: unknown attribute '" + key
							+ "' with value '" + value + "' used");
				}
			}
			return;
		}

		log.warn("startElement: unknown tag '" + name + "'");
	}

	/**
	 * End element, event from SAX parser.
	 * <p />
	 * 
	 * @param name
	 *            Tag name
	 */
	public void endElement(String uri, String localName, String name) {
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *            indenting of tags for nice response
	 * @return internal state of the RequestBroker logonservice a XML ASCII string
	 */
	public final String writeObject(RunLevelAction runLevelAction,
			String extraOffset) {
		return runLevelAction.toXml(extraOffset);
	}

	/**
	 * A human readable name of this factory
	 * 
	 * @return "RunLevelActionSaxFactory"
	 */
	public String getName() {
		return "RunLevelActionSaxFactory";
	}
}

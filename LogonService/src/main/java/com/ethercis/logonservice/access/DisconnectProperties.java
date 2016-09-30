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

//Copyright
package com.ethercis.logonservice.access;
/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */
import com.ethercis.logonservice.session.SessionProperties;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.property.PropBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

/**
 * This class encapsulates the qos of a logout() or disconnect()
 * 
 * @see <a
 *      href="http://www.xmlBlaster.org/xmlBlaster/doc/requirements/interface.disconnect.html">The
 *      interface.disconnect requirement</a>
 * @see org.xmlBlaster.util.qos.DisconnectQosSaxFactory
 * @see org.xmlBlaster.test.classtest.DisconnectQosTest
 */
public class DisconnectProperties extends SessionProperties implements
		java.io.Serializable, Cloneable {
	private static Logger log = LogManager.getLogger(DisconnectProperties.class
			.getName());
	private static final long serialVersionUID = 2690405423464959314L;

	private PropBoolean clearSessions = new PropBoolean(false);

	/**
	 * Default constructor
	 */
	public DisconnectProperties(RunTimeSingleton glob) {
		this(glob, null);
	}

	/**
	 * Parses the given ASCII logout QoS.
	 */
	public DisconnectProperties(RunTimeSingleton glob,
			I_SessionClientProperties serialData) {
		super(glob, serialData, MethodName.DISCONNECT);
	}

	/**
	 * @return true/false
	 */
	public boolean isPersistent() {
		return false;
	}

	/**
	 * Converts the data into a valid XML ASCII string.
	 * 
	 * @return An XML ASCII string
	 */
	public String toString() {
		return toXml();
	}

	/**
	 * Return true if we shall kill all other sessions of this user on logout
	 * (defaults to false).
	 * 
	 * @return false
	 */
	public boolean clearSessions() {
		return this.clearSessions.getValue();
	}

	/**
    */
	public PropBoolean clearSessionsProp() {
		return this.clearSessions;
	}

	/**
	 * @param true if we shall kill all other sessions of this user on logout
	 *        (defaults to false).
	 */
	public void clearSessions(boolean del) {
		this.clearSessions.setValue(del);
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * The default is to include the security string
	 * 
	 * @return internal state of the RequestBroker logonservice a XML ASCII string
	 */
	public final String toXml() {
		return toXml(null, null);
	}

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *            indenting of tags for nice response
	 * @return internal state of the RequestBroker logonservice a XML ASCII string
	 */
	public final String toXml(String extraOffset, Properties props) {
		// return this.factory.writeObject(this, extraOffset, props);
		return "not implemented";
	}

	/**
	 * Returns a deep clone, you can change safely all data.
	 */
	public Object clone() {
		DisconnectProperties newOne = null;
		newOne = (DisconnectProperties) super.clone();
		synchronized (this) {
			newOne.clearSessions = (PropBoolean) this.clearSessions.clone();
		}
		return newOne;
	}

	/** For testing: java org.xmlBlaster.common.qos.DisconnectQosData */
	/*
	 * public static void main(String[] args) { try { Global glob = new
	 * Global(args); DisconnectQosData qos = new DisconnectQosData(glob);
	 * qos.clearSessions(true); qos.deleteSubjectQueue(false);
	 * System.out.println(qos.toXml()); } catch(Throwable e) {
	 * System.err.println("TestFailed : " + e.toString()); } }
	 */
}

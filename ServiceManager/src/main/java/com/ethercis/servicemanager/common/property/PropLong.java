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


package com.ethercis.servicemanager.common.property;

public final class PropLong extends PropEntry implements java.io.Serializable, Cloneable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2283655533085090317L;
	private long valueDefault; // Remember default setting for usage response etc.
	private long value;

	/**
	 * Constructor for the default value
	 */
	public PropLong(long value) {
		this(null, value);
	}

	/*
	 * Constructor for the default value
	 * @param propName The environment property name
	 */
	public PropLong(String propName, long value) {
		super(propName);
		this.valueDefault = value;
		this.value = value;
	}

	/**
	 * @return "long"
	 */
	public final String getTypeString() {
		return "long";
	}

	/**
	 * @return The value in String form
	 */
	public final String getValueString() {
		return ""+this.value;
	}

	/**
	 * Overwrites any default or environment settings. 
	 * Used by clients to set hardcoded values or by SAX parser if enforced by XML
	 */
	public void setValue(long value) {
		this.value = value;
		super.creationOrigin = CREATED_BY_SETTER;
	}

	/**
	 * @param The new value logonservice String type, will be converted to native type
	 * @param creationOrigin e.g. PropEntry.CREATED_BY_JVMENV
	 */
	public void setValue(String value, int creationOrigin) {
		if (value == null) return;
		setValue(Long.parseLong(value), creationOrigin);
	}

	/**
	 * @param the new value to use
	 * @param creationOrigin e.g. PropEntry.CREATED_BY_JVMENV
	 */
	public void setValue(long value, int creationOrigin) {
		if (creationOrigin >= super.creationOrigin) {
			this.value = value;
			super.creationOrigin = creationOrigin;
		}
		else
			System.out.println("Old value=" + this.value + " not overwritten with " + value + " logonservice old origin=" + super.creationOrigin + " and new origin=" + creationOrigin + " is weaker");
	}

	public long getValue() {
		return this.value;
	}

	/**
	 * Overwrite the default value given to the constructor. 
	 */
	public void setDefaultValue(long value) {
		this.valueDefault = value;
		if (CREATED_BY_DEFAULT == super.creationOrigin) {
			this.value = value; // overwrite the default setting
		}
	}

	public long getDefaultValue() {
		return this.valueDefault;
	}

	/**
	 * Returns a shallow clone, you can change safely all basic or immutable types
	 * like boolean, String, int.
	 */
	public Object clone() {
		return super.clone();
	}

}

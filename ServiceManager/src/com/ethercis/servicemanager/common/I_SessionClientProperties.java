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
package com.ethercis.servicemanager.common;

import java.util.Map;

import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public interface I_SessionClientProperties {

	/**
	 * Access the client property.
	 * 
	 * @param name
	 *            The property key
	 * @return The ClientProperty instance or null if not found
	 */
	public abstract ClientProperty getClientProperty(String name);

	/**
	 * Check for client property.
	 * 
	 * @param name
	 *            The property key
	 * @return true if the property exists
	 */
	public abstract boolean propertyExists(String name);

	/**
	 * Access the String client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract String getClientProperty(String name, String defaultValue);

	/**
	 * Access the integer client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract int getClientProperty(String name, int defaultValue);

	/**
	 * Access the boolean client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract boolean getClientProperty(String name, boolean defaultValue);

	/**
	 * Access the double client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract double getClientProperty(String name, double defaultValue);

	/**
	 * Access the float client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract float getClientProperty(String name, float defaultValue);

	/**
	 * Access the byte client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract byte getClientProperty(String name, byte defaultValue);

	/**
	 * Access the byte[] client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract byte[] getClientProperty(String name, byte[] defaultValue);

	/**
	 * Access the byte[] client property assuming is Base64 encoded (if f.ex.
	 * parameter sent by a client)
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract byte[] getEncodedClientProperty(String name,
			byte[] defaultValue);

	/**
	 * Access the long client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract long getClientProperty(String name, long defaultValue);

	/**
	 * Access the short client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract short getClientProperty(String name, short defaultValue);

	/**
	 * Access the Object client property.
	 * 
	 * @param name
	 *            The property key
	 * @param defaultValue
	 *            The value to return if the property is not known
	 */
	public abstract Object getClientProperty(String name, Object defaultValue);

	/**
	 * Access all client properties.
	 * 
	 * @return a map The return is unordered and the map values are of type
	 *         ClientProperty.
	 * @see org.xmlBlaster.util.qos.ClientProperty
	 */
	public abstract Map<String, ClientProperty> getClientProperties();

	/**
	 * @return never null
	 */
	public abstract ClientProperty[] getClientPropertyArr();

	public abstract String writePropertiesXml(String offset);

	public abstract String writePropertiesXml(String offset,
			boolean forceReadable);

	public abstract String getContentCharset();

	/**
	 * Convenience method to get the raw content logonservice a string, the encoding is
	 * UTF-8 if not specified by clientProperty
	 * {@link Constants#CLIENTPROPERTY_CONTENT_CHARSET}
	 * 
	 * @return never null
	 */
	public abstract String getContentStr(byte[] msgContent)
			throws ServiceManagerException;

	public abstract String getContentStrNoEx(byte[] msgContent);

	public abstract Map<String, String> clientProps2StringMap();

	public abstract void addClientProperty(String key, Object value);

}
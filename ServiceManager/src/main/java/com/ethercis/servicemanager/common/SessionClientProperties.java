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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class SessionClientProperties implements I_SessionClientProperties {

    public SessionClientProperties(RunTimeSingleton glob) {
	super();
	this.glob = glob;
    }

    /**
     * map to add more client specific parameters
     */
    private Map<String, ClientProperty> clientProperties = new HashMap<String, ClientProperty>();

    RunTimeSingleton glob;

    /**
     * Sets the client property to the given value
     */
    public final void addClientProperty(ClientProperty clientProperty) {
	this.clientProperties.put(clientProperty.getName(), clientProperty);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param type
     *            For example Constants.TYPE_FLOAT
     * @param value
     *            Of any type, it will be forced to the given <code>type</code>
     */
    public final void addClientProperty(String key, String type, Object value) {
	String encoding = null;
	String str = (value == null) ? null : value.toString();
	ClientProperty clientProperty = new ClientProperty(key, type, encoding,
		str);
	this.clientProperties.put(clientProperty.getName(), clientProperty);
    }

    /**
     * Sets the client property to the given value and encoding
     * 
     * @param key
     * @param type
     *            For example Constants.TYPE_FLOAT
     * @param encoding
     *            for example Constants.ENCODING_BASE64
     * @param value
     *            Of any type, it will be forced to the given <code>type</code>
     */
    public final void addClientProperty(String key, String type,
	    String encoding, Object value) {
	String str = (value == null) ? null : value.toString();
	ClientProperty clientProperty = new ClientProperty(key, type, encoding,
		str);
	this.clientProperties.put(clientProperty.getName(), clientProperty);
    }

    /**
     * Sets the client property to the given value and encoding
     * 
     * @param key
     * @param type
     *            For example Constants.TYPE_FLOAT
     * @param encoding
     *            for example Constants.ENCODING_BASE64
     * @param value
     *            Of any type, it will be forced to the given <code>type</code>
     */
    public final void addClientProperty(String key, byte[] value) {
	ClientProperty clientProperty = new ClientProperty(key, value);
	this.clientProperties.put(clientProperty.getName(), clientProperty);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     *            for example a Float or Integer value
     */
    public final void addClientProperty(String key, Object value) {
	addClientProperty(key, ClientProperty.getPropertyType(value), value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, boolean value) {
	addClientProperty(key, Constants.TYPE_BOOLEAN, "" + value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, int value) {
	addClientProperty(key, Constants.TYPE_INT, "" + value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, byte value) {
	addClientProperty(key, Constants.TYPE_BYTE, "" + value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, long value) {
	addClientProperty(key, Constants.TYPE_LONG, "" + value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, short value) {
	addClientProperty(key, Constants.TYPE_SHORT, "" + value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, double value) {
	addClientProperty(key, Constants.TYPE_DOUBLE, "" + value);
    }

    /**
     * Sets the client property to the given value
     * 
     * @param key
     * @param value
     */
    public final void addClientProperty(String key, float value) {
	addClientProperty(key, Constants.TYPE_FLOAT, "" + value);
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String)
	 */
    @Override
	public final ClientProperty getClientProperty(String name) {
	if (name == null)
	    return null;
	return (ClientProperty) this.clientProperties.get(name);
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#propertyExists(java.lang.String)
	 */
    @Override
	public final boolean propertyExists(String name) {
	if (name == null)
	    return false;
	return (this.clientProperties.get(name) != null);
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, java.lang.String)
	 */
    @Override
	public final String getClientProperty(String name, String defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getStringValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, int)
	 */
    @Override
	public final int getClientProperty(String name, int defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getIntValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, boolean)
	 */
    @Override
	public final boolean getClientProperty(String name, boolean defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getBooleanValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, double)
	 */
    @Override
	public final double getClientProperty(String name, double defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getDoubleValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, float)
	 */
    @Override
	public final float getClientProperty(String name, float defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getFloatValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, byte)
	 */
    @Override
	public final byte getClientProperty(String name, byte defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getByteValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, byte[])
	 */
    @Override
	public final byte[] getClientProperty(String name, byte[] defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getBlobValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getEncodedClientProperty(java.lang.String, byte[])
	 */
    @Override
	public final byte[] getEncodedClientProperty(String name,
	    byte[] defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	p.setEncoding(Constants.ENCODING_BASE64);
	return p.getBlobValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, long)
	 */
    @Override
	public final long getClientProperty(String name, long defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getLongValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, short)
	 */
    @Override
	public final short getClientProperty(String name, short defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getShortValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperty(java.lang.String, java.lang.Object)
	 */
    @Override
	public final Object getClientProperty(String name, Object defaultValue) {
	if (name == null)
	    return defaultValue;
	ClientProperty p = (ClientProperty) this.clientProperties.get(name);
	if (p == null)
	    return defaultValue;
	return p.getObjectValue();
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientProperties()
	 */
    @Override
	public final Map<String, ClientProperty> getClientProperties() {
	return this.clientProperties;
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getClientPropertyArr()
	 */
    @Override
	public final ClientProperty[] getClientPropertyArr() {
	if (this.clientProperties == null)
	    return new ClientProperty[0];
	return (ClientProperty[]) this.clientProperties.values().toArray(
		new ClientProperty[this.clientProperties.size()]);
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#writePropertiesXml(java.lang.String)
	 */
    @Override
	public final String writePropertiesXml(String offset) {
	return writePropertiesXml(offset, false);
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#writePropertiesXml(java.lang.String, boolean)
	 */
    @Override
	public final String writePropertiesXml(String offset, boolean forceReadable) {
	if (this.clientProperties.size() > 0) {
	    Object[] arr = this.clientProperties.keySet().toArray();
	    StringBuilder sb = new StringBuilder(arr.length * 256);
	    for (int i = 0; i < arr.length; i++) {
		ClientProperty p = this.clientProperties.get(arr[i]);
		sb.append(p.toXml(offset, null, forceReadable));
	    }
	    return sb.toString();
	}
	return "";
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getContentCharset()
	 */
    @Override
	public String getContentCharset() {
	return getClientProperty(Constants.CLIENTPROPERTY_CONTENT_CHARSET,
		Constants.UTF8_ENCODING);
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getContentStr(byte[])
	 */
    @Override
	public String getContentStr(byte[] msgContent) throws ServiceManagerException {
	if (msgContent == null)
	    return null;
	String encoding = getClientProperty(
		Constants.CLIENTPROPERTY_CONTENT_CHARSET,
		Constants.UTF8_ENCODING);

	try {
	    return new String(msgContent, encoding);
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	    throw new ServiceManagerException(glob, SysErrorCode.USER_ILLEGALARGUMENT,
		    "UpdateQos-ClientProperty",
		    "Could not encode according to '" + encoding + "': "
			    + e.getMessage());
	}
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#getContentStrNoEx(byte[])
	 */
    @Override
	public String getContentStrNoEx(byte[] msgContent) {
	if (msgContent == null)
	    return null;
	String encoding = getClientProperty(
		Constants.CLIENTPROPERTY_CONTENT_CHARSET,
		Constants.UTF8_ENCODING);

	try {
	    return new String(msgContent, encoding);
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	    System.err
		    .println("UpdateQos-ClientProperty: Could not encode according to '"
			    + encoding + "': " + e.getMessage());
	    return Constants.toUtf8String(msgContent);
	}
    }

    /* (non-Javadoc)
	 * @see I_SessionClientProperties#clientProps2StringMap()
	 */
    @Override
	public Map<String, String> clientProps2StringMap() {
	Map<String, String> retmap = new HashMap<String, String>();

	for (String k : clientProperties.keySet()) {
	    if (clientProperties.get(k) == null)
		retmap.put(k, "");
	    else
		retmap.put(k, clientProperties.get(k).getStringValue());
	}
	return retmap;
    }

    @Override
	public String toString() {
	   StringBuffer sb =new StringBuffer();
	   sb.append('{');
	   for(String key:clientProperties.keySet()){
	      sb.append(key);
	      sb.append('=');
	      sb.append(clientProperties.get(key));
	      sb.append(';');
	   }
	   sb.append('}');
	    return sb.toString();
	}

}

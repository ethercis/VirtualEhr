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
//Copyright
package com.ethercis.logonservice.security;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_SecurityProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;


/**
 * Helper class for Java clients.
 * <p />
 * This class only generates a login() or initializeSession() properties
 */
public class SecurityProperties implements I_SecurityProperties
{
	private String type = "gui";
	private String version = "1.0";
	private String user = "";
	private String passwd = "";
	private String IP = "";
	private RunTimeSingleton controller;

	public SecurityProperties(RunTimeSingleton glob)
	{
		this.controller = glob;
	}

	public SecurityProperties(RunTimeSingleton glob, String loginName, String password)
	{
		this.controller = glob;
		this.user = loginName;
		this.passwd = password;
	}

	public void parse(I_ConnectProperties props) throws ServiceManagerException
	{
		this.user = props.getSecurityProperties().getUserId();
		this.passwd = "";
	}

	public void setUserId(String userId)
	{
		this.user = userId;
	}

	public String getUserId()
	{
		return user;
	}

	public String getPluginType()
	{
		return type;
	}

	public String getPluginVersion()
	{
		return version;
	}

	public void setCredential(String cred)
	{
		this.passwd = cred;
	}

	public String getCredential()
	{
		return this.passwd;
	}

	public void setClientIp (String ip){
		this.IP = ip;
	}

	public String getClientIp(){
		return IP;
	}



	public String toXml(String extraOffset)
	{
		StringBuffer sb = new StringBuffer(200);
		String offset = "\n   ";
		if (extraOffset == null) extraOffset = "";
		offset += extraOffset;

		if(passwd==null) passwd="";
		if(user==null) user="";

		sb.append(offset).append("<securityService type=\"").append(type).append("\" version=\"").append(version).append("\">");
		// The XmlRpc driver does not like it.
		sb.append(offset).append("   <![CDATA[");
		sb.append(offset).append("      <user>").append(user).append("</user>");
		sb.append(offset).append("      <passwd>").append(passwd).append("</passwd>");
		sb.append(offset).append("   ]]>");
		sb.append(offset).append("</securityService>");
		return sb.toString();
	}

}

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
package com.ethercis.logonservice.access;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;

import java.io.Serializable;

public class QueryUnit implements Serializable, I_QueryUnit
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 5407594346531246551L;
	private String					clientIP;									// optional client IP address
	private final String			resource;									// resource in service (e.g. path)
	private final String			tag;										// specify the tag for permission check
    private String                  sessionId;
																				// (ex. Constants.PATH_TAG)
	private final MethodName		method;									// method name (e.g. method)
	private final MethodName		action;									// action (e.g. GET, POST, etc.)
	private I_SessionClientProperties	parameters;								// query parameters
	private I_SessionClientProperties	headers;

	public QueryUnit(I_ServiceRunMode.DialectSpace dialectSpace, String action, String tag, I_SessionClientProperties headerprops, String method, String resource, I_SessionClientProperties parameters)
	{
		super();
		// convert the string action name into a methodname
		MethodName actionname = MethodName.toMethodName(action);
		MethodName methodname = MethodName.toMethodName(method);
		this.method = methodname;
		this.action = actionname;
		this.resource = resource;
		this.parameters = parameters;
		this.headers = headerprops;
		this.tag = (tag == null) ? Constants.PATH_TAG : tag; // default is PATH

		// maybe used to limit host for user...
		if (headerprops != null) {
            this.clientIP = headerprops.getClientProperty(I_SessionManager.CLIENT_IP, "");
            this.sessionId = headerprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), "");
        }
	}

	public QueryUnit(I_ServiceRunMode.DialectSpace dialectSpace, MethodName action, String tag, I_SessionClientProperties headerprops, String method, String resource, I_SessionClientProperties parameters)
	{
		super();
		// convert the string action name into a methodname
		MethodName methodname = MethodName.toMethodName(method);
		this.method = methodname;
		this.action = action;
		this.resource = resource;
		this.parameters = parameters;
		this.tag = (tag == null) ? Constants.PATH_TAG : tag; // default is PATH

		// maybe used to limit host for user...
        if (headerprops != null) {
            this.clientIP = headerprops.getClientProperty(I_SessionManager.CLIENT_IP, "");
            this.sessionId = headerprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), "");
        }
	}

	public QueryUnit(I_ServiceRunMode.DialectSpace dialectSpace, MethodName action, String tag, I_SessionClientProperties headerprops, MethodName method, String resource, I_SessionClientProperties parameters)
	{
		super();
		// convert the string action name into a methodname
		this.method = method;
		this.action = action;
		this.resource = resource;
		this.parameters = parameters;
		this.tag = (tag == null) ? Constants.PATH_TAG : tag; // default is PATH

        if (headerprops != null) {
            this.clientIP = headerprops.getClientProperty(I_SessionManager.CLIENT_IP, "");
            this.sessionId = headerprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(dialectSpace), "");
        }
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getClientIP()
	 */
	@Override
	public String getClientIP()
	{
		return clientIP;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#setClientIP(java.lang.String)
	 */
	@Override
	public void setClientIP(String clientIP)
	{
		this.clientIP = clientIP;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getMethod()
	 */
	@Override
	public MethodName getMethod()
	{
		return method;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getAction()
	 */
	@Override
	public MethodName getAction()
	{
		return action;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getParameters()
	 */
	@Override
	public I_SessionClientProperties getParameters()
	{
		return parameters;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#setParameters(com.ethercis.common.common.SessionClientProperties)
	 */
	@Override
	public void setParameters(I_SessionClientProperties parameters)
	{
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getHeaders()
	 */
	@Override
	public I_SessionClientProperties getHeaders()
	{
		return headers;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#setHeaders(com.ethercis.common.common.SessionClientProperties)
	 */
	@Override
	public void setHeaders(I_SessionClientProperties headers)
	{
		this.headers = headers;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getResource()
	 */
	@Override
	public String getResource()
	{
		return resource;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#getTag()
	 */
	@Override
	public String getTag()
	{
		return tag;
	}
	

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.access.I_QueryUnit#toString()
	 */
	@Override
	public String toString()
	{
		String result = "QueryUnit [clientIP=" + clientIP + ", resource=" + resource + ", tag=" + tag;
		
		if(method!=null)
		{
			result += ", method=" + method.toString();
		}else{
			result += ", method=";
		}
		
		if(action!=null)
		{
			result += ", action=" + action.toString();
		}else{
			result += ", action=";
		}
		
		if(parameters!=null)
		{
			result += ", parameters=" + parameters.toString();
		}else{
			result += ", parameters=";
		}
		
		if(headers!=null)
		{
			result += ", headers=" + headers.toString()+ "]";	
		}else{
			result += ", headers=]";	
		}
				
		return result;	
	}

}

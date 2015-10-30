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
package com.ethercis.vehr.parser;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;

import javax.servlet.http.HttpServletRequest;

/**
 * utility class to perform various handling on http request URI
 * <p>
 * The request URI generally consists of the following:<p>
 * <code>/service[/resource....]/method</code>
 * <p>
 * the aim of this class is to take the path and return its fields
 * @author Christian Chevalley
 *
 */

public abstract class URIParser implements I_URIParser {
    protected final I_ServiceRunMode.DialectSpace dialectSpace;
    String delimiter = "/"; //simple forward slash delimiter
    protected String[] pathitems; //array for the split
    protected String path;
    protected RunTimeSingleton global;
	
	/**
	 * create a new parser for path, check format
	 * @param global
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public URIParser(RunTimeSingleton global) {
		this.global = global;

        //figure out the current runtime dialect
        String compatibilityValue = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_PARAMETER, I_ServiceRunMode.DialectSpace.STANDARD.toString());
        dialectSpace =  I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue);
	}

    @Override
    public void parse(HttpServletRequest servletRequest) throws ServiceManagerException {
        String requestURI = servletRequest.getRequestURI();
        if (requestURI.charAt(0) != delimiter.toCharArray()[0])
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, "Badly formed query, invalid format, first char in path should be '/' in path:["+path+"]");

        requestURI = requestURI.substring(1);

        this.pathitems = requestURI.split(delimiter);
        this.path = requestURI;

        if (pathitems.length < 2)
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, "Badly formed query, invalid format, this should be at least /service/method in path:["+path+"]");

    }
	
	/**
	 * return the mandatory service
	 * @return
	 */
    @Override
	public String identifyService() throws ServiceManagerException {
		if (pathitems.length > 1)
			return pathitems[0];
		
		//throws an exception if no service
		throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, "No service in query, invalid format in :["+path+"]");
	}
	
	
	/**
	 * return the optional service resource
	 * @return resource
	 */
    @Override
	public String identifyResource(){
		if (pathitems.length > 2){
			StringBuffer res = new StringBuffer();
			for (int i = 1; i < pathitems.length - 1; i++) {
				res.append(pathitems[i]+"/");
			}
			//remove trailing '/'
			String ret = res.substring(0, res.length() - 1);
			return ret;
		}
		
		return null; //no resource defined
	}
	
	/**
	 * return the path (e.g. service+optional resource...)
	 * @return resource
	 */
    @Override
	public String identifyPath(){
		if (pathitems.length > 1){
			StringBuffer res = new StringBuffer();
			for (int i = 0; i < pathitems.length - 1; i++) {
				res.append(pathitems[i]+"/");
			}
			//remove trailing '/'
			String ret = res.substring(0, res.length() - 1);
			return ret;
		}
		else if (pathitems.length == 1)
			return pathitems[0];
		
		return null; //no resource defined
	}	
	
	/**
	 * return method in path
     * NB. if dialect is EHRSCAPE, method can be unspecified. It is substituted by "unknown" for compatibility purpose
	 * @return method
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException if no method found
	 */
    @Override
	public String identifyMethod() throws ServiceManagerException {
		
		String method = pathitems[pathitems.length - 1];
		
		if (method == null){
            if (dialectSpace.equals(I_ServiceRunMode.DialectSpace.EHRSCAPE))
                method = "unknown";
            else
			    throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, "PathParser", "No method in query, invalid format for::["+path+"]");
		}
		
		return method;
		
	}
}

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
package com.ethercis.vehr.response;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Response;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Abstract class common to all response classes<p>
 * This class is designed to handle seamlessly HttpServletResponse (synchronous) and ServletResponse (asynchronous)
 * since there are differences in the way headers can be dealt with logonservice well logonservice other attributes.
 * @author Pro7
 *
 */
public abstract class GenericHttpResponse {
	private String contentType;
	protected static Logger log = Logger.getLogger("ResponseOutput");
	private final String header = "<html><meta http-equiv='no-cache'><meta http-equiv='Cache-Control' content='no-cache'><meta http-equiv='expires' content='Wed, 26 Feb 1997 08:21:57 GMT'>";
	protected PrintWriter writer = null;
	protected Object response;
	
	public GenericHttpResponse(String contenttype, Object response) throws IOException{
		this.contentType = contenttype;
		this.response = response; //for additional settings if needed...
		
		if (response instanceof HttpServletResponse){
			((HttpServletResponse)response).setContentType(contentType);
			this.writer = ((HttpServletResponse)response).getWriter();

		}
		else if (response instanceof ServletResponse){
			((ServletResponse)response).setContentType(contentType);
			this.writer = ((ServletResponse)response).getWriter();
		}
        else if (response instanceof Response){
            log.info("Jetty server response...");
        }
	}
	
	/**
	 * sets an entry into the HTTP header<p>
	 * NB. This method has no effect for ServletResponse (Asynchronous service)
	 * please use the specific setter when dealing with asynchronous methods.
	 * 
	 * @param name
	 * @param value
	 */
	public void setHeader(String name, String value){
		if (response instanceof HttpServletResponse){
			((HttpServletResponse)response).setHeader(name, value);

		}
		else if (response instanceof ServletResponse){
//			((ServletResponse)response).setHeader(name, value);
		}		
	}
	
	/**
	 * sets the status into the HTTP header<p>
	 * NB. This method has no effect for ServletResponse (Asynchronous service)
	 * please use the specific setter when dealing with asynchronous methods.
	 * 
	 * @param astatus an HTTP status code
	 */
	
	public void setStatus(int astatus){
		if (response instanceof HttpServletResponse){
			((HttpServletResponse)response).setStatus(astatus);

		}
		else if (response instanceof ServletResponse){
//			((ServletResponse)response).;
		}			
	}
	
	/**
	 * sets the content length of the content (use standard HTTP key) 
	 * @param length
	 */
	public void setContentLength(int length){
		if (response instanceof HttpServletResponse){
			((HttpServletResponse)response).setHeader("Content-length", ""+length);

		}
		else if (response instanceof ServletResponse){
			((ServletResponse)response).setContentLength(length);
		}			
	}
	
	/**
	 * sets the no content tag in http header
	 */
	public void setNoContent(){
		if (response instanceof HttpServletResponse){
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_NO_CONTENT);

		}
		else if (response instanceof ServletResponse){
			((ServletResponse)response).setContentLength(0);
		}			
	}


}

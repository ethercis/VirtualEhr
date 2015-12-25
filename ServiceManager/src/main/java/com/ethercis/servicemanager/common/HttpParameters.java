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
import com.ethercis.servicemanager.jmx.SerializeHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

/**
 * this class handle http parameters (in a URL) passed logonservice a Map<String, String[]> and
 * builds a SessionClientProperties matching it
 * @author Pro7
 *
 */
public class HttpParameters {

	SessionClientProperties props;
	RunTimeSingleton global;
	
	public HttpParameters(RunTimeSingleton global){
		this.global = global;
		props = new SessionClientProperties(global);
	}
	

	/**
	 * build SessionClientProperties based on passed parameters
	 * @param parms
	 * @throws IOException 
	 */
	public void setParameters(Map<String, String[]> parms) throws IOException{
		for (String k: parms.keySet()){
			if (parms.get(k) == null) //null value
				props.addClientProperty(k, (String)null);
			else if (parms.get(k).length == 0) //no value -> empty string
				props.addClientProperty(k, "");
			else if (parms.get(k).length == 1) //single value
				props.addClientProperty(k, parms.get(k)[0]);
			else{
				//since we support arrays in properties (but pass it logonservice a BLOB[])
				ArrayList<String> arrlist = new ArrayList<String>();
				for (String s: parms.get(k))
					arrlist.add(s);
			    SerializeHelper helper = new SerializeHelper(global);
				byte[] serialized = helper.serializeObject(arrlist.toArray(new String[]{}));
			    
				props.addClientProperty(k, serialized); //add the array encoded Base64
			}
		}
	}
	
	/**
	 * return the set properties
	 * @return
	 */
	public SessionClientProperties getProperties(){
		return props;
	}
	
	/**
	 * factory. The parameters are the one from HttpServletRequest getParameterMap()
	 * @param glob
	 * @param parameters
	 * @return
	 * @throws IOException 
	 */
	public static HttpParameters getInstance(RunTimeSingleton glob, Map<String, String[]> parameters) throws IOException{
		if (parameters == null)
			return null;
		if (parameters.size() == 0){
			return new HttpParameters(glob);
		}
		
		HttpParameters parmsUtil = new HttpParameters(glob);
		
		parmsUtil.setParameters(parameters);
		
		return parmsUtil;
	}

	/**
	 * build a SessionClientProperties from the array passed in http header
	 * @param global
	 * @param req
	 * @return
	 */
	public static I_SessionClientProperties getInstanceFromHeader(RunTimeSingleton global, HttpServletRequest req) {
		SessionClientProperties sc = new SessionClientProperties(global);
		Enumeration<String> names = req.getHeaderNames();
		
		while (names.hasMoreElements()){
			String key = names.nextElement();
			String value = req.getHeader(key);
			sc.addClientProperty(key, value);
			
		}
		return sc;
		
	}
	
	
}

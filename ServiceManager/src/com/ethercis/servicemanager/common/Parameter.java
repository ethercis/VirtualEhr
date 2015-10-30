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

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.ethercis.servicemanager.common.security.I_Parameter;

public class Parameter implements I_Parameter {
	private String name;
	private String pattern;
	
	
	public Parameter() {
		super();
	}
	public Parameter(String name, String pattern) {
		super();
		this.name = name;
		this.pattern = pattern;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.common.common.I_Parameter#getUserId()
	 */
	@Override
	public String getName() {
		return name;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.common.common.I_Parameter#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.common.common.I_Parameter#getPattern()
	 */
	@Override
	public String getPattern() {
		return pattern;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.common.common.I_Parameter#setPattern(java.lang.String)
	 */
	@Override
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	@Override
	public String toString() {
		return toJsonString();
		//return "Parameter [name=" + name + ", pattern=" + pattern + "]";
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.common.common.I_Parameter#toJsonString()
	 */
	@Override
	public String toJsonString() {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("name", name);
			jsonObj.put("pattern", pattern);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonObj.toString();
	}
}

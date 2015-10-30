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
package com.ethercis.logonservice.session;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.SessionClientProperties;

import java.io.Serializable;

public class ResponseHolder implements Serializable {
	private static final long serialVersionUID = 2625302472610393847L;
	private String contentType;
	private Object data;
	private SessionClientProperties sessionClientProperties;
	private RunTimeSingleton glob;

	public ResponseHolder(String contentType, RunTimeSingleton glob) {
		this.glob = glob;
		this.contentType = contentType;
		sessionClientProperties = new SessionClientProperties(glob);

	}

	public String getContentType() {
		return contentType;
	}

	public SessionClientProperties getSessionClientProperties() {
		return sessionClientProperties;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}

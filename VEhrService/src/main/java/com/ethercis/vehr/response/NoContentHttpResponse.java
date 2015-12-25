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

import com.ethercis.logonservice.session.ResponseHolder;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import org.codehaus.jackson.JsonGenerationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * format a void response e.g. no content...
 * @author Christian Chevalley
 *
 */
public class NoContentHttpResponse extends GenericHttpResponse {

	public NoContentHttpResponse(Object response) throws IOException{
		super("application/json;charset=UTF-8", response);
	}
	
	public void respond(Object data, String path) throws IOException {
		//traverse the map of header fields and add it to the response header
		if (data instanceof Map) {
			Map<String, String> fieldMap = (Map)data;
			for (String field: fieldMap.keySet()) {
				String fieldValue = fieldMap.get(field);
				setHeader(field, fieldValue.replaceAll(Constants.URI_TAG, path));
			}
		}
		setNoContent();
	}
}

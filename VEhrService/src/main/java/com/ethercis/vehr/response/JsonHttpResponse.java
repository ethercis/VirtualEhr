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

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.ethercis.servicemanager.common.def.Constants;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * format a json response
 * @author Christian Chevalley
 *
 */
public class JsonHttpResponse extends GenericHttpResponse {
	
	public JsonHttpResponse(Object response) throws IOException{
		super("application/json;charset=UTF-8", response);
	}
	
	public void respond(Object data, String path) throws IOException {

		//do some substitutions
		if (data instanceof Map){
			Map contentMap = (Map)data;
			if (contentMap.containsKey("meta")){
				data = MetaBuilder.substituteVarMetaMap(contentMap, Constants.URI_TAG, path);
			}
			if (contentMap.containsKey("headers")){
				I_SessionClientProperties props = (I_SessionClientProperties)contentMap.get("headers");
				for (String s: props.clientProps2StringMap().keySet()){
					setHeader(s, props.getClientProperty(s, ""));
				}
				contentMap.remove("headers"); //do not serialize in body
			}
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDateFormat(new ISO8601DateFormat());


		String bodyContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        setContentLength(bodyContent.getBytes().length);
		writer.println(bodyContent);
		writer.close();
	}
}

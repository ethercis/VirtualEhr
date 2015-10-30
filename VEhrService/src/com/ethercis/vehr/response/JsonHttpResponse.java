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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * format a json response
 * @author Christian Chevalley
 *
 */
public class JsonHttpResponse extends GenericHttpResponse {
	
	public JsonHttpResponse(Object response) throws IOException{
		super("application/json;charset=UTF-8", response);
	}
	
	public void respond(Object data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        new org.codehaus.jackson.map.ObjectMapper().writer().writeValue(byteArrayOutputStream,data);
        String bodyContent = byteArrayOutputStream.toString();
//        Gson json = new GsonBuilder().setPrettyPrinting().create();
//        String bodyContent = json.toJson(data);
        setContentLength(bodyContent.getBytes().length);
		writer.println(bodyContent);
		writer.close();
	}
}

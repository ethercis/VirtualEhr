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

import java.io.IOException;

/**
 * format a void response e.g. no content...
 * @author Christian Chevalley
 *
 */
public class VoidHttpResponse extends GenericHttpResponse {
	
	public VoidHttpResponse(Object response) throws IOException{
		super("", response);
	}
	
	public void respond(ResponseHolder responseHolder){
        I_SessionClientProperties props =responseHolder.getSessionClientProperties();
        //set the header
        for (String s: props.clientProps2StringMap().keySet()){
            setHeader(s, props.getClientProperty(s, ""));
        }
		setNoContent();

		writer.println("");
		writer.close();
	}
}

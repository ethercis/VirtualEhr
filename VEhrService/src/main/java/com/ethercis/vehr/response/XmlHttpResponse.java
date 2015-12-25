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

import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * format an xml response
 * @author Christian Chevalley
 *
 */
public class XmlHttpResponse extends GenericHttpResponse {
	
	public XmlHttpResponse(Object response) throws IOException{
		super("application/xml;charset=UTF-8", response);
	}
	
	public void respond(Object data, String path){

		if (data instanceof String) {
			writer.println((String)data);
			writer.close();
		}
		else if (data instanceof Document){
			Document document = (Document)data;
			String xml = document.asXML().replaceAll(Constants.URI_TAG, path);
			//massaging...
			OutputFormat outputFormat = OutputFormat.createPrettyPrint();
			StringWriter stringWriter = new StringWriter();
			XMLWriter xmlWriter = new XMLWriter(stringWriter, outputFormat);
			try {
				Document document1 = DocumentHelper.parseText(xml);
				xmlWriter.write(document1);
			} catch (Exception e){
				throw new IllegalArgumentException("Could not encode content:"+e);
			}
			writer.println(stringWriter.toString());
			writer.close();
		}
	}
}

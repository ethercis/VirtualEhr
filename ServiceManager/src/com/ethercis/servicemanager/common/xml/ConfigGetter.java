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
package com.ethercis.servicemanager.common.xml;

import org.apache.xmlbeans.XmlObject;
/**
 * perform simple access to XML<p> 
 * @author christian
 *
 */
public class ConfigGetter extends ConfigHandler {
	
	public ConfigGetter(XmlObject root){
		super(root);
	}

	/**
	 * encode and perform an XQuery<p>
	 * for example:<p>
	 * <code>//ns:authenticate[@id='JOE']/ns:publicCredential/ns:realName</code><p>
	 * Please note that the namespace prefix is required!
	 * @param pathexp
	 * @return
	 */
	public XmlObject[] queryXPath(String pathexp){
		XmlObject[] retxml = docroot.selectPath(queryexp+pathexp);
		return retxml;
	}
}

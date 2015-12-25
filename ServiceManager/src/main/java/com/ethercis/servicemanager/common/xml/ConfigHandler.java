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
 * abstract class to share common attributes and methods between getter and setter
 * @author Christian Chevalley
 *
 */
public abstract class ConfigHandler {
	protected XmlObject docroot;
	protected String namespace;
	protected String queryexp;
	
	public ConfigHandler(XmlObject root){
		this.docroot = root;
		if (docroot != null){
			this.namespace = root.getDomNode().getNamespaceURI();
			if (namespace == null)
				this.queryexp = "$this/";
			else
				this.queryexp = "declare namespace ns = '"+namespace+"'; ";
		}
	}

	/**
	 * can be used to set a new doc root<p>
	 * @param root a document 
	 */
	public void setDocRoot(XmlObject root){
		this.docroot = root;
		if (docroot != null){
			this.namespace = root.getDomNode().getNamespaceURI();
			if (namespace == null)
				this.queryexp = "$this/";
			else
				this.queryexp = "declare namespace ns = '"+namespace+"'; ";
		}		
	}
}

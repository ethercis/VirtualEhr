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


import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper to parse a command line embedded in XML.
 * 
 * the parser supports one level of depth XML tree such logonservice:
 * <br><br>
 * &lt;TAG1&gt;<br>
 * 	&lt;ELT1 type='Type1'&gt;VAL1&lt;/ELT1&gt;<br>
 * 	&lt;PARM type='Type2'/&gt;PARMVAL1&lt;/PARM&gt;<br>
 * 	&lt;PARM type='Type3'/&gt;PARMVAL2&lt;/PARM&gt;<br>
 *  ...<br>
 * &lt;/TAG1&gt;<br>
 * <br><br>
 * This will translate logonservice:<br>
 * <br><br>
 * map('ELT1')[0].val->VAL1<br>
 * map('ELT1')[0].type->Type1<br>
 * <br> 
 * map('PARM')[0].val->PARMVAL1<br>
 * map('PARM')[0].type->Type2 <br>
 * map('PARM')[1].val->PARMVAL2<br>
 * map('PARM')[1].type->Type3 <br>
 * <br><br>
 * The internal representation is:<br>
 * <br><br>
 * HASH(elt-name, LIST (HASH(node-type, object)))<br>
 * <br><br>
 * Where the HASH(node-type, object) is one of:<br>
 * <br>
 * HASH(ATTRIBUTE, LIST(HASH(attribute-name, attribute-value)))<br>
 * HASH(VALUE, String)<br>
 * <br><br>
 * @author christian
 *
 */
public class CommandParser extends DefaultHandler
{
    /** Default parser name (dom.wrappers.Xerces). */
    protected static final String DEFAULT_PARSER_NAME = "dom.wrappers.Xerces";
    DocumentBuilderFactory docfactory;
    Document domdoc;
	/**
	 * public constructor
	 *
	 */
	public CommandParser(){
        // create parser
        try {
            docfactory = DocumentBuilderFactory.newInstance();
            docfactory.setCoalescing(true);
        }
        catch (Exception e) {
            System.err.println("error: Unable to instantiate parser ("+DEFAULT_PARSER_NAME+")");
        }		
	}
	
	/**
	 * parse an XML string
	 * @param str
	 */
	public void parse(String str){
		try {
			str = str.replaceAll("[\n\t]","");
			str = str.trim();
			DocumentBuilder builder = docfactory.newDocumentBuilder();
			ByteArrayInputStream ins = new ByteArrayInputStream(str.getBytes());
			domdoc = builder.parse(ins);
			if (domdoc == null){
				System.err.println("Could not parse document: "+str);				
			}
		}
		catch (Exception e){
			System.err.println("Could not parse string: "+e);
		}
	}
	/**
	 * get the value of the 'index' occurence of an element
	 * @param eltid the string identifying the element
	 * @param index the occurence index (0 based)
	 * @return a string value
	 */
	public String getElementValue(String eltid, int index){
		NodeList nl = domdoc.getElementsByTagName(eltid);
		if (nl == null)
			return "";
		Node n = nl.item(index);
		if (n != null && n.hasChildNodes()){
			return n.getFirstChild().getNodeValue();
		}
		else
			return "";
	}
	
	/**
	 * return the String content of this element and all its descendants
	 * @param eltid the string identifying the element
	 * @param index the occurence index (0 based)
	 * @return a string value
	 */
	public String getTextContent(String eltid, int index){
		NodeList nl = domdoc.getElementsByTagName(eltid);
		if (nl == null)
			return "";
		Node n = nl.item(index);
		if (n != null){
			return n.toString();
		}
		else
			return null;		
	}
	/**
	 * get the list of attributes for the 'index' occurence of element "eltid"
	 * @param eltid the string identifying the element
	 * @param index the occurence index (0 based)
	 * @return an object pointer to the list
	 */
	public Object getElementAttribute(String eltid, int index){
		NodeList nl = domdoc.getElementsByTagName(eltid);
		Node n = nl.item(index);
		if (n != null && n.hasAttributes()){
			return n.getAttributes();
		}
		return null;
	}
	/**
	 * gets the number of occurences of a given element
	 * @param eltid the string identifying the element
	 * @return an integer giving the size of the list
	 */
	public int getOccurences(String eltid){
		NodeList nl = domdoc.getElementsByTagName(eltid);
		return nl.getLength();
	}
	/**
	 * returns the length of the list of attributes for an element
	 * @param eltid the string identifying the element
	 * @return the size of the list
	 */
	public int getAttrLength(String eltid){
		return 0;
	}
	/**
	 * returns the value of an attribute identified by a string for a given element
	 * @param attributes an object pointing to the attribute list
	 * @param attrid the attribute to look for
	 * @return the attribute value
	 */
	public String getAttributeValue(Object attributes, String attrid){
		NamedNodeMap attmap = (NamedNodeMap)attributes;
		Node attribute = attmap.getNamedItem(attrid);
		if (attribute != null)
			return attribute.getNodeValue();
		return "";
	}
	/**
	 * convenience returning the value of an attribute for a given element occurence
	 * @param eltid the element owning the attribute
	 * @param eltindex the occurence of the element
	 * @param attrid the id of the attribute
	 * @return the value of the attribute
	 */
	public String getIndexedAttrVal(String eltid, int eltindex, String attrid){
		return "";
	}
	/**
	 * returns the string value of an attribute for an element (assuming the element is unique)
	 * @param eltid the element id
	 * @param attrid the attribute id
	 * @return the attribute value
	 */
	public String getAttributeValue(String eltid, String attrid){
		return "";
	}

	/**
	 * Returns the 'index' value of an attribute for a given element
	 * @param eltid the element id
	 * @param index the index of the attribute
	 * @return the value of the attribute
	 */
	public String getAttributeValue(String eltid, int index){
		return "";		
	}

	/**
	 * Returns the 'index' name of an attribute for a given element
	 * @param eltid the element id
	 * @param index the index of the attribute
	 * @return the name of the attribute
	 */
	public String getAttributeName(String eltid, int index){
		return "";		
	}
	
	/**
	 * dump the mapper for debugging purpose
	 * @return the string representation for debugging
	 */
	public String dump(){
		return domdoc.toString();
	}
	
	public Map<String,List<String>> elements2map(){
		Map<String,List<String>> m = new HashMap<String,List<String>>();
		NodeList nl = domdoc.getElementsByTagName("*");
		for (int i = 0; i < nl.getLength(); i++){
			Node n = nl.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE){
				String name = n.getNodeName();
				List<String> vl = null;
				if (!m.containsKey(name)){
					//allocate a new list of values
					vl = new LinkedList<String>();
					String value = getElementValue(n.getNodeName(), 0);
					vl.add(value);
					m.put(n.getNodeName(), vl);
				}
				else { //retrieve the value list and add the value to it
					vl = m.get(n.getNodeName());
					String value = getElementValue(n.getNodeName(), vl.size());
					vl.add(value);
				}
			}
		}
		return m;
	}
	


	public static Map setClientProperties(String ts, byte[] bytes) {
		// TODO Auto-generated method stub
		return null;
	}

}

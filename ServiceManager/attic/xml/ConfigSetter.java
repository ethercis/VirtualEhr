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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlIDREF;
import org.apache.xmlbeans.XmlIDREFS;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlTime;

import com.ethercis.servicemanager.common.ClientProperty;

/**
 * perform write operations on config XML<p>
 * This class implements methods required to create, insert, delete and set (update)
 * elements and attributes.
 * @author christian
 *
 */
public class ConfigSetter extends ConfigHandler {
	private Exception lastException;
	private static final String NEWID_TAG = "$id";
	private static Logger log = LogManager.getLogger(ConfigSetter.class);
	
	public ConfigSetter(XmlObject root){
		super(root);
	}
	
	/**
	 * copy all attributes in a element header<p>
	 * this method has been implemented logonservice I could not find another way to directly copy the element with its
	 * attributes to a new location. Probably, because copyXmlContents is used instead of copyXml,
	 * however, copyXml did not provide the expected result...
	 * @param origin
	 * @param dest
	 */
	private void copyAttributes(XmlCursor origin, XmlCursor dest){
		dest.toNextToken();
		
		origin.toStartDoc();
		int attrcnt = 0;
		
		while(attrcnt < 2 && origin.hasNextToken()){
			if (origin.isAttr()){
				String attrname = origin.getName().getLocalPart();
				String attrval  = origin.getTextValue();
				
				if (attrval.compareTo(NEWID_TAG)==0){
					XmlObject[] o = docroot.selectPath(queryexp+"generate-id(.)");
					attrval = o[0].newCursor().getTextValue();
				}
				
//				System.out.println("ATTR:"+parsedloc.getUserId()+"="+parsedloc.getTextValue());
				dest.insertAttributeWithValue(attrname, attrval);
			}
//			else if (t.isText())
//				System.out.println("TXT:"+parsedloc.getTextValue());
			else if (origin.isStart() || origin.isEnd() || origin.isEnddoc()){
				attrcnt++;
//				System.out.println("START");
			}
//			else
//				System.out.println("TOK:"+t.toString());
			origin.toNextToken();
		}
				
	}

	/**
	 * Build a new entity and insert it into the docroot
	 * @param xmlstr
	 * @return
	 */
	public XmlObject[] newConfigObject(String xmlstr){
		XmlCursor parsedloc = null;
		XmlCursor orgloc = null;
		
		try {
			XmlObject parsed = XmlObject.Factory.parse(xmlstr);
			
			parsedloc = parsed.newCursor();
			parsedloc.toNextToken();			
			String classname = parsedloc.getName().getLocalPart();
			classname = capitalize(classname);
			//create a new place holder of class 'classname', the new instance method is like 'addNewInput'
			String instmkStr = "addNew"+classname;
			XmlCursor childloc = docroot.newCursor();
			childloc.toFirstChild();
			XmlObject firstchild = childloc.getObject(); 
			
			Class<?> childclass = firstchild.getClass();
			Method newinst = childclass.getDeclaredMethod(instmkStr,(Class[])null);
			XmlObject newobj = (XmlObject)newinst.invoke(firstchild, (Object[])null);
			orgloc = newobj.newCursor();
			
			orgloc.toNextToken();
			parsedloc.copyXmlContents(orgloc);
			
			orgloc.dispose();
			orgloc = newobj.newCursor();
			copyAttributes(parsedloc, orgloc);
			parsedloc.dispose();
			orgloc.dispose();
			return new XmlObject[]{newobj};
		} catch (Exception e){
			lastException = e;
			log.error("NewConfigObject exception:",e);
			return null;
		}
		finally {
			if (parsedloc != null)
				parsedloc.dispose();
			if (orgloc != null)
				orgloc.dispose();
		}
	}


	/**
	 * check if an idref exists
	 * @param aref the idref to check 
	 * @return true if exists, false otherwise
	 */
	private boolean checkIntegrity(String aref){
		//get the reflist
		String pathexp = "//*[@id='"+aref+"']";
		XmlObject[] xoarr = docroot.selectPath(queryexp+pathexp);
		if (xoarr.length == 0)
			return false;
		return true;
	}
	/**
	 * Set an element located at a path with an value<p>
	 * The path can return a list of elements to set, in this case all matching elements
	 * are set.
	 * @param pathexp the XPath expression to get the element
	 * @param value an Object compatible with the value to set
	 * @param withIntegrity - true if referential integrity is to be checked
	 * @return true if success, false if refential integrity is compromised, or path return null
	 */
	public XmlObject[] setElement(String pathexp, Object value, boolean withIntegrity){
		XmlObject[] xoarr = docroot.selectPath(queryexp+pathexp);
		if (xoarr.length == 0) log.info("setElement, XPath query returned null:"+queryexp+pathexp);
		List<XmlObject> xlist = new ArrayList<XmlObject>();
		
		if (xoarr == null) return null;
		
		if (xoarr.length == 0)
			return forceSetElement(pathexp, value);
		
		for (XmlObject o: xoarr){
			XmlCursor co = o.newCursor();
			if (o instanceof XmlTime)
				co.setTextValue(((XmlTime)value).getStringValue());
			else if (withIntegrity && o instanceof XmlIDREF){
				if (!checkIntegrity(value.toString())){
					continue;
				}
				co.setTextValue(value.toString());
				
			}
			else if (withIntegrity && o instanceof XmlIDREFS){
				for (String aref: value.toString().split(" "))
					if (!checkIntegrity(aref))
						continue;
				co.setTextValue(value.toString());
			}
			else
				co.setTextValue(value.toString());
			xlist.add(o);
		}
		
		return xlist.toArray(new XmlObject[0]);
	}	
	/**
	 * set an element with integrity checking<p>
	 * @param pathexp the XPath expression to get the element
	 * @param value an Object compatible with the value to set
	 * @return true if success, false if refential integrity is compromised, or path return null
	 */
	public XmlObject[] setElement(String pathexp, Object val){
		return setElement(pathexp, val, true);
	}

	/**
	 * set the value of the last element matching an XPath query<p>
	 * The method extracts the last element from a simple query and tries to set its value
	 * @param pathexp
	 * @param value
	 * @return
	 */
	public XmlObject[] forceSetElement(String pathexp, Object value){
		int last = pathexp.lastIndexOf("/");
		String upper = pathexp.substring(0,last);
		String elem = pathexp.substring(last+1);
		return setSimpleValue(upper, elem, value);
	}

	/**
	 * set an element/attribute value<p> 
	 * @param o - the object to set
	 * @param tag - the tag of the object 
	 * @param value - the value logonservice an object
	 * @return the object with set new value
	 */
	XmlObject setSimpleObjectValue(XmlObject o, String tag, Object value){
		//get the method for this tag
		try {
			SchemaType st = o.schemaType();
			
			SchemaProperty ste;
			if (tag.charAt(0)=='@'){
				tag = tag.substring(1);
				ste = st.getAttributeProperty(new QName(tag));
			}
			else
				ste = st.getElementProperty(new QName(namespace,tag));
			
			SchemaType otype = ste.getType();
			Class<?> valclazz = otype.getJavaClass();
			Class<?> clazz = o.getClass();
			Method m = clazz.getDeclaredMethod("xset"+capitalize(tag), valclazz);
			//invoke the method if any
			//find out the factory for this XmlType:
			Class<?> cfact = Class.forName(valclazz.getName()+"$Factory");
			Method factory = cfact.getDeclaredMethod("newValue", new Class[]{Object.class});
			Object xval = factory.invoke(null, new Object[]{value});
			m.invoke(o,xval);
			return o;
		}
		catch (NoSuchMethodException e){
			lastException = e;
			return null;
		} catch (IllegalAccessException e){
			lastException = e;
			return null;
		} catch (InvocationTargetException e){
			lastException = e;
			return null;
		} catch (ClassNotFoundException e){
			lastException = e;
			return null;
		}
		
	}
	/**
	 * set a value to element/attribute(s) selected from an XPath query<p>
	 * @param pathexp 
	 * @param tagname
	 * @param value
	 * @return an array of objects with set value
	 */
	public XmlObject[] setSimpleValue(String pathexp, String tagname, Object value){
		XmlObject[] xoarr = docroot.selectPath(queryexp+pathexp);
		if (xoarr.length == 0) log.info("setElement, XPath query returned null:"+queryexp+pathexp);
		List<XmlObject> xlist = new ArrayList<XmlObject>();
		
		if (xoarr == null) return null;

		for (XmlObject o: xoarr){
			XmlObject xo = setSimpleObjectValue(o, tagname, value);
			if (xo != null)
				xlist.add(xo);
			else
				return null;
		}
		return xlist.toArray(new XmlObject[0]);
	}
	/**
	 * insert a new object instance<p>
	 * @param pos location where to insert the new object
	 * @param classname - the class name of the object to insert (generally a tag name)
	 * @return the new object or null if error
	 */
	private XmlObject addNewInstance(XmlObject pos, String classname){

		classname = capitalize(classname);
		String instmkStr = "addNew"+classname;
		
		try {
			Method mins = pos.getClass().getDeclaredMethod(instmkStr, (Class[])null);
			XmlObject newobj = (XmlObject)mins.invoke(pos, (Object[])null);
			return newobj;
		} catch (NoSuchMethodException e){
			lastException = e;
		} catch (IllegalAccessException e){
			lastException = e;
		} catch (InvocationTargetException e){
			lastException = e;
		}

		return null;
	}
	
	
	/**
	 * insert a new element located at a given path<p>
	 * The path can return a list of elements to set, in this case all matching elements
	 * get a new element to be inserted.<br>
	 * The method uses either an addNewXYZ scheme or try to set a simple value if this fails
	 * @param pathexp - the select filter
	 * @param xmlval - the value to insert
	 * @return an array of object with inserted values or null if error
	 */
	public XmlObject[] insertElement(String pathexp, String xmlval){
		List<XmlObject> xlist = new ArrayList<XmlObject>();
		XmlCursor parsedloc = null;
		XmlCursor toc = null;
		try {
			XmlObject newelm = XmlObject.Factory.parse(xmlval);
			parsedloc = newelm.newCursor();
			parsedloc.toNextToken();			
			String tagname = parsedloc.getName().getLocalPart();
			
			//point to the node where to do the insertion
			XmlObject[] xoarr = docroot.selectPath(queryexp+pathexp);
			if (xoarr.length == 0) log.info("insertElement, XPath query returned null:"+queryexp+pathexp);
			for (XmlObject xo : xoarr) {
				XmlObject newobj = addNewInstance(xo, tagname);

				//addNewXYZ failed so insert the xml sequence below the selected object
				if (newobj == null){
					//try a set element
					XmlObject[] xnews = setSimpleValue(pathexp, tagname, parsedloc.getTextValue());
					xlist.addAll(Arrays.asList(xnews));
				}
				else {
					toc = newobj.newCursor();
					toc.toNextToken();
					parsedloc.copyXmlContents(toc);
					toc.dispose();
					toc = newobj.newCursor();
					
					copyAttributes(parsedloc, toc);

					xlist.add(xo);
					toc.dispose();
				}
			}
			parsedloc.dispose();
			return xlist.toArray(new XmlObject[0]);
		}
		catch (XmlException e){
			lastException = e;
			return null;
		} finally {
			if (parsedloc != null)
				parsedloc.dispose();
			if (toc != null)
				toc.dispose();
		}
	}
	/**
	 * Delete an element at a given path<p>
	 * The path can return a list of elements to set, in this case all matching elements
	 * are removed. 
	 * @param pathexp 
	 * @return
	 */
	public XmlObject[] deleteElement(String pathexp){
		XmlObject[] xoarr = docroot.selectPath(queryexp+pathexp);
		if (xoarr.length == 0) log.info("deleteElement, XPath query returned null:"+queryexp+pathexp);
		List<XmlObject> xlist = new ArrayList<XmlObject>();

		if (xoarr == null) return null;
		
		for (XmlObject o: xoarr){
			
			XmlCursor co = o.newCursor();
			co.removeXml();
			co.dispose();
			xlist.add(o);
//			co.removeXmlContents();			
		}
		return xlist.toArray(new XmlObject[0]);
	}
	/**
	 * return the last set exception<p>
	 * @return
	 */
	public Exception getLastException(){
		return lastException;
	}
	
	public static String getPath(Map props){
		ClientProperty method = (ClientProperty)props.get("path");
		if (method == null)
			return null;
		return method.getStringValue();
	}
	

	public static String getValue(Map props){
		ClientProperty cpval = (ClientProperty)props.get("value");
		if (cpval == null)
			return null;
		return cpval.getStringValue();
	}

	private static String capitalize(String s){
		return s.substring(0,1).toUpperCase() + s.substring(1);
	}
	
}

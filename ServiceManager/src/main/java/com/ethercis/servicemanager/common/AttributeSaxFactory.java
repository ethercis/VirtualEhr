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

/**
 * Project: EtherCIS system application
 * 
 * @author <a href="mailto:christian@adoc.co.th">Christian Chevalley</a>
 * @author <a href="mailto:michele@laghi.eu">Michele Laghi</a>
 * @author <a href="mailto:xmlblast@marcelruff.info">Marcel Ruff</a>
 */


package com.ethercis.servicemanager.common;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;


/**
 * This class parses an xml string to generate an Attribute which can be used either in PluginConfig or on the 
 * normal properties server- or clientside.
 * <p>
 * <pre>
 *  &lt;action do='LOAD'
 *             onStartupRunlevel='3'
 *             sequence='5'
 *             onFail='RESOURCE_CONFIGURATION_PLUGINFAILED'/>
 * </pre>
 */
public class AttributeSaxFactory extends SaxHandlerBase
{
   private String ME = "AttributeSaxFactory";
   private final RunTimeSingleton glob;
   private static Logger log = Logger.getLogger(AttributeSaxFactory.class);

   private ServiceManagerException ex;

   private I_AttributeUser attributeUser;
   private String attributeKey;
   private StringBuffer attributeValue;
   private boolean inAttribute = false;
   private boolean wrappedInCDATA = false; // for example: <attribute id='publishQos'><![CDATA[ bla ]]></attribute>
   private boolean embeddedCDATA = false;  // for example: <attribute id='publishQos'><qos><![CDATA[<expiration lifeTime='4000'/>]]></qos></attribute>
   private int subTagCounter;

   /**
    * Can be used logonservice singleton.
    */
   public AttributeSaxFactory(RunTimeSingleton glob, I_AttributeUser attributeUser) {
      super(glob);
      setUseLexicalHandler(true); // to allow CDATA wrapped attributes 
      this.glob = glob;
      this.attributeUser = attributeUser;
   }

   /**
    * resets the factory (to be invoked before parsing)
    */
   public void reset(I_AttributeUser attributeUser) {
      this.attributeUser = attributeUser;
      this.ex = null; // reset the exeptions
      this.wrappedInCDATA = false;
   }

   public boolean isInAttribute() {
      return this.inAttribute;
   }
   
   /**
    * Start element, event from SAX parser.
    * <p />
    * @param name Tag name
    * @param attrs the attributes of the tag
    */
   public final void startElement(String uri, String localName, String name, Attributes attrs) {
      if (this.ex != null ) return;

      if ("attribute".equalsIgnoreCase(name)) {
         this.inAttribute = true;
         this.wrappedInCDATA = false;
         this.attributeKey = attrs.getValue("id");
         this.attributeValue = new StringBuffer(1024);
         if (this.attributeKey == null || this.attributeKey.length() < 1)
            this.ex = new ServiceManagerException(this.glob, SysErrorCode.RESOURCE_CONFIGURATION, ME + ".startElement",  "the attributes in the <plugin> tag must have an non-empty 'id' attribute");
         return;
      }

      if (this.inAttribute) {
         this.subTagCounter++;
         this.attributeValue.append("<").append(name);
         for (int i=0; i<attrs.getLength(); i++) {
            String qName = attrs.getQName(i);
            String val = attrs.getValue(i);
            this.attributeValue.append(" ").append(qName).append("='").append(val).append("'");
         }
         this.attributeValue.append(">");
      }
      else {
         log.warn("startElement: unknown tag '" + name + "'");
      }
   }

   public void startCDATA() {
     log.debug("startCDATA");
      this.wrappedInCDATA = true;
      if (this.subTagCounter > 0) {
         this.attributeValue.append("<![CDATA[");
         this.embeddedCDATA = true;
      }
   }

   /**
    * The characters to be filled
    */
   public void characters(char[] ch, int start, int length) {
      if (this.attributeValue != null)
         this.attributeValue.append(ch, start, length);
   }


   /**
    * End element, event from SAX parser.
    * <p />
    * @param name Tag name
    */
   public void endElement(String uri, String localName, String name) {
      if (this.ex != null ) return;
      if ("attribute".equalsIgnoreCase(name)) {
         this.inAttribute = false;
         this.subTagCounter = 0;
         if (this.attributeKey != null && this.attributeValue != null) {
            String val = this.attributeValue.toString();

            /*if (val.startsWith("<![CDATA[")) {*/  //if (this.wrappedInCDATA) {
            if (false) { // currently bypassed since we don't want to strip &lt;![CDATA[ 
               // Strip CDATA if ampersand '&lt;![CDATA[' was used instead of '<![CDATA[':
               int pos = val.indexOf("<![CDATA[");
               if (pos >= 0) {
                  val = val.substring(pos+9);
               }
               pos = val.lastIndexOf("]]>");
               if (pos >= 0) {
                  val = val.substring(0, pos);
               }
            }
            if (this.attributeUser != null) {
               this.attributeUser.addAttribute(this.attributeKey, val);
               if (this.wrappedInCDATA) {
                  this.attributeUser.wrapAttributeInCDATA(this.attributeKey);
                  this.wrappedInCDATA = false;
               }
            }
         }
         this.attributeKey = null;
         this.attributeValue = null;
         return;
      }   
      if (this.inAttribute) {
         if (this.embeddedCDATA) {
            this.attributeValue.append("]]>");
            this.embeddedCDATA = false;
         }
         this.attributeValue.append("</"+name+">");
         this.subTagCounter--;
      }
   }

   /**
    * A human readable name of this factory
    * @return "AttributeSaxFactory"
    */
   public String getName() {
      return "AttributeSaxFactory";
   }
}


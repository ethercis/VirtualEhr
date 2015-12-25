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

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

/**
 * Factory for JAXP factories.
 *
 * <p>Use this factory when you need to localy override the System default settings for the JAXP parser and transformer factories.</p>
 *
 * @author Peter Antman
 */
public class JAXPFactory {

   private final static JAXPFactory factory = new JAXPFactory();

   /**
    * Use the default SAXParserFactory.
    */
   public static SAXParserFactory newSAXParserFactory()
      throws FactoryConfigurationError{
      return SAXParserFactory.newInstance();
   }

   /**
    * Use the SAXParserFactory class specifyed.
    */
   public static SAXParserFactory newSAXParserFactory(String factoryName)
      throws FactoryConfigurationError {
      if (factoryName == null || factoryName.length() < 1) {
         return newSAXParserFactory();
      }
      try {
         SAXParserFactory spf = (SAXParserFactory) factory.getClass().getClassLoader().loadClass(factoryName).newInstance();
         return  spf;
      } catch (Exception e) {
         throw new FactoryConfigurationError(e,e.getMessage());
      } // end of try-catch
   }
   
   /**
    * Use the default DocumentBuilderFactory.
    */
   public static DocumentBuilderFactory newDocumentBuilderFactory()
      throws FactoryConfigurationError {
      return DocumentBuilderFactory.newInstance();
   }

   /**
    * Use the DocumentBuilderFactory class specifyed.
    */
   public static DocumentBuilderFactory newDocumentBuilderFactory(String factoryName)
      throws FactoryConfigurationError {
      if (factoryName == null || factoryName.length() < 1) {
         return newDocumentBuilderFactory();
      }
      try {
         DocumentBuilderFactory dbf = (DocumentBuilderFactory) factory.getClass().getClassLoader().loadClass(factoryName).newInstance();
         return dbf;
      } catch (Exception e) {
         throw new FactoryConfigurationError(e,e.getMessage());
      }
   }

   /**
    * Use the default TransformerFactory.
    */
   public static TransformerFactory newTransformerFactory()
      throws TransformerFactoryConfigurationError {
      return TransformerFactory.newInstance();
   }

   /**
    * Use the TransformerFactory class specifyed.
    */
   public static TransformerFactory newTransformerFactory(String factoryName)
      throws TransformerFactoryConfigurationError {
      if (factoryName == null || factoryName.length() < 1) {
         return newTransformerFactory();
      }
      try {
         TransformerFactory tf = (TransformerFactory) factory.getClass().getClassLoader().loadClass(factoryName).newInstance();
         return tf;
      } catch (Exception e) {
         throw new TransformerFactoryConfigurationError(e,e.getMessage());
      } // end of try-catch
   }
}

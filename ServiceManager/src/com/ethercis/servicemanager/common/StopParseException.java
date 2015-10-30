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

import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Throw this exception to stop SAX parsing. 
 * <p />
 * Usually thrown in startElement() or endElement() if
 * you are not interested in the following tags anymore.<br />
 * Note that this exception extends RuntimeException,
 * so we don't need to declare it with a throws clause.
 */
public class StopParseException extends RuntimeException
{
   private static final long serialVersionUID = -8413175809990498728L;
   ServiceManagerException e;
   /**
    * Use this constructor to stop parsing when you are done. 
    */
   public StopParseException() {}

   /**
    * Use this constructor to stop parsing when an exception occurred. 
    * The ServiceManagerException is transported embedded in this class
    */
   public StopParseException(ServiceManagerException e) {
      this.e = e;
   }

   public boolean hasError() {
      return this.e != null;
   }

   public ServiceManagerException getServiceManagerException() {
      return this.e;
   }
}

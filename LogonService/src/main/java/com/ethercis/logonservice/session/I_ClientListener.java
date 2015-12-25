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

//Copyright
package com.ethercis.logonservice.session;

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Listens on sessionRemoved and sessionAdded events.
 * <p>
 * The events are fired by the Authenticate object.
 *
 * @version $Revision: 1.5 $
 * @author $Author: ruff $
 */
public interface I_ClientListener extends java.util.EventListener {
    /**
     * Invoked on successful client login
     */
    public void sessionAdded(ClientEvent e) throws ServiceManagerException;

    /**
     * Invoked on successful client login
     */
    public void sessionUpdated(ClientEvent e) throws ServiceManagerException;

    /**
     * Invoked on first successful client login, when SubjectInfo is created
     */
    public void subjectAdded(ClientEvent e) throws ServiceManagerException;

   /**
    * Invoked before a client does a logout
    */
   public void sessionPreRemoved(ClientEvent e) throws ServiceManagerException;

   /**
    * Invoked when client does a logout
    */
   public void sessionRemoved(ClientEvent e) throws ServiceManagerException;

   /**
    * Invoked when client does its last logout
    */
   public void subjectRemoved(ClientEvent e) throws ServiceManagerException;
       
}

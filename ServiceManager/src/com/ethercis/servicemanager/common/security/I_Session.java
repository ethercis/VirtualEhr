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
package com.ethercis.servicemanager.common.security;

import java.util.Map;


import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.common.session.I_SecurityProperties;
import com.ethercis.servicemanager.common.session.I_SessionHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * @author  W. Kleinertz
 */

public interface I_Session extends I_MsgSecurityInterceptor {

	
	/**
	 * Initialize the session with useful information. 
	 * <p> 
	 * Is called before {@link #initializeSession(I_SecurityProperties)} which does the authentication
	 * @param connectProperties The current login information
	 * @param map Additional information, is currently null
	 * @return the connectProperties we got, can be manipulated
	 */
	public I_SecurityProperties init(I_ConnectProperties connectprops, Map<?, ?> map) throws ServiceManagerException;

   /**
    * Initialize a new session and do the credential check. 
    */
   public I_SecurityProperties initializeSession(I_SecurityProperties securityprops) throws ServiceManagerException;

   /**
    * Allows to check the given securityQos again.
    * <p>
    * Note:
    * </p>
    * <ul>
    *   <li>This call does not modify anything in the I_Session implementation.</li>
    *   <li>The initializeSession() method must have been invoked before, otherwise we return false</li>
    * </ul>
    * @param String The already parsed QoS. The meaning will be defined by the real implementation.
    * @return true If the credentials are OK<br />
    *         false If access is denied
    */
   public boolean verify(I_SecurityProperties securityprops) throws ServiceManagerException;

   /**
    * Get the owner of this session.
    * <p/>
    * @param I_Subject The owner.
    */
   public I_Authenticate getAuthenticate();

   /**
    * How controls this session?
    * <p/>
    * @return I_Manager
    */
   public I_Manager getManager();

   /**
    * The current implementation of the user session handling 
    */
    // @deprecated
   public void changeSecretSessionId(String sessionId) throws ServiceManagerException;

   /**
    * Return the id of this session.
    * <p>
    * @param String The sessionId.
    */
   public String getSecretSessionId();

   /**
    * Check if this authenticate instance is permitted to do something
    */
   public boolean isAuthorized(I_SessionHolder sessionHolder, I_ContextHolder dataHolder);
   
   /**
    * If an exception occurs after successful authorization
    * the security framework has the chance to suppress the exception
    * by returning a return QOS
    * <p>
    * A dummy implementation should always return null!
    * <p>
    * A dead message can be produced like this:
    * <pre>
    * SessionInfo sessionInfo = sessionHolder.getSessionInfo();
	*	try {
	*		return sessionInfo.getMsgErrorHandler().handleErrorSync(new MsgErrorInfo(glob, sessionInfo.getSessionName(), dataHolder.getMsgUnit(), throwable));
    *	} catch (XmlBlasterException e) {
	*		e.printStackTrace();
	*		return null;
	*	}
    * </pre>
    * @param sessionHolder
    * @param dataHolder
    * @param throwable
    * @return if null, this call has no influence, usually the exception is thrown back to the client. 
    * if not null the string is returned to the client. Can be useful for dumb clients which don't know what to do with the exception.
    * In this case the security framework should handle the message itself, e.g. send it logonservice dead message or forward it to another place.
    */
   public String interceptExeptionByAuthorizer(Throwable throwable, I_SessionHolder sessionHolder, I_ContextHolder dataHolder);

}

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
package com.ethercis.servicemanager.common.session;

import java.util.Map;

import com.ethercis.servicemanager.common.security.I_Manager;
import com.ethercis.servicemanager.common.security.I_MsgSecurityInterceptor;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * @author  W. Kleinertz
 */

public interface I_Session extends I_MsgSecurityInterceptor {

   /*
    * Initialize a new session. 
    * <br \>
    * E.g.: An implementation could include authentication etc.
    * <p/>
    * @param String A qos-literal. The meaning will be defined by the real implementation.
    * @return String Like the securityQos param, but the other direction.
    * @exception ServiceManagerException The initialization failed (key exchange, authentication ... failed)
    * @deprecated This is never called, now #initializeSession(I_SecurityQos) is called
    */
   //public String initializeSession(String securityQos) throws ServiceManagerException;
	
	/**
	 * Initialize the session with useful information. 
	 * <p> 
	 * Is called before {@link #init(I_SecurityProperties)} which does the authentication
	 * @param connectQos The current login information
	 * @param map Additional information, is currently null
	 * @return the connectQos we got, can be manipulated
	 */
	public I_SessionProperties init(I_SessionProperties connectprops, Map<?, ?> map) throws ServiceManagerException;

   /**
    * Initialize a new session and do the credential check. 
    * <br \>
    * E.g.: An implementation could include authentication etc.
    * <p/>
    * @param String The already parsed QoS. The meaning will be defined by the real implementation.
    * @return String Like the securityQos param, but the other direction.
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException The initialization failed (key exchange, authentication ... failed)
    * @see #init(String)
    */
   public String init(I_SecurityProperties securityprops) throws ServiceManagerException;

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
   public I_Authenticate getSubject();

   /**
    * How controls this session?
    * <p/>
    * @return I_Manager
    */
   public I_Manager getManager();

   /**
    * The current implementation of the user session handling (especially
    * {@link SessionManager#connect(org.xmlBlaster.engine.qos.ConnectQosServer, String)})
    * cannot provide a real sessionId when this object is created. Thus, it
    * uses a temporary id first and changes it to the real in a later step.<p>
    * The purpose of this method is to enable this functionality.<p>
    *
    * @param String The new sessionId.
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown if the new sessionId is already in use.
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
    * <p/>
    * @param sessionHolder Holding information about the authenticate which requires rights
    * @param dataHolder Holding information about the data which shall be accessed
    *
    * EXAMPLE:
    *    isAuthorized("publish", "thisIsAMessageKey");
    *
    * The above line checks if this authenticate is permitted to >>publish<<
    * a message under the key >>thisIsAMessageKey<<
    *
    * Known action keys:
    *    publish, subscribe, get, erase, ...
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
    *	} catch (ServiceManagerException e) {
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

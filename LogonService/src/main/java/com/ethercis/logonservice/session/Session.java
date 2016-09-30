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
//Copyright
package com.ethercis.logonservice.session;

import com.ethercis.authenticate.Authenticate;
import com.ethercis.logonservice.security.SecurityProperties;
import com.ethercis.logonservice.security.ServiceSecurityManager;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.AuthErrorCode;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Manager;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.session.*;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.Map;
/**
 * Session directly manages user authentication and check for authorization<p>
 * @author christian
 *
 */
public class Session implements I_Session {
	private final RunTimeSingleton runTimeSingleton;
	private static Logger log = LogManager.getLogger(Constants.LOGGER_SECURITY);
	private String ME = Session.class.getName();
	protected ServiceSecurityManager securityManager = null;
	protected String secretSessionId = null;
	protected boolean authenticated = false;
	private Calendar loginTime;
	private Calendar lastActivity;
	private String ipAddress;

//	private boolean errorStatus = false;
//	private ErrorCodes errorCode = null;

	protected I_Authenticate authenticate;

	public Session( ServiceSecurityManager serviceSecurityManager, String sessionId ) throws ServiceManagerException {
		this.runTimeSingleton = (serviceSecurityManager.getGlobal() == null) ? RunTimeSingleton.instance() : serviceSecurityManager.getGlobal();
		
		log.debug("Initializing Session common="+serviceSecurityManager+", sessionId="+sessionId+".");
		
		this.securityManager = serviceSecurityManager;
		this.secretSessionId = sessionId;
		
//		this.htpasswd = new HtPasswd(this.clusterController);
	}
	
	public I_SecurityProperties init(I_SecurityProperties securityProperties, Map<?, ?> map) throws ServiceManagerException {
		return initializeSession(new SecurityProperties(runTimeSingleton));
	}

//	@Override
//	public I_SecurityProperties initializeSession(I_ConnectProperties connectprops, Map<?, ?> map)
//			throws ServiceManagerException {
//		// TODO Auto-generated method stub
//		return initializeSession(connectprops.getSecurityProperties());
//	}
	
	/**
	 * initialize the session and authentifies user. Throws an exception
	 * if the authentication fails<p>
	 * @return null
	 */
	public I_SecurityProperties initializeSession(I_SecurityProperties securityProps) throws ServiceManagerException {
	      if (securityProps == null) {
	          throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME, AuthErrorCode.USER_INVALID_SECURITY_PARAMETERS.getFullMessage());
	       }
	       this.authenticated = false;
	       String loginName = securityProps.getUserId();
	       String passwd = securityProps.getCredential();
	       this.ipAddress = securityProps.getClientIp();
	       
	       if (loginName == null || passwd == null) {
	          throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME, AuthErrorCode.USER_INVALID_CREDENTIALS.getFullMessage()+" id:"+loginName);
	       }

	       log.debug( "Checking password ...");
	       this.authenticate = Authenticate.newWrapper(runTimeSingleton, securityManager.getPolicyMode(), loginName);
	       this.authenticated = authenticate.checkCredential(passwd);
	       log.debug( "The user for " + loginName + " is " + ((this.authenticated)?"":" NOT ") + " authenticated");

	       
	       if (!this.authenticated)
	          throw new ServiceManagerException(runTimeSingleton, SysErrorCode.USER_SECURITY_AUTHENTICATION_ACCESSDENIED, ME, AuthErrorCode.USER_AUTHENTICATION_FAILED.getFullMessage()+" id:"+loginName);
	       
	       //check the time of access
  		   this.loginTime = Calendar.getInstance();
  		   this.lastActivity = loginTime;
       
	       return securityProps; // no extra information
	}

	/**
	 * returns true if the session is still valid
	 * TODO: extend the verify to more than private credentials eg. inactivity, time of day, ...
	 * 
	 */
	public boolean verify(I_SecurityProperties securityProps) throws ServiceManagerException {
		if (!this.authenticated)
			return false;
		
		if (authenticate.checkPrivateCredentials(securityProps.getUserId(), securityProps.getCredential()) )
			return true;
	       
	       return false;
	}

	public I_Authenticate getAuthenticate() {
		return authenticate;
	}

	public I_Manager getManager() {
		return securityManager;
	}

	public void changeSecretSessionId(String sessionId) throws ServiceManagerException {
	      if(this.secretSessionId.endsWith(sessionId)) return;
	      synchronized(this) {
	         securityManager.changeSecretSessionId(this.secretSessionId, sessionId);
	         this.secretSessionId = sessionId;
	      }
	}

	public String getSecretSessionId() {
		 return secretSessionId;
	}

	/**
	 * verify if this dataholder is authorized for this session<p>
	 * @return false if:<p>
	 * <ul>
	 * <li>user is not authenticated</li>
	 * <li>session is invalid</li>
	 * <li>user is invalid</li>
	 * <li>dataholder is not authorized</li>
	 * 
	 * TODO: verify the session (eg. do not the explicit check below)
	 */
	public boolean isAuthorized(I_SessionHolder sessionHolder, I_ContextHolder dataHolder) {
//		errorStatus = false;
		if (!isWithinTimeout()){
//			errorStatus = true;
//			errorCode = ErrorCodes.SESSION_INACTIVITY_TIMEOUT;
			log.warn("Inactivity Timeout for user: " + authenticate.getUserId());
			return false;
		}
			
		if (this.authenticated == false) {
			log.warn("Authentication of user " + authenticate.getUserId() + " failed");
			return false;
		}
		I_SessionInfo sessionInfo = sessionHolder.getSessionInfo();
		if (sessionInfo == null) {
			log.warn("sessionInfo is null, will not authorize");
			return false;
		}
		I_SessionName sessionName = sessionInfo.getSessionName();
		if (sessionName == null) {
			log.warn("sessionName for '" + sessionInfo.toXml() + "' is null, will not authorize");
			return false;
		}
		String loginName = sessionName.getLoginName();
		if (loginName == null) {
			log.warn("loginName for '" + sessionName.toXml() + "' is null, will not authorize");
			return false;
		}
		
		return authenticate.isAuthorized(dataHolder);
	}

	public boolean isAuthorized(I_ContextHolder dh){
		return authenticate.isAuthorized(dh);
	}

	public boolean isAuthorized(String rightName, String object, String pattern){
		return authenticate.isAuthorized(rightName, object, pattern);
	}
	
//	public MsgUnitRaw importMessage(CryptContextHolder cdh)
//			throws ServiceManagerException {
//		return cdh.getMsgUnitRaw();
//	}
//
//	public MsgUnitRaw exportMessage(CryptContextHolder cdh)
//			throws ServiceManagerException {
////		if (errorStatus){
////			MsgUnit merror = MessageHelper.msgError(clusterController, errorCode.getFullMessage(), ME);
////			errorStatus = false;
////			errorCode = null;
////			return new MsgUnitRaw(merror.getKey(),merror.getContent(),merror.getQos());
////		}
//		return cdh.getMsgUnitRaw();
//	}

	public Calendar getLastActivityTime(){
		return lastActivity;
	}
	
	private boolean isWithinTimeout(){
		boolean b = true;
		if (authenticate.getTimeOut() > 0) {
			Calendar boundary = (Calendar)lastActivity.clone();
			boundary.roll(Calendar.MILLISECOND, authenticate.getTimeOut());
			//check if the current time is beyond the boundary
			Calendar now = Calendar.getInstance();
			
			if (now.compareTo(boundary) > 0)
				b = false;
		}
		lastActivity = Calendar.getInstance();
		return b;
	}

	//used by license manager
	public String getIPAddress(){
		return ipAddress;
	}

	@Override
	public I_ContextHolder importMessage(I_ContextHolder dataHolder)
			throws ServiceManagerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public I_ContextHolder exportMessage(I_ContextHolder dataHolder)
			throws ServiceManagerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String interceptExeptionByAuthorizer(Throwable throwable,
			I_SessionHolder sessionHolder, I_ContextHolder dataHolder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public I_SecurityProperties init(I_ConnectProperties connectprops,
			Map<?, ?> map) throws ServiceManagerException {
		// TODO Auto-generated method stub
		return null;
	}	
	
//	private void setExtendedInfoMessage(ContextHolder dh, ErrorCodes ecode){
//		dh.getMsgUnit().getQosData().addClientProperty("ErrorCode", ecode.getErrorCode());
//		dh.getMsgUnit().getQosData().addClientProperty("ErrorMessage", ecode.getMessage());
//		dh.getMsgUnit().getQosData().addClientProperty("URL", ecode.getURL());
//	}
}

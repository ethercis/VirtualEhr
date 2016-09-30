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
package com.ethercis.authenticate.shiro;
/*
 * 
 */

import com.ethercis.authenticate.dummy.DummyPrincipal;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * Shiro based authentication and permission
 * see http://shiro.apache.org/index.html
 * @author christian
 *
 */
public class ShiroAuthenticate implements I_Authenticate {

    private static final Logger log = LogManager.getLogger(Constants.LOGGER_SECURITY);
    private static final String ME = ShiroAuthenticate.class.getName();

    private int timeout = -1; //timeout is the number of millisec allowed
    private String userid;
    private RunTimeSingleton global;
    private Subject currentUser;


	/**
	 * create a new authenticate for a given id (<b>logon id</b>)<p>
	 * @param glob Global
	 * @param id the logon id (unique)
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException if the authenticate is revoked access or other error
	 */
	public ShiroAuthenticate(RunTimeSingleton glob, String id) throws ServiceManagerException {
		this.global = glob;
		this.userid = id;

        try {
            currentUser = SecurityUtils.getSubject();
        } catch (UnavailableSecurityManagerException usme){
           throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Shiro Security Manager is not configured properly:"+usme);
        }

	}
	/**
	 * check is authenticate is authorized this dataholder<p>
	 * @param dataHolder
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorized(I_ContextHolder dataHolder){

		return true;
	}
	/**
	 * check if authenticate is authorized this right to access an object (target) with
	 * a given pattern<p>
	 * @param rightName the symbolic right name
	 * @param object the target name
	 * @param pattern the pattern to check for
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorized(String rightName, String object, String pattern){

		return true;		
	}


	/**
	 * returns the user id of this user<p>
	 */
	public String getUserId() {
		return "USER[SHIRO]_"+userid;
	}

    @Override
    public void setUserId(String userId) {
        this.userid = userId;
    }

    /**
	 * get the code
	 */
	public String getUserCode(){
		return "DEBUG";
	}

	/**
	 * check the user credential<p>
	 * User password verification. The credential is validated if:<br>
	 * <ul>
	 * <li>an empty password is authorized</li>
	 * <li>the supplied crypted password matches the one defined in the policy file</li>
	 * </ul> 
	 * 
	 * @param credential (password)
	 * @return true is validated, false otherwise
	 */
	public boolean checkCredential(String credential) throws ServiceManagerException {

        return checkPrivateCredentials(userid, credential);
	}
	/**
	 * not used.
	 * @param logonId
	 * @param passwd
	 * @return
	 */
	public boolean checkPrivateCredentials(String logonId, String passwd) throws ServiceManagerException {
        if (currentUser.isAuthenticated()){
            return true;
        }

        UsernamePasswordToken token = new UsernamePasswordToken( logonId, passwd );
        try {
            currentUser.login(token);
        }  catch (UnknownAccountException uae){
            throw new ServiceManagerException(global, SysErrorCode.USER_CONNECT, ME, "Unknown account:"+logonId);
        } catch (IncorrectCredentialsException ice){
            throw new ServiceManagerException(global, SysErrorCode.USER_CONNECT, ME, "Invalid credential:"+logonId);
        } catch (LockedAccountException lae){
            throw new ServiceManagerException(global, SysErrorCode.USER_CONNECT, ME, "Account is locked:"+logonId);
        } catch (ExcessiveAttemptsException eae){
            throw new ServiceManagerException(global, SysErrorCode.USER_CONNECT, ME, "Too many login attempts:"+logonId);
        } catch (AuthenticationException ae){
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_CONNECTIONFAILURE, ME, "Internal connection error:"+logonId);
        }

        return currentUser.isAuthenticated();
	}

	/**
	 * returns the defined inactivity timeout (millisec)<p>
	 * @return int timeout
	 */
	public int getTimeOut(){
		return timeout;
	}
	

	/**
	 * get the public credentials for a user in CSV format.<p>

	 * @return the csv string 
	 */
	public String getPublicCredentialsCSV(){
		StringBuffer sb = new StringBuffer();
		sb.append("USER[SHIRO] id:"+userid);
	
		return sb.toString();
	}	
	/**
	 * return an array of CSV string for all public credentials of all defined
	 * users in the policy.
	 * @return
	 */
	public String[] getSubjectArrayCSV(boolean withSystem){
		ArrayList<String> result = new ArrayList<String>();
		
		return result.toArray(new String[0]);
	}
	@Override
	public List<I_Principal> getPrincipals() {
		List<I_Principal> principals = new ArrayList<I_Principal>();
        principals.add(new DummyPrincipal());
        return principals;
	}

}

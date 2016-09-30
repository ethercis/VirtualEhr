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
package com.ethercis.authenticate.dummy;
/*
 * 
 */
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.ArrayList;
import java.util.List;

/**
 * Pass through Subject (e.g. no security check, all is OK) for debugging only
 * @author christian
 *
 */
public class DummyAuthenticate implements I_Authenticate {

	private static final Logger log = LogManager.getLogger(Constants.LOGGER_SECURITY);
	private static final String ME = DummyAuthenticate.class.getName();

	private int timeout = -1; //timeout is the number of millisec allowed
	private String userid;
	private RunTimeSingleton global;
	
	/**
	 * create a new authenticate for a given id (<b>logon id</b>)<p>
	 * @param glob Global
	 * @param id the logon id (unique)
	 * @param policy the policy associated to this authenticate
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException if the authenticate is revoked access or other error
	 */
	public DummyAuthenticate(RunTimeSingleton glob, String id) throws ServiceManagerException {
		this.global = glob;
		this.userid = id;

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
		return "DEBUG_USER_"+userid;
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
	public boolean checkCredential(String credential){
		return true;
	}
	/**
	 * not used.
	 * @param logonId
	 * @param passwd
	 * @return
	 */
	public boolean checkPrivateCredentials(String logonId, String passwd){
		return true;
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
		sb.append("DEBUG_USER id:"+userid);
	
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

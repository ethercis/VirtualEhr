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
package com.ethercis.authenticate;

import com.ethercis.authenticate.dummy.DummyAuthenticate;
import com.ethercis.authenticate.shiro.ShiroAuthenticate;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.interfaces.data.I_User;
import com.ethercis.servicemanager.common.jcrypt;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract to handle logic associated to Subject Normally specialized according
 * to the policy.
 * 
 * @author Pro7
 * 
 */
public abstract class Authenticate implements I_Authenticate {
	protected static final Logger log = LogManager.getLogger(Authenticate.class.getName());
	protected static final String ME = Authenticate.class.getName();
	protected int timeout = -1; // timeout is the number of millisec allowed
	protected RunTimeSingleton global;
	protected I_User user;

	protected List<I_Principal> principals = new ArrayList<I_Principal>();

    /**
     * get a new newWrapper for a virtual subject defined elsewhere
     * @param global
     * @param mode defines the actual policy mode: XML, LDAP, JDBC, DEBUG
     * @param loginName the user id for this authenticate
     * @return an interface to new Subject
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    public static I_Authenticate newWrapper(RunTimeSingleton global, int mode, String loginName) throws ServiceManagerException {
        switch (mode) {
            case Constants.POLICY_DEBUG:
                return new DummyAuthenticate(global, loginName);
            case Constants.POLICY_SHIRO:
                return new ShiroAuthenticate(global, loginName);
            default:
                log.error("Authenticate mode is not defined properly, got:"+mode);
                throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION, ME, "Authenticate mode is not defined properly, got:"+mode);

        }
    }

    /**
	 * check is authenticate is authorized this dataholder
	 * <p>
	 * 
	 * @param dataHolder
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorized(I_ContextHolder dataHolder) {
		// check the result for each principals
		for (I_Principal p : principals) {
			// log.dummy("Principal " + p + ":" + dataHolder );
			if (p.isAuthorized(dataHolder))
				return true;
		}
		return false;
	}

	/**
	 * check if authenticate is authorized this right to access an object (target)
	 * with a given pattern
	 * <p>
	 * 
	 * @param rightName
	 *            the symbolic right name
	 * @param object
	 *            the target name
	 * @param pattern
	 *            the pattern to check for
	 * @return true if authorized, false otherwise
	 */
	public boolean isAuthorized(String rightName, String object, String pattern) {
		for (I_Principal p : principals) {
			// log.dummy("Principal " + p + ":" + rightName + ":" + object + ":"
			// +pattern );
			if (p.isAuthorized(rightName, object, pattern))
				return true;
		}
		return false;
	}

	public List<I_Principal> getPrincipals() {
		return principals;
	}

	/**
	 * not used.
	 * 
	 * @param logonId
	 * @param passwd
	 * @return
	 */
	public boolean checkPrivateCredentials(String logonId, String passwd) {
		return (logonId.compareTo(user.getId()) == 0 && passwd.compareTo(user
				.getPassword()) == 0);
	}

	/**
	 * returns the defined inactivity timeout (millisec)
	 * <p>
	 * 
	 * @return int timeout
	 */
	public int getTimeOut() {
		return timeout;
	}

	/**
	 * check the user credential
	 * <p>
	 * User password verification. The credential is validated if:<br>
	 * <ul>
	 * <li>an empty password is authorized</li>
	 * <li>the supplied crypted password matches the one defined in the policy
	 * file</li>
	 * </ul>
	 * 
	 * @param credential
	 *            (password)
	 * @return true is validated, false otherwise
	 */
	public boolean checkCredential(String credential) {
		String encoded = null, salt, userEncoded;

		encoded = user.getPassword();
		if (encoded != null && encoded.length() == 0)
			return true; // empty password "joe::"
		if (encoded != null && encoded.length() > 2) {
			salt = encoded.substring(0, 2); // this is a constant part in the
											// encoding
			userEncoded = jcrypt.crypt(salt, credential);
			log.debug("comparing: " + userEncoded + " with:" + encoded);
			return (userEncoded.trim().equals(encoded.trim()));
		}
		return false;
	}

	/**
	 * returns the user id of this user
	 * <p>
	 */
	public String getUserId() {
		return user.getId();
	}

	/**
	 * get the code
	 */
	public String getUserCode() {
		return user.getCode();
	}

	public I_User getUser() {
		return user;
	}

	public String getPublicCredentialsCSV() {
		return "Not implemented, pls override";
	}
}

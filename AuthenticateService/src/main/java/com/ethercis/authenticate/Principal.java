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

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.security.I_Permission;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.security.I_Right;
import com.ethercis.servicemanager.common.security.I_Rights;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Abstract to manage the logic associated with Principal
 * This class must be specialized according to one or more policy implementation.<br>
 * Each principal holds a list of permissions, the permissions are used to
 * set an authorization mask depending on which permission is to be checked<p>
 * <ul>
 * <li>An implied permission is a permission which is implied by the permission
 * to check against, for instance, the defined permission with:<br>
 * <code>target = 'OID'</code> and<br> 
 * <code>pattern = 'Report.*'</code><br>
 * will be implied by a permission with<br>
 * <code>target = 'OID'</code> and<br>
 * <code>pattern = 'ReportStatistics'</code>
 * </li>  
 * <li>The authorization mask is a bitmap defining the rights associated to an
 * implied permission (either granted[1] or revoked[0])</li>
 * </ul>
 * @author C.Chevalley
 *
 */
public abstract class Principal implements I_Principal {
	protected static final Logger logger = LogManager.getLogger(Constants.LOGGER_SECURITY);
	protected String name;
	protected I_Rights rights;
	protected List<I_Permission> permissions = new ArrayList<I_Permission>();
	
	/**
	 * used to keep Permission abstract
	 * @author Christian
	 *
	 */
	private class LocalPermission extends Permission {
		/**
		 * 
		 */
		private static final long serialVersionUID = 3778315542637718936L;

		public LocalPermission(String name, MethodName action, String objectName, String pattern, Map<String, String> parameters){
			super(name, action, objectName, pattern, parameters);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Principal#isAuthorized(com.ethercis.common.common.session.I_ContextHolder)
	 */
	@Override
	public boolean isAuthorized(I_ContextHolder holder){
		I_Right right = rights.forName(holder.getMethod().getMethodName());
		//logger.debugst("rights=" +right);
		MethodName mname = holder.getMethod();
		String resource = holder.getQueryUnit().getResource();
		// get the parameters
		Map<String, String> props = null;
		I_SessionClientProperties parms = holder.getQueryUnit().getParameters();
		if (parms != null){
			props = parms.clientProps2StringMap();
		}

		//get the equivalent permission for the holder
		I_Permission msgperm = 
			new LocalPermission("", mname, holder.getQueryUnit().getTag(), resource, props);
		//find all implied permission for this principal
		return rights.isGranted(right, setAuthorizationMask(msgperm));
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Principal#isPermissionAuthorized(java.lang.String, com.ethercis.common.common.session.I_ContextHolder)
	 */
	@Override
	public boolean isPermissionAuthorized(String permissionname, I_ContextHolder holder){
		I_Right right = rights.forName(holder.getMethod().getMethodName());
		//get the equivalent permission for the holder
		I_Permission msgperm = 
			new LocalPermission(permissionname, null, null, null, null);
		//find all implied permission for this principal
		return rights.isGranted(right, setAuthorizationMask(msgperm));
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Principal#isAuthorized(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isAuthorized(String rightName, String object, String pattern){
		I_Right right = rights.forName(rightName);
		I_Permission thisperm = 
			new LocalPermission("",null,object,pattern,null);
		return rights.isGranted(right, setAuthorizationMask(thisperm));
		
	}
	
	/**
	 * sets the authorization mask depending on implied permissions for a permission
	 * @param permission
	 * @return BigInteger holding the bitmask of granted rights
	 */
	protected BigInteger setAuthorizationMask(I_Permission permission){
		BigInteger authorizationMask = BigInteger.ZERO; //no authorization
		for (I_Permission defperm: permissions){
			if (defperm.implies(permission)){
				logger.debug("Implied permission:"+defperm.getName());
				//get the authorization mask for this permission
				for (String s: defperm.getGranted()){
					I_Right r = rights.forName(s);
					if (r == null){
						logger.error("Possible invalid policy file, no mapping for right:'"+s+"'");
						return BigInteger.ZERO; //empty mask
					}
					BigInteger mask = r.getMaskBI();
					authorizationMask = authorizationMask.or(mask);
				}
				for (String s: defperm.getRevoked()){
					I_Right r = rights.forName(s);
					BigInteger mask = r.getMaskBI();
					authorizationMask = authorizationMask.andNot(mask);
				}
			}
		}
		return authorizationMask;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Principal#getUserId()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Principal#getPermissions()
	 */
	@Override
	public List<I_Permission> getPermissions() {
		return permissions;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Principal#getRights()
	 */
	@Override
	public I_Rights getRights() {
		return rights;
	}
	
}

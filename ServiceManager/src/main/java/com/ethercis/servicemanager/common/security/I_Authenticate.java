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

import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.util.List;

/**
 * Interface to Subject to hide the actual Subject implementation (Xml, Ldap, dummy etc.)
 * @author C.Chevalley
 *
 */
public interface I_Authenticate {
	public boolean isAuthorized(I_ContextHolder dataHolder);
	public boolean isAuthorized(String rightName, String object, String pattern);
	public String getUserId();
    public void setUserId(String userId);
	public String getUserCode();
	public boolean checkCredential(String credential) throws ServiceManagerException;
	public boolean checkPrivateCredentials(String logonId, String passwd) throws ServiceManagerException;
	public int getTimeOut();
    public List<I_Principal> getPrincipals();
	void release();
}

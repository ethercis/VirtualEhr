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
package com.ethercis.servicemanager.common.interfaces.services;

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.session.I_SessionHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Interface to be used logonservice facade whenever the policy can be edited from the backend.
 * This excludes normally policy management done from another service such logonservice LDAP or SQL DB<p>
 * One of the issue not addressed here is the format for the queries. For XML policy, the queries 
 * use normally XML syntax including XPATH which is not applicable to other representation.
 * @author C.Chevalley
 *
 */
public interface I_PolicyManager {
	public Object doChangePolicy(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
	public void doCommitPolicy(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
	public void doEditPolicy(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
	public Object doGetPolicy(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
	public Object doPolicyAddNew(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
	public Object doDeletePolicy(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
	public void doReversePolicy(I_SessionHolder sessionholder, I_SessionClientProperties parms) throws ServiceManagerException;
}

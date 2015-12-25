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

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

public interface I_ContextHolder {

	/**
	 * @return Returns the method, never null
	 */
	public abstract MethodName getMethod();

	/**
	 * @return Returns the action, never null
	 */
	public abstract MethodName getAction();

	/**
	 * @return Returns the msgUnit, is never null
	 */
	public abstract I_QueryUnit getQueryUnit();

	/**
	 * Convenience method to access the parameters of QueryUnit
	 * @return Never null
	 */
	public abstract I_SessionClientProperties getClientProperties();

	public abstract String toString();

	/**
	 * @return Usual null, can contain additional info for caller in error case
	 */
	public abstract String getNotAuthorizedInfo();

	public abstract void setNotAuthorizedInfo(String notAuthorizedInfo);

	public abstract ServiceManagerException getExceptionToThrow();

	/**
	 * Allows a security plugin to throw another exception instead of ErrorCode.USER_SECURITY_AUTHORIZATION_NOTAUTHORIZED
	 * when returning false during isAuthorized() call
	 */
	public abstract void setExceptionToThrow(ServiceManagerException exceptionToThrow);

}
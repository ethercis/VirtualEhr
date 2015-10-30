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

import java.util.List;

import com.ethercis.servicemanager.common.session.I_ContextHolder;

public interface I_Principal {

	/**
	 * check if a ContextHolder is authorized for this principal<p>
	 * The method gets all permission verification from the holder, including:<p>
	 * <ul>
	 * <li>action: the MethodName from the holder (eg. get, publish, connect ...)</li>
	 * <li>oid: the resource <b>pattern</b></li>
	 * <li>parameters 
	 * @param holder
	 * @return true if the action (MethodName) is granted for this holder 
	 */
	public abstract boolean isAuthorized(I_ContextHolder holder);

	/**
	 * check if a specific permission with ContextHolder is authorized for this principal<p>
	 * The method gets name permission verification from the holder and check if the right is set
	 * @param sessionname the name of the permission logonservice found in the policy (permission name="a name")
	 * @param holder
	 * @return true if the action (MethodName) is granted for this holder 
	 */
	public abstract boolean isPermissionAuthorized(String permissionname,
			I_ContextHolder holder);

	/**
	 * check if a right is granted for a given target and pattern<p>
	 * This method is intended to be used with request to check for 
	 * an object access such logonservice menuItem<p>
	 * @param rightName
	 * @param object
	 * @param pattern
	 * @return
	 */
	public abstract boolean isAuthorized(String rightName, String object,
			String pattern);

	public abstract String getName();

	public abstract List<I_Permission> getPermissions();

	public abstract I_Rights getRights();

}
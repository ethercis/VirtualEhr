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

import java.math.BigInteger;
import java.util.Map;


public interface I_Rights {

	/**
	 * Checks if a right is granted for a given bit mask
	 * <p>
	 * 
	 * @param the
	 *            bit mask to check for
	 * @param authorizationMask
	 *            the bit mask to look in
	 * @return true if right is granted
	 */
	public abstract boolean isGranted(BigInteger testmask,
			BigInteger authorizationMask);

	/**
	 * Checks if a right is granted for a given bit mask
	 * <p>
	 * 
	 * @param r
	 *            the right to check for
	 * @param authorizationMask
	 *            the bit mask to look in
	 * @return true if right is granted
	 */
	public abstract boolean isGranted(I_Right r, BigInteger authorizationMask);

	/**
	 * returns right for a symbolic name
	 * <p>
	 * 
	 * @param name
	 * @return Right
	 */
	public abstract I_Right forName(String name);

	/**
	 * returns rights corresponding to a bit mask
	 * <p>
	 * Each right is defined by a bit set into a bit mask. See policy for more
	 * details.
	 * 
	 * @param mask
	 * @return an array of Right
	 */
	public abstract I_Right[] forMask(BigInteger mask);

	/**
	 * Checks if a right is granted for a given bit mask
	 * <p>
	 * 
	 * @param rightName
	 *            the symbolic name of right
	 * @param authorizationMask
	 *            the bit mask to look in
	 * @return true if right is granted
	 */
	public abstract boolean isGranted(String rightName,
			BigInteger authorizationMask);

	/**
	 * Checks if a right is revoked for a given bit mask
	 * <p>
	 * 
	 * @param r
	 *            the right to check for
	 * @param authorizationMask
	 *            the bit mask to look in
	 * @return true if right is granted
	 */
	public abstract boolean isRevoked(I_Right r, BigInteger authorizationMask);

	public abstract void setRights(Map<String, I_Right> rights);

	public abstract Map<String, I_Right> getRights();

}
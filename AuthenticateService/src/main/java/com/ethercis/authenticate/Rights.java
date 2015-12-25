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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ethercis.servicemanager.common.security.I_Right;
import com.ethercis.servicemanager.common.security.I_Rights;


/**
 * Used to check actual rights (based on bitmask of allowed methods)
 * @author C.Chevalley
 *
 */
public abstract class Rights implements I_Rights {
	protected Map<String, I_Right> rights = new HashMap<String, I_Right>();

	/**
	 * Checks if a right is granted for a given bit mask<p>
	 * @param the bit mask to check for
	 * @param authorizationMask the bit mask to look in
	 * @return true if right is granted
	 */
	public boolean isGranted(BigInteger testmask, BigInteger authorizationMask){
		// a null right is only authorized if all rights are granted 
		// the authorizationMask must be equal to all bits to 1
		if (testmask == null){
			if (authorizationMask.not().compareTo(BigInteger.ZERO)==0)
				return true;
			else
				return false;
		}
//		BigInteger testmask = getMask(r);
//		return (testmask.and(authorizationMask)).compareTo(testmask) == 0;
		return (testmask.and(authorizationMask)).compareTo(testmask) == 0;
	}
	
	/**
	 * Checks if a right is granted for a given bit mask<p>
	 * @param r the right to check for
	 * @param authorizationMask the bit mask to look in
	 * @return true if right is granted
	 */
	public boolean isGranted(I_Right r, BigInteger authorizationMask){
		// a null right is only authorized if all rights are granted 
		// the authorizationMask must be equal to all bits to 1
		if (r == null){
			if (authorizationMask.not().compareTo(BigInteger.ZERO)==0)
				return true;
			else
				return false;
		}
		BigInteger testmask = new BigInteger(r.getMask());
		return (testmask.and(authorizationMask)).compareTo(testmask) == 0;
	}
	/**
	 * returns right for a symbolic name<p>
	 * @param name
	 * @return Right
	 */
	public I_Right forName(String name){
		return rights.get(name);
	}
	/**
	 * returns rights corresponding to a bit mask<p>
	 * Each right is defined by a bit set into a bit mask. See policy for more
	 * details.
	 * @param mask
	 * @return an array of Right
	 */
	public I_Right[] forMask(BigInteger mask){
		List<I_Right> retlist = new ArrayList<I_Right>();
		
		for (I_Right r: rights.values()){
			if (isGranted(r, mask))
				retlist.add(r);
		}
		return retlist.toArray(new I_Right[0]);
	}

	/**
	 * Checks if a right is granted for a given bit mask<p>
	 * @param rightName the symbolic name of right
	 * @param authorizationMask the bit mask to look in
	 * @return true if right is granted
	 */
	public boolean isGranted(String rightName, BigInteger authorizationMask){
		I_Right r = rights.get(rightName);
		return isGranted(r, authorizationMask);
	}
	/**
	 * Checks if a right is revoked for a given bit mask<p>
	 * @param r the right to check for
	 * @param authorizationMask the bit mask to look in
	 * @return true if right is granted
	 */
	public boolean isRevoked(I_Right r, BigInteger authorizationMask){
		return !isGranted(r, authorizationMask);
	}

	public Map<String, I_Right> getRights() {
		return rights;
	}
	
	
}

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
import java.util.Map;
import java.util.regex.Pattern;

import com.ethercis.servicemanager.common.def.MethodName;

public interface I_Permission {

	/**
	 * check if a permission is implied by another<p>
	 * The implication follows the following rules<p>
	 * <ol>
	 * <li>it is implied if the names are equals</li>
	 * <li>it is <b>NOT</b> if the action is defined but not in the one to check for</li>
	 * <li>it is <b>NOT</b> if both actions are defined but they are not identical<li>
	 * <li>it is <b>NOT</b> if the target is defined but is not matching with the one to check for (regexp check)</li>
	 * <li>if no target pattern is defined in this permission, check parameters only (see below)</li>
	 * <li>if target pattern is defined but not in the other one, the implication failed</li>
	 * <li>if target pattern is defined but none of the defined one in the other permission, the implication failed</li>
	 * </ol><br>
	 * Parameters check<p>
	 * The parameters match if:<p>
	 * <ol>
	 * <li>if no parameters are defined for this permission</li>
	 * <li>if both permission defined parameters AND</li>
	 * <li>if both pattern match for each defined parameters</li>
	 * </ol> 
	 * @param another
	 * @return
	 */
	public abstract boolean implies(I_Permission another);

	/**
	 * evaluate parameters implication for a permission<p>
	 * @param another
	 * @return
	 */
	public abstract boolean impliesParameters(I_Permission another);

	public abstract String getName();

	public abstract String getObjectName();

	public abstract String getFilter();

	public abstract Map<String, List<String>> getParameters();

	public abstract MethodName getAction();

	/**
	 * return granted rights for this permission<p>
	 * @return
	 */
	public abstract List<String> getGranted();

	/**
	 * return revoked rights for this permission<p>
	 * @return
	 */
	public abstract List<String> getRevoked();

	public abstract Pattern getPattern();

	/**
	 * string out<p>
	 */
	public abstract String toString();

}
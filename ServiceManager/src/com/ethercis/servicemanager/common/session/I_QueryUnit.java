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

public interface I_QueryUnit {

	public abstract String getClientIP();

	public abstract void setClientIP(String clientIP);

	public abstract MethodName getMethod();

	public abstract MethodName getAction();

	public abstract I_SessionClientProperties getParameters();

	public abstract void setParameters(I_SessionClientProperties parameters);

	public abstract I_SessionClientProperties getHeaders();

	public abstract void setHeaders(I_SessionClientProperties headers);

	public abstract String getResource();

	public abstract String getTag();

	public abstract String toString();

}
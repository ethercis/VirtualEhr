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
package com.ethercis.logonservice.session.protectors;

import com.ethercis.servicemanager.common.session.I_ConnectProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Export methods for JMX. 
 */
public interface SessionInfoProtectorMBean {
	public  String getId();
	public  String getLoginName();
	public  String getConnectionState();
	public  long getPublicSessionId();
	public  String getLoginDate();
	public  String getSessionTimeoutExpireDate();
	public  String getAliveSinceDate();
	public  void refreshSession() throws ServiceManagerException;
	public  long getUptime();
	public  String killSession() throws ServiceManagerException;
	public  long getMaxSessions();
	public  long getSessionTimeout();
	public String getUsageUrl();
	public I_ConnectProperties getConnectProperties();
}


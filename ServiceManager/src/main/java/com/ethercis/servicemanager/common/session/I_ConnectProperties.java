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


public interface I_ConnectProperties {

	/**
	 * @return The user ID or "NoLoginName" if not known
	 */
//	public  String getUserId();

	public  I_SessionName getSessionName();
//
	public  I_SecurityProperties getSecurityProperties();
//
	public  boolean bypassCredentialCheck();
//
	public  String toXml();
//
	public  String getSecretSessionId();
//
	public  I_SessionProperties getSessionProperties();
//
	public  boolean reconnectSameClientOnly();
//
	public  void setSecretSessionId(String oldSecretSessionId);
//
	public  boolean clearSessions();
//
	public  void setPtpAllowed(boolean clientProperty);

	public  void setClusterNode(boolean clientProperty);

	public  void setRefreshSession(boolean clientProperty);

	public  void setDuplicateUpdates(boolean clientProperty);
//
//	public  void setReconnected(boolean clientProperty);
//
//	public  void setSessionTimeout(int clientProperty);
//
//	public  void setMaxSessions(int clientProperty);
//
//	public  void clearSessions(boolean clientProperty);

	public  void bypassCredentialCheck(boolean clientProperty);
	
	public  boolean isPtpAllowed();
	public  boolean isPersistent();
	public  String getUserId();

}
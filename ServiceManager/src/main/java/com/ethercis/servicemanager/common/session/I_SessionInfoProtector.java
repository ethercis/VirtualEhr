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

import java.util.Map;


public interface I_SessionInfoProtector {

	public abstract String getId();

	public abstract String getLoginName();

	public abstract String getQos();

	public abstract String getConnectionState();

	public abstract long getPublicSessionId();

	public abstract String getLoginDate();

	public abstract String getSessionTimeoutExpireDate();

	public abstract String getAliveSinceDate();

	public abstract String getPollingSinceDate();

	public abstract long getUptime();

	public abstract Map<String, Object> getUserObjectMap();

	public abstract boolean isBlockClientSessionLogin();

	/** Enforced by ConnectQosDataMBean interface. */
	public abstract long getMaxSessions();

	/** Enforced by ConnectQosDataMBean interface. */
	public abstract long getSessionTimeout();

	/** Enforced by ConnectQosDataMBean interface. */
	public abstract boolean isPtpAllowed();

	/** Enforced by ConnectQosDataMBean interface. */
	public abstract boolean isPersistent();

	public abstract String[] getRemoteProperties();

	/** JMX */
	public abstract java.lang.String getUsageUrl();

	public abstract I_ConnectProperties getConnectProperties();

	public abstract boolean isStalled();

	public abstract boolean isAcceptWrongSenderAddress();

}
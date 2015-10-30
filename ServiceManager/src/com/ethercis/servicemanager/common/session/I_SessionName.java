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

import com.ethercis.servicemanager.cluster.NodeId;

public interface I_SessionName {

	public abstract String getAbsoluteName();

	/**
	 * If the nodeId is not known, the relative name is returned
	 * 
	 * @return e.g. "/node/heron/client/joe/2", never null
	 */
	public abstract String getAbsoluteName(boolean forceSessionMarker);

	/**
	 * If the nodeId is not known, the relative name is returned a possible
	 * session is ommitted
	 * 
	 * @return e.g. "/node/heron/client/joe", never null
	 */
	public abstract String getAbsoluteSubjectName();

	/**
	 * @return #getAbsoluteName()
	 */
	public abstract String toString();

	/**
	 * @return e.g. "client/joe/session/2" or "client/joe", never null
	 */
	public abstract String getRelativeName();

	/**
	 * @param forceSessionMarker
	 *           If false the configured syntax is chosen, if true wie force the
	 *           /session/ markup
	 * @return e.g. "client/joe/session/2" or "client/joe", never null
	 */
	public abstract String getRelativeName(boolean forceSessionMarker);

	/**
	 * @return e.g. "client/joe/2" or "client/joe", never null
	 */
	public abstract String getRelativeNameWithoutSessionMarker();

	/**
	 * Check if the address string given to our constructor had an explicit
	 * specified nodeId
	 */
	public abstract boolean isNodeIdExplicitlyGiven();

	/**
	 * @return e.g. "heron", or null
	 */
	public abstract NodeId getNodeId();

	/**
	 * @return e.g. "heron", or null
	 */
	public abstract String getNodeIdStr();

	/**
	 * @return e.g. "joe", never null
	 */
	public abstract String getLoginName();

	public abstract boolean isInternalLoginName();

	public abstract boolean isPluginInternalLoginName();

	public abstract boolean isCoreInternalLoginName();

	/**
	 * @return The public session identifier e.g. "2" or 0 if in authenticate context
	 */
	public abstract long getPublicSessionId();

	/**
	 * Check if we hold a session or a authenticate
	 */
	public abstract boolean isSession();

	/** @return true it publicSessionId is given by ehrserver server (if < 0) */
	public abstract boolean isPubSessionIdInternal();

	/** @return true it publicSessionId is given by user/client (if > 0) */
	public abstract boolean isPubSessionIdUser();

	/**
	 * @return true if relative name equals
	 */
	public abstract boolean equalsRelative(I_SessionName sessionName);

	public abstract boolean equalsAbsolute(I_SessionName sessionName);

	/**
	 * Check matching name, the subjectId or pubSessionId can be a wildcard "*"
	 * (note: no blanks in the example below are allowed, we need to write it
	 * like this to distinguish it from a java comment)
	 * 
	 * @param pattern
	 *           for example "client/ * /session/ *", ""client/ *
	 *           /session/1", "client/joe/session/ *", "client/joe/session/1"
	 * @return true if wildcard matches or exact match
	 */
	public abstract boolean matchRelativeName(String pattern);

	/**
	 * Note: no blanks in the example below are allowed, we need to write it like
	 * this to distinguish it from a java comment
	 * 
	 * @return e.g. "client/ * /session/2"
	 */
	public abstract String getRelativeSubjectIdWildcard();

	/**
	 * Note: no blanks in the example below are allowed, we need to write it like
	 * this to distinguish it from a java comment
	 * 
	 * @return e.g. "client/joe/session/ *
	 */
	public abstract String getRelativePubSessionIdWildcard();

	/**
	 * Note: no blanks in the example below are allowed, we need to write it like
	 * this to distinguish it from a java comment
	 * 
	 * @return "client/* /session/*
	 */
	public abstract String getRelativeWildcard();

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @return internal state of SessionName logonservice a XML ASCII string
	 */
	public abstract String toXml();

	/**
	 * Dump state of this object into a XML ASCII string. <br>
	 * 
	 * @param extraOffset
	 *           indenting of tags for nice response
	 * @return internal state of SessionName logonservice a XML ASCII string
	 */
	public abstract String toXml(String extraOffset);

}
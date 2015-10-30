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
package com.ethercis.vehr;

import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.common.security.I_Authenticate;

public class Authentication {
	private I_Session session;
	private I_Authenticate subject;

	public Authentication(I_Session session, I_Authenticate subject) {
		super();
		this.session = session;
		this.subject = subject;
	}

	public Authentication() {
		super();
	}

	public I_Session getSession() {
		return session;
	}

	public void setSession(I_Session session) {
		this.session = session;
	}

	public I_Authenticate getSubject() {
		return subject;
	}

	public void setSubject(I_Authenticate subject) {
		this.subject = subject;
	}

}

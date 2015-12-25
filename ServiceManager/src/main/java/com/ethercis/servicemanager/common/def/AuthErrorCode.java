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
package com.ethercis.servicemanager.common.def;

public enum AuthErrorCode {
	
	USER_NOT_AUTHENTICATED(901,"User is not authenticated",null),
	SESSION_INACTIVITY_TIMEOUT(902,"Session inactivity timeout", null),
	USER_ACCESS_LOCKED(903,"Session is locked", null),
	USER_INVALID_CREDENTIALS(904, "User credentials are invalid", null),
	USER_NOT_AUTHORIZED(905,"User is not authorized", null),
	USER_INVALID_SECURITY_PARAMETERS(906,"User supplied invalid security parameters", null),
	USER_AUTHENTICATION_FAILED(907,"User authentication failed", null),
	USER_ACCOUNT_DISABLED(908,"User account is disabled", null),
	USER_ACCESS_OUT_OF_TIME(909,"User is not allowed access at this time", null),
	SESSION_INVALID(910,"The session is invalid", null),
	LICENSE_QUOTA_EXCEEDED(911, "Quota exceeded, contact your system administrator", null), 
	USER_SECURITY_AUTHENTICATION_ACCESSDENIED(912, "User is denied access", null);
	public static final String __REVISION_ID="$Revision: 1.3 $ $Date: 2007/11/20 23:47:25 $";
	
	int code;
	String message;
	String URL;
	
	AuthErrorCode(int code, String message, String URL){
		this.code = code;
		this.message = message;
		this.URL = URL;
	}
	
	public String getMessage(){
		return message;
	}
	
	public String getFullMessage(){
		return "["+code+"]:"+message;
	}
	
	public int getErrorCode(){
		return code;
	}
	
	public String getURL(){
		return URL;
	}
}

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

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */
//Copyright
package com.ethercis.logonservice.access;

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Container to transport information to the isAuthorized() method.
 * 
 */
public class ContextHolder implements I_ContextHolder {

   private final MethodName action; //for example an HTTP method: GET, POST, etc.
   private final I_QueryUnit qryUnit;
   private transient String notAuthorizedInfo;
   private transient ServiceManagerException exceptionToThrow;

   /**
    * @param method May not be null
    * @param qryUnit May not be null
    */
   public ContextHolder(MethodName action, I_QueryUnit qryUnit) {
      super();
      if (qryUnit == null)
    	  throw new IllegalArgumentException("Creating DataHolder expects none null QueryUnit");
      if (qryUnit.getMethod() == null) throw new IllegalArgumentException("Creating DataHolder expects none null method");
      this.action = action;
      this.qryUnit = qryUnit;
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#getMethod()
 */
   @Override
public MethodName getMethod() {
      return this.qryUnit.getMethod();
   }

   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#getAction()
 */
   @Override
public MethodName getAction() {
      return this.action;
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#getQueryUnit()
 */
   @Override
public I_QueryUnit getQueryUnit() {
      return this.qryUnit;
   }


   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#getClientProperties()
 */
   @Override
public I_SessionClientProperties getClientProperties() {
	   if (this.qryUnit.getParameters() == null) 
		   throw new IllegalStateException("ContextHolder.getClientProperties should never be null");
	   return this.qryUnit.getParameters();
   }

	
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#toString()
 */
@Override
public String toString() {
      return this.qryUnit.getMethod() + " " + this.qryUnit.getParameters();
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#getNotAuthorizedInfo()
 */
   @Override
public String getNotAuthorizedInfo() {
      return notAuthorizedInfo;
   }
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#setNotAuthorizedInfo(java.lang.String)
 */
@Override
public void setNotAuthorizedInfo(String notAuthorizedInfo) {
      this.notAuthorizedInfo = notAuthorizedInfo;
   }
   
   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#getExceptionToThrow()
 */
@Override
public ServiceManagerException getExceptionToThrow() {
	  return exceptionToThrow;
   }

   /* (non-Javadoc)
 * @see com.ethercis.logonservice.access.I_ContextHolder#setExceptionToThrow(com.ethercis.common.exceptions.ServiceManagerException)
 */
   @Override
public void setExceptionToThrow(ServiceManagerException exceptionToThrow) {
	  this.exceptionToThrow = exceptionToThrow;
   }
}

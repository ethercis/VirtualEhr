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

import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Interface declaring methods to intercept messages in the security layer
 * to allow crypt/encrypt etc. messages before sending. 
 * <p />
 * If for example the server security service ciphers
 * messages with rot13, we need to deciphers it on the
 * client side with the same algorithm. This is done here.
 * <p />
 * The reason is for clients to access xmlBlaster
 * transparently from the authentication method
 * <p />
 * For every service type you need, you need on instance of this class.
 */
public interface I_MsgSecurityInterceptor {

   /**
    * Decrypt, check, unseal etc an incomming message. 
    * <p/>
    * Use this to import (deciphers) the xmlKey or xmlQos
    * @param dataHolder A container holding the MsgUnitRaw and some additional informations
    * @return The original or modified message
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown i.e. if the message has been modified
    * @see #exportMessage(CryptContextHolder)
    */
   public I_ContextHolder importMessage(I_ContextHolder dataHolder) throws ServiceManagerException;

   /**
    * Encrypt, sign, seal an outgoing message. 
    * <p/>
    * Use this to export (encrypt) the xmlKey or xmlQos
    * @param dataHolder A container holding the MsgUnitRaw and some additional informations
    * @return The probably more secure string
    * @exception com.ethercis.servicemanager.exceptions.ServiceManagerException Thrown if the message cannot be processed
    * @see #importMessage(CryptContextHolder)
    */
   public I_ContextHolder exportMessage(I_ContextHolder dataHolder) throws ServiceManagerException;
}

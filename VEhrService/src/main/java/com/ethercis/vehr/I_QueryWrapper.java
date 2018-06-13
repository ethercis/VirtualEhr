/*
 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.util.Map;

/**
 * Created by christian on 5/30/2018.
 */
public interface I_QueryWrapper {


    Map connect(MethodName action, I_SessionClientProperties hdrprops, String path, MethodName method, Object... parameters) throws ServiceManagerException;

    I_QueryUnit query(I_QueryUnit qryunit, I_SessionClientProperties qryparms, I_SessionClientProperties hdrprops, String path, MethodName action, MethodName method) throws ServiceManagerException;

    boolean isConnectAction(String path, MethodName action, MethodName method);

    boolean isDisconnectAction(String path, MethodName action, MethodName method);
}

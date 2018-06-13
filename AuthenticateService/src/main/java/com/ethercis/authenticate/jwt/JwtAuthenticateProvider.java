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

package com.ethercis.authenticate.jwt;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

/**
 * Created by christian on 6/4/2018.
 */
public class JwtAuthenticateProvider {

    RunTimeSingleton singleton;

    public JwtAuthenticateProvider(RunTimeSingleton singleton) {
        this.singleton = singleton;
    }

    public void init() throws ServiceManagerException {
        JwtContext jwtContext = new JwtContext(singleton);
        singleton.addObjectEntry(Constants.JWT_CONTEXT, jwtContext);
    }
}

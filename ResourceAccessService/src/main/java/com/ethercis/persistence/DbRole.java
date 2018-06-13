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

package com.ethercis.persistence;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;

/**
 * Created by christian on 6/6/2018.
 */
public class DbRole {

    RunTimeSingleton runTimeSingleton;

    public DbRole(RunTimeSingleton runTimeSingleton) {
        this.runTimeSingleton = runTimeSingleton;
    }

    public String role(String subject, String principal) throws IllegalArgumentException{
        String role = subject;

        if (runTimeSingleton.getProperty().get(Constants.DB_SECURITY_PRINCIPAL_PRECEDENCE, false) && principal != null) {
            role = principal;
        }

        if (role == null){
            throw new IllegalArgumentException("Subject cannot be null unless a principal is provided");
        }

        return role;
    }
}

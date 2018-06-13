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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;

/**
 * Created by christian on 5/30/2018.
 */
public class AuthorizationScheme {

    public enum Mode {SESSION_MANAGED, JWT};

    protected RunTimeSingleton global;

    public AuthorizationScheme(RunTimeSingleton global) {
        this.global = global;
    }

    public Mode mode(){
        //check for server.security.policy.type
        String securityMode = global.getProperty().get(Constants.POLICY_TYPE_TAG, (String)null);
        Mode mode;
        switch (securityMode.toUpperCase()){
            case "SHIRO":
                mode = Mode.SESSION_MANAGED;
                break;
            case "JWT":
                mode = Mode.JWT;
                break;
            default:
                mode = Mode.SESSION_MANAGED;
                break;
        }

        return mode;
    }


}

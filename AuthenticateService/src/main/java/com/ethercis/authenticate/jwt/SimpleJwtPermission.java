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

import com.ethercis.authenticate.Permission;
import com.ethercis.servicemanager.common.def.MethodName;

import java.util.Map;

/**
 * Created by christian on 6/1/2018.
 */
public class SimpleJwtPermission extends Permission {

    public SimpleJwtPermission(String name, MethodName action, String objectName, String pattern, Map<String, String> parameters) {
        super(name, action, objectName, pattern, parameters);
    }

    public SimpleJwtPermission(String name){
        new SimpleJwtPermission(name, null, null, null, null);
    }
}

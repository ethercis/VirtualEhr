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

import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.security.I_Principal;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by christian on 6/6/2018.
 */
public class Principal {

    List<I_Principal> principals;

    public Principal(List<I_Principal> principals) {
        this.principals = principals;
    }

    public String toString(){
        List<String> principalAsString = new ArrayList<>();
        String result = null;

        if (principals != null) {
            for (I_Principal principal : principals) {
                principalAsString.add(principal.getName());
            }
            result =  String.join(",", principalAsString);
        }

        return result;

    }
}

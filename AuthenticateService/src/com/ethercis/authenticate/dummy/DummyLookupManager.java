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
//Copyright
package com.ethercis.authenticate.dummy;

import com.ethercis.authenticate.interfaces.I_LookupManager;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.security.I_Authenticate;

import java.util.List;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 5/13/2015.
 */
public class DummyLookupManager  implements I_LookupManager {
    private RunTimeSingleton global;
    private static final String ME = "LookupManager";


    public DummyLookupManager(RunTimeSingleton runTimeSingleton){
        this.global = runTimeSingleton;
    }

    @Override
    public List<I_Authenticate> findSubjectsByPrincipal(String principal) {
        return null;
    }
}

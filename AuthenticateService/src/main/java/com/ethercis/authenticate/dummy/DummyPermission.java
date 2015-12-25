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

import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.security.I_Permission;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 5/13/2015.
 */
public class DummyPermission implements I_Permission {
    @Override
    public boolean implies(I_Permission another) {
        return true;
    }

    @Override
    public boolean impliesParameters(I_Permission another) {
        return true;
    }

    @Override
    public String getName() {
        return "DummyPermission";
    }

    @Override
    public String getObjectName() {
        return "DummyObjectPermission";
    }

    @Override
    public String getFilter() {
        return null;
    }

    @Override
    public Map<String, List<String>> getParameters() {
        return null;
    }

    @Override
    public MethodName getAction() {
        return null;
    }

    @Override
    public List<String> getGranted() {
        return null;
    }

    @Override
    public List<String> getRevoked() {
        return null;
    }

    @Override
    public Pattern getPattern() {
        return null;
    }
}

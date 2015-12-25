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

import com.ethercis.servicemanager.common.security.I_Permission;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.security.I_Rights;
import com.ethercis.servicemanager.common.session.I_ContextHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 5/13/2015.
 */
public class DummyPrincipal implements I_Principal {

    @Override
    public boolean isAuthorized(I_ContextHolder holder) {
        return true;
    }

    @Override
    public boolean isPermissionAuthorized(String permissionname, I_ContextHolder holder) {
        return true;
    }

    @Override
    public boolean isAuthorized(String rightName, String object, String pattern) {
        return true;
    }

    @Override
    public String getName() {
        return "DummyPrincipal";
    }

    @Override
    public List<I_Permission> getPermissions() {
        I_Permission permission = new DummyPermission();
        List<I_Permission> permissions = new ArrayList<I_Permission>();
        permissions.add(permission);
        return permissions;
    }

    @Override
    public I_Rights getRights() {
        return null;
    }
}

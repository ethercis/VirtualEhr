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

package com.ethercis.authenticate;

import com.ethercis.servicemanager.common.interfaces.data.I_User;

import java.util.Date;

/**
 * Created by christian on 6/1/2018.
 */
public class User implements I_User {

    private String id;
    private String password;
    private String code;
    private Date lastlogin;

    public User(String id, String password, String code, Date lastlogin) {
        this.id = id;
        this.password = password;
        this.code = code;
        this.lastlogin = lastlogin;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public Date getLastLogin() {
        return lastlogin;
    }
}

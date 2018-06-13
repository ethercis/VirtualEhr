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

import com.ethercis.authenticate.Authenticate;
import com.ethercis.authenticate.User;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Principal;
import com.ethercis.servicemanager.common.session.I_ContextHolder;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Created by christian on 6/1/2018.
 */
public class JwtAuthenticate extends Authenticate implements I_JwtAuthenticate {

    private static final Logger log = LogManager.getLogger(Constants.LOGGER_SECURITY);
    private static final String ME = JwtAuthenticate.class.getName();

    private JwtContext jwtContext;
    private Jws<Claims> jws;

    public JwtAuthenticate(RunTimeSingleton glob) throws ServiceManagerException {
        this.global = glob;

        //get JwtContext from singleton
        jwtContext = (JwtContext) glob.getObjectEntry(Constants.JWT_CONTEXT);

        if (jwtContext == null) {
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Internal error, no JWT context configured");
        }

    }

    @Override
    public boolean isAuthorized(I_ContextHolder dataHolder) {
        return false;
    }

    @Override
    public boolean isAuthorized(String rightName, String object, String pattern) {
        return false;
    }

    @Override
    public String getUserId() {
        user = new User(jws.getBody().getSubject(), null, jws.getBody().getId(), null);
        return user.getId();
    }

    @Override
    public void setUserId(String jwt) {

    }

    @Override
    public boolean checkCredential(String jwt) throws ServiceManagerException {
        return (jws = jwtContext.checkCredential(jwt)) != null;
    }

    @Override
    public boolean checkCredential() throws ServiceManagerException {
        return false;
    }

    @Override
    public List<I_Principal> getPrincipals() {
        String roles = (String) jws.getBody().get(ROLE);

        if (roles != null && !roles.isEmpty()) {
            for (String role : roles.split(",")) {
                principals.add(new SimpleJwtPrincipal(role, null));
            }
        }
        else
            principals = null;

        return principals;
    }

    @Override
    public void release() {
        jws = null;
    }
}

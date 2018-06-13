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
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import io.jsonwebtoken.*;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Properties;

/**
 * Created by christian on 6/1/2018.
 */
public class JwtContext {

    private JwtParser jwtParser;
    private static final String ME = JwtContext.class.getName();
    private Jws<io.jsonwebtoken.Claims> claim;
    RunTimeSingleton global;

    public JwtContext(RunTimeSingleton global) throws ServiceManagerException {
        String secretKey;
        this.global = global;
        SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
        //get the JWT key
        secretKey = signature();

//        jwtParser = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(secretKey));
        jwtParser = Jwts.parser().setSigningKey((secretKey).getBytes());
    }


    public Jws<io.jsonwebtoken.Claims> checkCredential(String jwt) throws ServiceManagerException {
        Jws<io.jsonwebtoken.Claims> claimsJws;
        try {
            claimsJws = jwtParser.parseClaimsJws(jwt);
        } catch (MalformedJwtException me) {
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "JWT Malformed token");
        } catch (ExpiredJwtException ee) {
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "JWT Expired token");
        } catch (SignatureException se) {
            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "JWT Invalid Signature");
        }

        return claimsJws;
    }

    /**
     * for test purpose, make sure key encoding is consistent
     *
     * @param userId
     * @param role
     * @return
     */
    public String createToken(String key, String userId, String role) {
        SignatureAlgorithm sigAlg = SignatureAlgorithm.HS256;
        byte[] apiKeySecretBytes = key.getBytes();
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, sigAlg.getJcaName());

        JwtBuilder builder = Jwts.builder()
                .setSubject(userId)
                .signWith(sigAlg, signingKey);

        if (role != null)
            builder = builder.claim(I_JwtAuthenticate.ROLE, role);

        return builder.compact();
    }

    public String createToken(String userId, String role) throws ServiceManagerException {
        return createToken(signature(), userId, role);
    }

    private String signature() throws ServiceManagerException {
        String secretKey;

        if ((secretKey = (global.getProperty().get(Constants.JWT_KEY, (String) null))) == null) {
            try { //get it from a file
                if (global.getProperty().get(Constants.JWT_KEY_FILE_PATH, (String) null) != null) {
                    String filepath = global.getProperty().get(Constants.JWT_KEY_FILE_PATH, (String) null);
                    Properties properties = new Properties();
                    properties.load(new FileInputStream(new File(filepath)));
                    secretKey = properties.getProperty("key");
//                    algorithm = SignatureAlgorithm.forName(properties.getProperty("algorithm"));
                } else {
                    throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not find JWT key definition, no file path specified");
                }
            } catch (IOException e) {
                throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "Could not open JWT key definition file: " + e);
            }
        }
        return secretKey;
    }
}

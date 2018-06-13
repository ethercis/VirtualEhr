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

package com.ethercis.authenticate.shiro;

import com.ethercis.authenticate.Authenticate;
import com.ethercis.authenticate.jwt.I_JwtAuthenticate;
import com.ethercis.authenticate.jwt.JwtAuthenticate;
import com.ethercis.authenticate.jwt.JwtContext;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.security.I_Authenticate;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import junit.framework.TestCase;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.Key;
import java.util.Properties;

/**
 * Created by christian on 5/29/2018.
 */
public class ShiroAuthenticateTest extends TestCase {

    org.apache.shiro.mgt.SecurityManager securityManager;

    public void testCheckSimpleCredential() throws Exception {

        setUpSecurityManager("src/test/resources/authenticate.ini");

        Subject currentUser = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken( "guest", "guest");
        currentUser.login(token);

        assertTrue(currentUser.isAuthenticated());
    }

    public void testCheckJwtCredential() throws Exception {
        setUpSecurityManager("src/test/resources/jwt.ini");

        SignatureAlgorithm sigAlg = SignatureAlgorithm.HS256;
        byte[] apiKeySecretBytes = "secret".getBytes();
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, sigAlg.getJcaName());

        String jwt = Jwts.builder()
                .setSubject("John Doe")
                .signWith(sigAlg, signingKey)
                .claim(I_JwtAuthenticate.ROLE, "user")
                .compact();

        if (jwt == null) {
            return;
        }

//        jwt = jwt.substring(jwt.indexOf(" "));

        String username = Jwts.parser().setSigningKey("secret".getBytes())
                .parseClaimsJws(jwt).getBody().getSubject();

        assertEquals("John Doe", username);

        String role =  (String)Jwts.parser().setSigningKey("secret".getBytes())
                .parseClaimsJws(jwt).getBody().get(I_JwtAuthenticate.ROLE);


        assertEquals("user", role);

        Subject subject = SecurityUtils.getSubject();
        ThreadState subjectThreadState = new SubjectThreadState(subject);
//        JWTAuthenticationToken jwtAuthenticationToken = new JWTAuthenticationToken(username, null);
//        subject.login(jwtAuthenticationToken);

        assertTrue(subject.isAuthenticated());

    }

    public void testJwtAuthenticate() throws ServiceManagerException {

        RunTimeSingleton runTimeSingleton = RunTimeSingleton.instance();
        runTimeSingleton.getProperty().getProperties().put(Constants.JWT_KEY, "secret");

        JwtContext jwtContext = new JwtContext(runTimeSingleton);
        String jwt = jwtContext.createToken("John Doe", "user");

        runTimeSingleton.addObjectEntry(Constants.JWT_CONTEXT, jwtContext);

        I_Authenticate authenticate = Authenticate.newWrapper(runTimeSingleton, Constants.POLICY_JWT, null);

        assertTrue(authenticate.checkCredential(jwt));
        assertEquals("John Doe", authenticate.getUserId());
        assertEquals(1, authenticate.getPrincipals().size());
    }

    public void testJwtAuthenticate2() throws ServiceManagerException {

        RunTimeSingleton runTimeSingleton = RunTimeSingleton.instance();
        runTimeSingleton.getProperty().getProperties().put(Constants.JWT_KEY_FILE_PATH, "src/test/resources/jwt.cfg");

        JwtContext jwtContext = new JwtContext(runTimeSingleton);
        String jwt = jwtContext.createToken("joe", "user");

        runTimeSingleton.addObjectEntry(Constants.JWT_CONTEXT, jwtContext);

        I_Authenticate authenticate = Authenticate.newWrapper(runTimeSingleton, Constants.POLICY_JWT, null);

        assertTrue(authenticate.checkCredential(jwt));
    }

//    public void testJwtAuthenticateRob1() throws ServiceManagerException {
//
//        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE1MjgzNjI2ODUsImlhdCI6MTUyODM2MjA4NSwiaXNzIjoicWV3ZC5qd3QiLCJhcHBsaWNhdGlvbiI6InJpcHBsZS1hdXRoIiwidGltZW91dCI6NjAwLCJxZXdkIjoiNTAzYjM3MWQ3YTE5NmZhZjliYTdiN2I0OGNkMWU5ZGVkYTMxNmE5MmYzYTRkMzMyMzI5NWQ1MWRkYTgwNjFjODZhODQwODc5ZGVhYTI2NmQwODk1OGQ1MGI5NWFmMmNkMjEzNzhmMThiYTA4YzE1Nzc1Nzg1NTlkZGZlMTZjYmQ2MzUwM2I3YmIwNjAyZDQ2NTQ3YTg3YWZmM2I3YzZjZmFlYTMzZmFlNDcwNGU2YjFmYzRjMGI0YjI5MDQ1MDU3IiwibmhzTnVtYmVyIjoiOTk5OTk5OTAxNSIsImVtYWlsIjoiaGFzaGltLmJvb3RoQHJpcHBsZS5mb3VuZGF0aW9uIiwicm9sZSI6InBoclVzZXIiLCJ1aWQiOiJiMTI2ODQ1Ny0wMmQzLTQyMGMtOTU0Yy1mZTQzY2Y2ZDMyN2EiLCJvcGVuaWQiOnsianRpIjoiZmU1OWFmYWMtZWNlOC00OGI2LTk1NjMtZWFlNWVkYjNmOGY5IiwiZXhwIjoxNTI4MzYyMzg1LCJuYmYiOjAsImlhdCI6MTUyODM2MjA4NSwiaXNzIjoiaHR0cHM6Ly9rZXljbG9hay5kZXYxLnNpZ25pbi5uaHMudWsvY2ljYXV0aC9yZWFsbXMvTkhTIiwiYXVkIjoibGVlZHMtaGVsbSIsInN1YiI6Ijk3YTIxNWU0LWU0ZWQtNDRhYi04MmUyLWEzNGZjMzI0ZjNlMyIsInR5cCI6IklEIiwiYXpwIjoibGVlZHMtaGVsbSIsImF1dGhfdGltZSI6MTUyODM2MjA4NCwic2Vzc2lvbl9zdGF0ZSI6ImIxMjY4NDU3LTAyZDMtNDIwYy05NTRjLWZlNDNjZjZkMzI3YSIsImFjciI6IjEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJoYXNoaW0uYm9vdGhAcmlwcGxlLmZvdW5kYXRpb24iLCJlbWFpbCI6Imhhc2hpbS5ib290aEByaXBwbGUuZm91bmRhdGlvbiIsIm5oc051bWJlciI6Ijk5OTk5OTkwMTUiLCJpZF90b2tlbiI6ImV5SmhiR2NpT2lKU1V6STFOaUlzSW5SNWNDSWdPaUFpU2xkVUlpd2lhMmxrSWlBNklDSTNXR2xOZEhWR1NITjBNVmR0WVVwcFNYZEpZVlpKWmxKT2F6UTVTemxUTlhsV2EwdGhNa1paWkhBMEluMC5leUpxZEdraU9pSm1aVFU1WVdaaFl5MWxZMlU0TFRRNFlqWXRPVFUyTXkxbFlXVTFaV1JpTTJZNFpqa2lMQ0psZUhBaU9qRTFNamd6TmpJek9EVXNJbTVpWmlJNk1Dd2lhV0YwSWpveE5USTRNell5TURnMUxDSnBjM01pT2lKb2RIUndjem92TDJ0bGVXTnNiMkZyTG1SbGRqRXVjMmxuYm1sdUxtNW9jeTUxYXk5amFXTmhkWFJvTDNKbFlXeHRjeTlPU0ZNaUxDSmhkV1FpT2lKc1pXVmtjeTFvWld4dElpd2ljM1ZpSWpvaU9UZGhNakUxWlRRdFpUUmxaQzAwTkdGaUxUZ3laVEl0WVRNMFptTXpNalJtTTJVeklpd2lkSGx3SWpvaVNVUWlMQ0poZW5BaU9pSnNaV1ZrY3kxb1pXeHRJaXdpWVhWMGFGOTBhVzFsSWpveE5USTRNell5TURnMExDSnpaWE56YVc5dVgzTjBZWFJsSWpvaVlqRXlOamcwTlRjdE1ESmtNeTAwTWpCakxUazFOR010Wm1VME0yTm1ObVF6TWpkaElpd2lZV055SWpvaU1TSXNJbkJ5WldabGNuSmxaRjkxYzJWeWJtRnRaU0k2SW1oaGMyaHBiUzVpYjI5MGFFQnlhWEJ3YkdVdVptOTFibVJoZEdsdmJpSXNJbVZ0WVdsc0lqb2lhR0Z6YUdsdExtSnZiM1JvUUhKcGNIQnNaUzVtYjNWdVpHRjBhVzl1SWl3aWJtaHpUblZ0WW1WeUlqb2lPVGs1T1RrNU9UQXhOU0o5LlJRSzM3Q0V5cVhjbnE1X3RiZElReFRKbnAxLXlnYmxkcXZkemtzdUtBR3docGJkclppZ1NubDVfTkdZeElTWGgtaG1oRDB4eFJHMXN3X01LTzNmaHZaU0RoWlF6RmEwLVdwcFA1NEk4YjktcUVhbVljeGo3blh3TGdBYVVmVU9GWWI1b3ZuZ0t5ZVFJMklDa1NoODd2OTA0THM2Zk5MVXY4YU1jSEhqTkpwMS1Nc3Zkb1lfZHowa2puaEF0Qi02dU5jUlZDa3BaZ041aHBpOGF4VjBGdmNENEJ5alRZRzh5c2ZXazB6bkI4SGo1MGpBZDdUajY1Y0hkNG1MaWowWU1rQVJ5QTRVWURteEVBUjZ4dFJrbV9qVzFyaXYzeVZraHFFbVVNaFMtWm5KSmhLYUIzQjV5bXhtaEVVTFlaRjN6ZXBIMC1wcEYyekhKbktOUmZnbkU3dyJ9fQ.IcC9S5bmkjnM2HjXtw_6zBCsed2YlD7HhhQpuax4lcg";
//
//        RunTimeSingleton runTimeSingleton = RunTimeSingleton.instance();
//        runTimeSingleton.getProperty().getProperties().put(Constants.JWT_KEY_FILE_PATH, "src/test/resources/jwt-rob.cfg");
//
//        JwtContext jwtContext = new JwtContext(runTimeSingleton);
//
//        runTimeSingleton.addObjectEntry(Constants.JWT_CONTEXT, jwtContext);
//
//        I_Authenticate authenticate = Authenticate.newWrapper(runTimeSingleton, Constants.POLICY_JWT, null);
//
//        assertTrue(authenticate.checkCredential(jwt));
//    }


    void setUpSecurityManager(String inipath) throws FileNotFoundException {
        Ini configuration = new Ini();
        InputStream inputStream = new FileInputStream(inipath);
        configuration.load(inputStream);
        IniRealm iniRealm = new IniRealm(configuration);
        org.apache.shiro.mgt.SecurityManager securityManager = new DefaultSecurityManager(iniRealm);
        SecurityUtils.setSecurityManager(securityManager);
    }

}
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

    public void testJwtAuthenticateSignatureAlgorithm() throws ServiceManagerException {

        RunTimeSingleton runTimeSingleton = RunTimeSingleton.instance().getClone(new String[]{"-propertyFile", "src/test/resources/services.properties"});

        runTimeSingleton.getProperty().getProperties().put(Constants.JWT_KEY_FILE_PATH, "src/test/resources/jwt.cfg");

        JwtContext jwtContext = new JwtContext(runTimeSingleton);
        String jwt = jwtContext.createToken("joe", "user");

        runTimeSingleton.addObjectEntry(Constants.JWT_CONTEXT, jwtContext);

        I_Authenticate authenticate = Authenticate.newWrapper(runTimeSingleton, Constants.POLICY_JWT, null);

        assertTrue(authenticate.checkCredential(jwt));
    }


    public void testBujiOpenIDConnect() throws Exception {

        setUpSecurityManager("src/test/resources/authbuji.ini");

        Subject currentUser = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken( "guest", "guest");
        currentUser.login(token);

        assertTrue(currentUser.isAuthenticated());
    }


    void setUpSecurityManager(String inipath) throws FileNotFoundException {
        Ini configuration = new Ini();
        InputStream inputStream = new FileInputStream(inipath);
        configuration.load(inputStream);
        IniRealm iniRealm = new IniRealm(configuration);
        org.apache.shiro.mgt.SecurityManager securityManager = new DefaultSecurityManager(iniRealm);
        SecurityUtils.setSecurityManager(securityManager);
    }

}
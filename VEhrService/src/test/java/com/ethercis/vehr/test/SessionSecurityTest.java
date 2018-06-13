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

package com.ethercis.vehr.test;

import com.ethercis.authenticate.jwt.JwtContext;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.vehr.Launcher;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created by christian on 6/4/2018.
 */
public class SessionSecurityTest  {

    protected Launcher launcher = null;
    protected RunTimeSingleton global;
    protected String resourcesRootPath;
    protected final String httpPort = "8080";
    protected static String hostname = "localhost";

    String strCompositionId = "4e607cd7-1f21-4d16-b14b-4239f48b9b04";

    @Before
    public void setUp() throws Exception {
        launcher = new Launcher();
    }

    @After
    public void tearDown(){
        try {
            launcher.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetCompositionJwt() throws Exception {
        launcher.start(new String[]{
                "-propertyFile", "config/jwt_settings.properties",
                "-java_util_logging_config_file", "config/logging.properties",
                "-servicesFile", "config/services.xml",
                "-dialect", "EHRSCAPE",
//                "-server_port", httpPort,
//                "-server_host", hostname,
                "-debug", "true"
        });

        global = launcher.getGlobal();

        //postgres, admin, user
        JwtContext jwtContext = new JwtContext(global);

//        String tokenPostgres = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwb3N0Z3JlcyIsInJvbGVzIjoiYWRtaW4sIHVzZXIifQ.JHUs6UcwVPyUFA_uxxK8Oks1EsiEPFnMCrEJBPhWmX8";
        String tokenPostgres = jwtContext.createToken("postgres", "admin");
        //postgres without role
//        String tokenPostgresNoRole = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwb3N0Z3JlcyJ9.WrwCDRrDyq7x8ddz2da61xsMPe10a8NlE3IRBs6zLp0";
        String tokenPostgresNoRole = jwtContext.createToken("postgres", null);
        //John Doe, roles: admin, user
//        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKb2huIERvZSIsInJvbGVzIjoiYWRtaW4sIHVzZXIifQ.ywclAEbAUNRH-ta7upa6LTP7eOKY32dz8M6VDnK0IWc";

        //joe, user
//        String tokenJoe = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2UiLCJyb2xlcyI6InVzZXIifQ.wUBTjH4okpoHJSGz41lrZHtE6nYW6ta6tJSixjVsMCU";
        String tokenJoe = jwtContext.createToken("joe", "dummy");
//        String token = "eyJhbGciOddiJIUzI1NiJ9.eyJzdWIiOiJKb2huIERvZSIsInJvbGVzIjoiYWRtaW4sIHVzZXIifQ.ywclAEbAUNRH-ta7upa6LTP7eOKY32dz8M6VDnK0IWc";


        HttpClient client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(3000); // 3 seconds timeout; if no server reply, the request expires

        client.start();

        ContentResponse response;

        Request request = client.newRequest("https://localhost:8443/rest/v1/composition/"+strCompositionId+"?format=ECISFLAT");
        request.method(HttpMethod.GET);
        request.header(HttpHeader.AUTHORIZATION, "Bearer "+tokenJoe);
        response = request.send();

        //get the session id from the header
        assertEquals(403, response.getStatus());

        if (response.getStatus()==200){
            System.out.println(response.getContentAsString());
        }
        else {
            System.out.println(response.getContentAsString());
//            fail("Could not complete query");
        }

        request = client.newRequest("https://localhost:8443/rest/v1/composition/"+strCompositionId+"?format=ECISFLAT");
        request.method(HttpMethod.GET);
        request.header(HttpHeader.AUTHORIZATION, "Bearer "+tokenPostgresNoRole);
        response = request.send();

        //get the session id from the header
        assertEquals(200, response.getStatus());

        if (response.getStatus()==200){
            System.out.println(response.getContentAsString());
        }
        else {
            System.out.println(response.getContentAsString());
            fail("Could not complete query");
        }

    }

    @Test
    public void testCompositionHandling() throws Exception {
        launcher.start(new String[]{
                "-propertyFile", "config/services.properties",
                "-java_util_logging_config_file", "config/logging.properties",
                "-servicesFile", "config/services.xml",
                "-dialect", "EHRSCAPE",
//                "-server_port", httpPort,
//                "-server_host", hostname,
                "-debug", "true"
        });

        global = launcher.getGlobal();

        HttpClient client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(3000); // 3 seconds timeout; if no server reply, the request expires

        client.start();

//        String userId = "guest";
//        String password = "guest";

        String userId = "postgres";
        String password = "postgres";

        ContentResponse response;

        //login first!
        response = client.POST("http://" + hostname + ":" + httpPort + "/rest/v1/session?username=" + userId + "&password=" + password).send();

        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));


        //retrieve the composition
        Request request = client.newRequest("http://" + hostname + ":" + httpPort + "/rest/v1/composition?uid=" + strCompositionId + "&format=XML");
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);
        request.method(HttpMethod.GET);
        response = request.send();

        assertNotNull(response);
        //output the content
        System.out.println(response.getContentAsString());
    }

}

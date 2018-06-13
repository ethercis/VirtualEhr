//Copyright
package com.ethercis.vehr;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import junit.framework.TestCase;
import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;

public class LauncherTest {

    static Launcher launcher = new Launcher();
//    HttpClient client;

    @BeforeClass
    public static void startServer() throws Exception {
        launcher.start(new String[]{
                "-propertyFile", "config/services.properties",
                "-java_util_logging_config_file", "config/logging.properties",
                "-servicesFile", "config/services.xml",
//                "-dialect", "EHRSCAPE",
//                "-server_port", "8080",
//                "-server_host", "localhost",
                "-debug", "true"
        });
//launcher.start(new String[]{
//                "-propertyFile", "test/resources/config/services.properties",
//                "-java_util_logging_config_file", "test/resources/config/logging.properties",
//                "-servicesFile", "test/resources/config/services.xml",
//                "-dialect", "EHRSCAPE",
//                "-server_port", "8080",
//                "-server_host", "localhost",
//                "-debug", "true"
//        });
    }


    @Test
    public void testConnectAndDisconnectSSL() throws Exception {
        HttpClient client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(3000); // 3 seconds timeout; if no server reply, the request expires

        client.start();

        String userId = "guest";
        String password = "guest";

        ContentResponse response;

        response = client.POST("https://localhost:8443/rest/v1/session?username=" + userId + "&password=" + password).send();

        //get the session id from the header
        assertNotNull(response);


        //simulate a DELETE session on the server side
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));;
        System.out.println("Connection successful with SSID:"+sessionId);

        Request request = client.newRequest("https://localhost:8443/rest/v1/session?sessionId="+sessionId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);

        request.method(HttpMethod.DELETE);
        response = request.send();

        assertNotNull(response);

    }

    @Test
    public void testConnectAndDisconnectHttp() throws Exception {
        HttpClient client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(3000); // 3 seconds timeout; if no server reply, the request expires

        client.start();

        String userId = "guest";
        String password = "guest";

        ContentResponse response;

        response = client.POST("http://localhost:8080/rest/v1/session?username=" + userId + "&password=" + password).send();

        //get the session id from the header
        assertNotNull(response);


        //simulate a DELETE session on the server side
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));;
        System.out.println("Connection successful with SSID:"+sessionId);

        Request request = client.newRequest("http://localhost:8080/rest/v1/session?sessionId="+sessionId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);

        request.method(HttpMethod.DELETE);
        response = request.send();

        assertNotNull(response);

    }


    @Test
    public void testJwtHttp() throws Exception {
        //John Doe, roles: admin, user
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKb2huIERvZSIsInJvbGVzIjoiYWRtaW4sIHVzZXIifQ.ywclAEbAUNRH-ta7upa6LTP7eOKY32dz8M6VDnK0IWc";

        HttpClient client = new HttpClient(new SslContextFactory(true));
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(3000); // 3 seconds timeout; if no server reply, the request expires

        client.start();

        ContentResponse response;

        Request request = client.newRequest("http://localhost:8080/rest/v1/template");
        request.method(HttpMethod.GET);
        request.header(HttpHeader.AUTHORIZATION, "Bearer "+token);
        response = request.send();

        //get the session id from the header
        assertNotNull(response);
    }

    @AfterClass
    static public void stopServer() throws Exception {
        launcher.stop();
    }

    public String sessionId(String responseBody){
        Gson json = new GsonBuilder().create();
        Map<String, Object> responseMap = json.fromJson(responseBody, Map.class);
        return (String) responseMap.get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));
    }
}
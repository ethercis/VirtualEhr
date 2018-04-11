//Copyright
package com.ethercis.vehr;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import junit.framework.TestCase;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class LauncherTest extends TestCase {

    Launcher launcher = new Launcher();
    HttpClient client;

    @Before
    public void setUp() throws Exception {
        launcher.start(new String[]{
                "-propertyFile", "config/services.properties",
                "-java_util_logging_config_file", "config/logging.properties",
                "-servicesFile", "config/services.xml",
                "-dialect", "EHRSCAPE",
                "-server_port", "8080",
                "-server_host", "localhost",
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

        client = new HttpClient();
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(3000); // 3 seconds timeout; if no server reply, the request expires
        client.start();
    }


    @Test
    public void testConnectAndDisconnect() throws Exception {
        String userId = "guest";
        String password = "guest";

        ContentResponse response;

//        ContentResponse response = client.GET("http://localhost:8080/vehr/connect?user=" + userId + "&password=" + password+"&x-max-session="+10);
        response = client.POST("http://localhost:8080/rest/v1/session?username=" + userId + "&password=" + password).send();

        //get the session id from the header
        assertNotNull(response);

        //simulate a DELETE session on the server side
        String sessionId = response.getHeaders().get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));;
        Request request = client.newRequest("http://localhost:8080/rest/v1/session?sessionId="+sessionId);
        request.header(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE), sessionId);

        request.method(HttpMethod.DELETE);
        response = request.send();

        assertNotNull(response);

//        System.out.println(new String(content));
//
//        assertNotNull(new String(content));
//        assertNotNull(request.getHeaders().getField(I_SessionManager.SECRET_SESSION_ID));


    }

    @After
    public void tearDown() throws Exception {
//        launcher.stop();
    }

    public String sessionId(String responseBody){
        Gson json = new GsonBuilder().create();
        Map<String, Object> responseMap = json.fromJson(responseBody, Map.class);
        return (String) responseMap.get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));
    }
}
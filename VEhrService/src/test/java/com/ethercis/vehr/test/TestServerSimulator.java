//Copyright
package com.ethercis.vehr.test;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.vehr.Launcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/2/2015.
 */
public abstract class TestServerSimulator extends TestCase {

    protected Launcher launcher = new Launcher();
    protected HttpClient client;
    protected RunTimeSingleton global;
    protected String resourcesRootPath;
    protected final String httpPort = "8889";
    protected static String hostname = "localhost";

    @Before
    public void setUp() throws Exception {
//        launcher.start(new String[]{
//                "-propertyFile", "resources/services.properties",
//                "-java_util_logging_config_file", "resources/logging.properties",
//                "-servicesFile", "resources/services.xml",
//                "-dialect", "EHRSCAPE",
//                "-server_port", "8080",
//                "-server_host", "localhost",
//                "-debug", "true"
//        });

        launcher.start(new String[]{
            "-propertyFile", "config/services.properties",
            "-java_util_logging_config_file", "config/logging.properties",
            "-servicesFile", "config/services.xml",
            "-dialect", "EHRSCAPE",
            "-server_port", httpPort,
            "-server_host", hostname,
            "-debug", "true"
        });

        setResourcesRootPath();

        global = launcher.getGlobal();

        client = new HttpClient();
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(30000); // 30 seconds timeout; if no server reply, the request expires
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        //stop the client
        client.stop();
        launcher.stop();
    }


    protected void setResourcesRootPath() {
        resourcesRootPath = getClass()
            .getClassLoader()
            .getResource(".")
            .getFile();
    }

    public String sessionId(String responseBody){
        Gson json = new GsonBuilder().create();
        Map<String, Object> responseMap = json.fromJson(responseBody, Map.class);
        return (String) responseMap.get(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.EHRSCAPE));
    }

    public byte[] readXMLNoBOM(String filePath) throws IOException {
        //read in a template into a string
        Path path = Paths.get(filePath);
        String readin = FileUtils.readFileToString(path.toFile(), "UTF-8");
        //start at index == 1 to eliminate any residual XML BOM (byte order mark)!!! see http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
        return readin.substring(readin.indexOf("<")).getBytes();
    }
}

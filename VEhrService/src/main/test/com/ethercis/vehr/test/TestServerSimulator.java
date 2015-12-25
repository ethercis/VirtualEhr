//Copyright
package com.ethercis.vehr.test;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.vehr.Launcher;
import junit.framework.TestCase;
import org.eclipse.jetty.client.HttpClient;
import org.junit.Before;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/2/2015.
 */
public abstract class TestServerSimulator extends TestCase {

    protected Launcher launcher = new Launcher();
    protected HttpClient client;
    protected RunTimeSingleton global;

    @Before
    public void setUp() throws Exception {
        launcher.start(new String[]{
                "-propertyFile", "resources/services.properties",
                "-java_util_logging_config_file", "resources/logging.properties",
                "-servicesFile", "resources/services.xml",
                "-dialect", "EHRSCAPE",
                "-server_port", "8080",
                "-server_host", "localhost",
                "-debug", "true"
        });

        global = launcher.getGlobal();

        client = new HttpClient();
        client.setMaxConnectionsPerDestination(200); // max 200 concurrent connections to every address
        client.setConnectTimeout(30000); // 30 seconds timeout; if no server reply, the request expires
        client.start();
    }
}

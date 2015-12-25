//Copyright
package com.ethercis.resource;

import com.ethercis.servicemanager.cluster.I_SignalListener;
import com.ethercis.servicemanager.exceptions.I_ServiceManagerExceptionHandler;
import com.ethercis.servicemanager.runlevel.I_RunlevelListener;
import com.ethercis.servicemanager.service.test.TestServiceBase;
import org.junit.Before;
import org.junit.Test;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 6/30/2015.
 */
public class ResourceAccessServiceTest extends TestServiceBase implements I_RunlevelListener, I_SignalListener, I_ServiceManagerExceptionHandler {

    private boolean initialized = false;
    private static final long serialVersionUID = 4350753857375153407L;

    @Before
    public void setUp() throws Exception {
        if (initialized) return;
        super.init(new String[]{
                "-propertyFile", "resources/services.properties"
        });
        initialized = true;
    }

    @Test
    public void testInitialized(){
        assertTrue(initialized);
    }

}

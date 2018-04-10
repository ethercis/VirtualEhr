//Copyright
package com.ethercis.systemservice;

import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.service.test.TestServiceBase;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
public class SystemServiceTest extends TestServiceBase {

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "services.properties"
        });
    }

    @Test
    public void testAccess() throws Exception {
        //create a new system
        I_SystemService systemService = ClusterInfo.getRegisteredService(global, "SystemService", "1.0", new Object[]{null});

        UUID id = systemService.create("test system", "44-87-FC-A9-B4-B2|TEST-PC");

        //retrieve
        UUID retrieved = systemService.retrieve("44-87-FC-A9-B4-B2|TEST-PC");

        assertEquals(id, retrieved);

        //delete
        Integer result = systemService.delete(id);

        assertTrue(result > 0);
    }

}

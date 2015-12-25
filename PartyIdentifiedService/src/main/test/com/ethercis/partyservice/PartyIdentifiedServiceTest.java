/*
 * Copyright (c) 2015 Christian Chevalley
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

//Copyright
package com.ethercis.partyservice;

import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.test.TestServiceBase;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
public class PartyIdentifiedServiceTest extends TestServiceBase {

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "resources/services.properties"
        });
    }

    @Test
    public void testRetrieveParty() throws ServiceManagerException {

        I_PartyIdentifiedService partyIdentifiedService = ClusterInfo.getRegisteredService(global, "PartyIdentifiedService", "1.0", new Object[] {null});

        if (partyIdentifiedService == null){
            fail("Could not retrieve partyIdentifiedService, check configuration and CLASSPATH");
        }

        //create a dummy identified party
        UUID uuid = partyIdentifiedService.getOrCreateParty("testParty", "99999-1234", "test issuer", "test assigner", "test type");

        assertNotNull(uuid);

        UUID retrieved = partyIdentifiedService.retrievePartyByIdentifier("99999-1234", "test issuer");

        assertEquals(uuid, retrieved);

        //delete
        Integer result = partyIdentifiedService.deleteParty(retrieved);

        assertTrue(result > 0);
    }

}

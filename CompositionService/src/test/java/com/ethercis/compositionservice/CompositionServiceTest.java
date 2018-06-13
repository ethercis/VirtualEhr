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

package com.ethercis.compositionservice;

import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.service.test.TestServiceBase;
import junit.framework.TestCase;
import org.junit.Before;

/**
 * Created by christian on 6/11/2018.
 */
public class CompositionServiceTest extends TestServiceBase {

    @Before
    public void setUp() throws Exception {
        super.init(new String[]{
                "-propertyFile", "src/test/resources/config/services.properties"
        });
    }


    public void testRetrieve() throws Exception {

        CompositionService compositionService = ClusterInfo.getRegisteredService(global, "CompositionService", "1.0", new Object[]{null});

        assertNotNull(compositionService);

        //perform a retrieve

        SessionClientProperties sessionClientProperties = new SessionClientProperties(global);

        sessionClientProperties.addClientProperty("uid", "001081d0-9ced-4ce6-bc1d-431895cad08c");
        sessionClientProperties.addClientProperty("format", "FLAT");
        sessionClientProperties.addClientProperty("Ehr-Session", "session-id");
        sessionClientProperties.addClientProperty("x-bypass-credential", "true");
        sessionClientProperties.addClientProperty("x-client-ip", "localhost");

        Object retval = compositionService.retrieve(sessionClientProperties);

        assertNotNull(retval);

    }
}
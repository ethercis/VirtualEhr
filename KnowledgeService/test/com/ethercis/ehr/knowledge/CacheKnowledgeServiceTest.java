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
package com.ethercis.ehr.knowledge;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.common.TestService;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import openEHR.v1.template.TEMPLATE;
import org.junit.Before;
import org.junit.Test;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CacheKnowledgeServiceTest {

	
	RunTimeSingleton controller;
	
	@Before
	public void setUp() throws Exception {
		controller = RunTimeSingleton.instance();
		CacheKnowledgeService service = new CacheKnowledgeService();
		TestService.setnstart(service, controller, "CacheKnowledgeService", "1.0",
//		new String[][] {
//				{"knowledge.path.archetype", "resources/knowledge/archetype_include"},
//				{"knowledge.path.template", "resources/knowledge/template"},
//                {"knowledge.path.opt", "resources/knowledge/opt"},
//                {"knowledge.forcecache", "true"}
//        });
        new String[][] {
                {"knowledge.path.archetype", "/Development/Dropbox/eCIS_Development/knowledge/production/archetypes"},
                {"knowledge.path.template", "/Development/Dropbox/eCIS_Development/knowledge/production/templates"},
                {"knowledge.path.opt", "/Development/Dropbox/eCIS_Development/knowledge/production/operational_templates"},
                {"knowledge.forcecache", "false"}
        });
	}

//    @Test
    public void testForceLoad() throws ServiceManagerException {
        I_CacheKnowledgeService service = ClusterInfo.getRegisteredService(controller, "CacheKnowledgeService", "1.0", new Object[] {null});

        I_KnowledgeCache knowledge = service.getKnowledgeCache();

    }

//    @Test
    public void testJMX() throws ServiceManagerException {
        I_CacheKnowledgeService service = ClusterInfo.getRegisteredService(controller, "CacheKnowledgeService", "1.0", new Object[] {null});

        I_KnowledgeCache knowledge = service.getKnowledgeCache();

        //suspend while a key is pressed
        try {
            System.out.println("Press  to continue...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	@Test
	public void testOPTRetrieval() throws Exception {
		I_CacheKnowledgeService service = ClusterInfo.getRegisteredService(controller, "CacheKnowledgeService", "1.0", new Object[] {null});
		
		I_KnowledgeCache knowledge = service.getKnowledgeCache();

        OPERATIONALTEMPLATE operationaltemplate = knowledge.retrieveOperationalTemplate("prescription");
        assertNotNull(operationaltemplate);

        UUID uuid = UUID.fromString(operationaltemplate.getUid().getValue());

        OPERATIONALTEMPLATE opt2 = knowledge.retrieveOperationalTemplate(uuid);

        assertEquals(operationaltemplate, opt2);
        //test recursive scan of archetypes

	}

    @Test
    public  void testOETRetrieval() throws Exception {
        I_CacheKnowledgeService service = ClusterInfo.getRegisteredService(controller, "CacheKnowledgeService", "1.0", new Object[] {null});

        I_KnowledgeCache knowledge = service.getKnowledgeCache();

        TEMPLATE template = knowledge.retrieveOpenehrTemplate("Tobacco use");

        assertNotNull(template);

        TEMPLATE template2 = knowledge.retrieveTemplate(UUID.fromString(template.getId()));

        assertEquals(template, template2);
    }

//    @Test
    public  void _testStats() throws Exception {
        I_CacheKnowledgeService service = ClusterInfo.getRegisteredService(controller, "CacheKnowledgeService", "1.0", new Object[] {null});

        I_KnowledgeCache knowledge = service.getKnowledgeCache();

        System.out.print(knowledge.archeypesList());
        System.out.print(knowledge.oetList());
        System.out.print(knowledge.optList());
    }

    @Test
    public void _testReload() throws ServiceManagerException {
        I_CacheKnowledgeService service = ClusterInfo.getRegisteredService(controller, "CacheKnowledgeService", "1.0", new Object[] {null});

        I_KnowledgeCache knowledge = service.getKnowledgeCache();

        String atlist = knowledge.archeypesList();
        String oetlist = knowledge.oetList();
        String optlist = knowledge.optList();

        ((CacheKnowledgeService)service).reload();

        assertEquals(atlist, knowledge.archeypesList());
        assertEquals(oetlist, knowledge.oetList());
        assertEquals(optlist, knowledge.optList());

    }

}

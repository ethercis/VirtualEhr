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
package com.ethercis.logonservice;

import com.ethercis.logonservice.security.ServiceSecurityManager;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.logonservice.session.SessionInfo;
import com.ethercis.logonservice.session.SessionName;
import com.ethercis.logonservice.session.SessionProperties;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.NodeId;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.TestService;
import com.ethercis.servicemanager.common.session.I_SessionInfo;
import com.ethercis.servicemanager.common.session.I_SessionProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LogonServiceTest {
	static Logger logger = LogManager.getLogger("com.ethercis");
    RunTimeSingleton controller;
    LogonService logonService;
    ServiceSecurityManager manager;

	@Before
	public void setUp() throws Exception {
        //create a new Authenticate instance
        controller = RunTimeSingleton.instance();

        //load the security plugin
        manager = new ServiceSecurityManager();
        TestService.setnstart(manager, controller, "ServiceSecurityManager", "1.0",
                new String[][]{
                        {"server.security.policy.type", "SHIRO"},
                        {"server.security.shiro.inipath", "./src/test/resources/authenticate.ini"}
                }
        );


        //create a logon service:
        logonService = new LogonService();
        TestService.setnstart(logonService, controller, "LogonService", "1.0", new String[][]{});

	}


    @After
    public void tearDown() throws ServiceManagerException {
        //stop gracefully the services
        manager.shutdown();
        logonService.shutdown();
    }
	
	@Test
	public void testLogonProperties() throws ServiceManagerException {
		SessionClientProperties props = new SessionClientProperties(controller);
		
		//simple login for user Joe
		props.addClientProperty(I_SessionManager.USER_ID, "guest");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "guest");
		
		SessionClientProperties retprops = logonService.connect(props).getSessionClientProperties();
		
		logger.debug(" logonsrv.connect = " + props);
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//disconnect
		SessionClientProperties loprops = new SessionClientProperties(controller);
		loprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		logonService.disconnect(loprops);
		
		//we should be disconnected...
		try {
            SessionInfo sessioninfo = logonService.check(sessionid);
            fail("Should have raised an exception...");
		} catch (ServiceManagerException e){
			System.out.println("all good...");
		}
	}

    @Test
    public void testUnsecureLogon() throws ServiceManagerException {
        I_SessionProperties props = new SessionProperties(controller, new NodeId("test-server"));

        //simple login for user Joe
        props.setSessionName(new SessionName(controller, "test-session"));

        I_SessionInfo sessionInfo = logonService.unsecureCreateSession(props);

        assertEquals("/node/ehrserver/client/test-session/-1", sessionInfo.getSessionName().getAbsoluteName());

        String secretSessionId = sessionInfo.getSecretSessionId();

        assertNotNull(secretSessionId);
    }

}

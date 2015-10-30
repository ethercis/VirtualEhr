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
package com.ethercis.servicemanager.service.test;

import com.ethercis.servicemanager.cluster.I_SignalListener;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.exceptions.I_ServiceManagerExceptionHandler;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_RunlevelListener;
import com.ethercis.servicemanager.runlevel.RunlevelManager;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Extend this class to simulate runtime loading of services
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/1/2015.
 */
public abstract class TestServiceBase extends TestCase implements I_RunlevelListener, I_SignalListener, I_ServiceManagerExceptionHandler {

    protected long startupTime;
    protected RunlevelManager runlevelManager = null;
    protected RunTimeSingleton global;
    protected static Logger log = Logger.getLogger("TestService");

    @Override
    public void runlevelChange(int from, int to, boolean force) throws ServiceManagerException {

    }

    @Override
    public void newException(ServiceManagerException e) {

    }

    @Override
    public void shutdownHook() {

    }

    /**
     * this method should be called to simulate a command line server startup. Loads all service according
     * to the runtime level. NB. All services to be loaded must be in the CLASSPATH
     * @param args
     */
    protected void init(String[] args) {

        log.info("Starting VEhrGate...");
        this.startupTime = System.currentTimeMillis();

        global = RunTimeSingleton.instance().getClone(args);

        log.debug("Initialize ...");

        initSystemProperties();

        if (ServiceManagerException.getExceptionHandler() == null)
            ServiceManagerException.setExceptionHandler(this);

        int runlevel = global.getProperty().get("runlevel", RunlevelManager.RUNLEVEL_RUNNING);
        try {
            runlevelManager = global.getRunlevelManager();
            runlevelManager.addRunlevelListener(this);
            runlevelManager.initServiceManagers();
            runlevelManager.changeRunlevel(runlevel, false);
        } catch (Throwable e) {
            if (e instanceof ServiceManagerException) {
                log.error(e.getMessage());
            } else {
                e.printStackTrace();
                log.error(e.toString());
            }

            log.error("Changing runlevel to '"
                    + RunlevelManager.toRunlevelStr(runlevel)
                    + "' failed, good bye");
            System.exit(1);
        }

    }

    static protected final void initSystemProperties() {
        Properties props = System.getProperties();

        if (props.size() > 0) {
            System.setProperties(props);
        }
    }
}

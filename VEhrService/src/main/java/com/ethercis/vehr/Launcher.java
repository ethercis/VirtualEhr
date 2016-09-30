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
package com.ethercis.vehr;


import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;


/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 5/12/2015.
 */
public class Launcher  {

    public static String command_server_port = "server_port";
    public static String command_server_host = "server_host";
    public static String default_port = "8080";
    public static String default_host = "localhost";

    static Options options = new Options();
    static Logger logger = LogManager.getLogger(Launcher.class);

    static {
        options.addOption(command_server_port, true, "port number to bind service to");
        options.addOption(command_server_host, true, "host id to bind service to");
        options.addOption("propertyFile", true, "global server properties");
        options.addOption("java_util_logging_config_file", true, "settings for logging");
        options.addOption("servicesFile", true, "services configuration file (XML)");
        options.addOption("dialect", true, "STANDARD");
        options.addOption("debug", true, "if set, start/stop the server in debug mode (should be used only for test units");

    }

    private static VEhrGateServlet vEhrGateServlet;
    private static Server server;

    public static void startServer(String[] args) throws Exception {
        new Launcher().start(args);
    }

    public void start(String[] args) throws Exception {
        boolean debug = false;
        List<String> normalizedArguments = new ArrayList<>(Arrays.asList(args));

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption("debug")) debug = true;

        if (commandLine.hasOption("dialect")){
            String compatibilityValue = commandLine.getOptionValue("dialect", "STANDARD");
            //TODO: add compatibility argument in args
            normalizedArguments.add("-"+I_ServiceRunMode.SERVER_DIALECT_PARAMETER);
            normalizedArguments.add(I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue).name());
        }

        Integer httpPort = Integer.parseInt(commandLine.getOptionValue(command_server_port, default_port));
        String httpHost = commandLine.getOptionValue(command_server_host, default_host);

        InetSocketAddress socketAddress = new InetSocketAddress(httpHost, httpPort);

        logger.info("Server starting on host:"+httpHost+" port:"+httpPort);

        server = new Server(socketAddress);
        if (debug)
            server.setStopAtShutdown(true);
        //create and initialize vEhrHandler
        vEhrGateServlet = new VEhrGateServlet();
        vEhrGateServlet.init(normalizedArguments.toArray(new String[] {}));

        ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);


        //add filter to allow CORS
        FilterHolder cors = new FilterHolder(CrossOriginFilter.class);
//        FilterHolder cors = servletContextHandler.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, null);
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET, POST, DELETE, PUT, OPTIONS, HEAD");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
        cors.setName("cross-origin");
        FilterMapping filterMapping = new FilterMapping();
        filterMapping.setFilterName("cross-origin");
        filterMapping.setPathSpec("*");
        servletContextHandler.addFilter(cors, "/*", EnumSet.allOf(DispatcherType.class));
//        servletContextHandler.addFilter(cors, "/*", null);
//        servletContextHandler.getServletHandler().addFilter(cors, filterMapping);

        ServletHolder servletHolder = new ServletHolder(vEhrGateServlet);
        servletContextHandler.addServlet(servletHolder, "/");

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[]{servletContextHandler, new DefaultHandler()});
        server.setHandler(handlerList);

        try {
            server.start();
        } catch (BindException e){
            logger.error("Could not bind to network interface ("+socketAddress.toString()+"), exception:"+e);
            throw new IllegalArgumentException("Could not bind to network interface  ("+socketAddress.toString()+"), exception:"+e);
        }
        logger.info("Server listening at:" + server.getURI().toString());
        if (!debug)
            server.join();

    }

    public void stop() throws Exception {
        server.stop();
        logger.info("Server successfully terminated...");
    }

    public RunTimeSingleton getGlobal(){
        return vEhrGateServlet.getGlobal();
    }

    public static void main(String[] args) throws Exception {
        startServer(args);
    }
}

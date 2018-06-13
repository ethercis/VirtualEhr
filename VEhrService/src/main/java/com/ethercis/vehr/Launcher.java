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
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import javax.net.ssl.SSLEngine;
import javax.servlet.DispatcherType;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;


/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 5/12/2015.
 */
public class Launcher implements I_HttpServiceConfiguration {

    public static String command_server_port = "server_port";
    public static String command_server_host = "server_host";
    public static String default_port = "8080";
    public static String default_host = "localhost";

    public static String allowHeaders = "Accept, Accept-CH, Accept-Charset, Accept-Datetime, " +
            "Accept-Encoding, Accept-Ext, Accept-Features, Accept-Language, " +
            "Accept-Params, Accept-Ranges, Access-Control-Allow-Credentials, " +
            "Access-Control-Allow-Headers, Access-Control-Allow-Methods, Access-Control-Allow-Origin, " +
            "Access-Control-Expose-Headers, Access-Control-Max-Age, Access-Control-Request-Headers, " +
            "Access-Control-Request-Method, Age, Allow, Alternates, Authentication-Info, " +
            "Authorization, C-Ext, C-Man, C-Opt, C-PEP, C-PEP-Info, CONNECT, Cache-Control, Compliance, " +
            "Connection, Content-Base, Content-Disposition, Content-Encoding, Content-ID, Content-Language, " +
            "Content-Length, Content-Location, Content-MD5, Content-Range, Content-Script-Type, " +
            "Content-Security-Policy, Content-Style-Type, Content-Transfer-Encoding, Content-Type, Content-Version, " +
            "Cookie, Cost, DAV, DELETE, DNT, DPR, Date, Default-Style, Delta-Base, Depth, Derived-From, " +
            "Destination, Differential-ID, Digest, ETag, Expect, Expires, Ext, Ehr-Session, From, GET, GetProfile, " +
            "HEAD, HTTP-date, Host, IM, If, If-Match, If-Modified-Since, If-None-Match, If-Range, If-Unmodified-Since, " +
            "Keep-Alive, Label, Last-Event-ID, Last-Modified, Link, Location, Lock-Token, " +
            "MIME-Version, Man, Max-Forwards, Media-Range, Message-ID, Meter, Negotiate, Non-Compliance, OPTION, " +
            "OPTIONS, OWS, Opt, Optional, Ordering-Type, Origin, Overwrite, P3P, PEP, PICS-Label, POST, PUT, " +
            "Pep-Info, Permanent, Position, Pragma, ProfileObject, Protocol, Protocol-Query, Protocol-Request, " +
            "Proxy-Authenticate, Proxy-Authentication-Info, Proxy-Authorization, Proxy-Features, Proxy-Instruction, " +
            "Public, RWS, Range, Referer, Refresh, Resolution-Hint, Resolver-Location, Retry-After, Safe, " +
            "Sec-Websocket-Extensions, Sec-Websocket-Key, Sec-Websocket-Origin, Sec-Websocket-Protocol, " +
            "Sec-Websocket-Version, Security-Scheme, Server, Set-Cookie, Set-Cookie2, SetProfile, SoapAction, " +
            "Status, Status-URI, Strict-Transport-Security, SubOK, Subst, Surrogate-Capability, Surrogate-Control, " +
            "TCN, TE, TRACE, Timeout, Title, Trailer, Transfer-Encoding, UA-Color, UA-Media, UA-Pixels, UA-Resolution, " +
            "UA-Windowpixels, URI, Upgrade, User-Agent, Variant-Vary, Vary, Version, Via, Viewport-Width, WWW-Authenticate, " +
            "Want-Digest, Warning, Width, X-Content-Duration, X-Content-Security-Policy, X-Content-Type-Options, " +
            "X-CustomHeader, X-DNSPrefetch-Control, X-Forwarded-For, X-Forwarded-Port, X-Forwarded-Proto, X-Frame-Options, " +
            "X-Modified, X-OTHER, X-PING, X-PINGOTHER, X-Powered-By, X-Requested-With";

    String allowedMethods = "CONNECT, DEBUG, DELETE, DONE, GET, HEAD, HTTP, HTTP/0.9, HTTP/1.0, " +
            "HTTP/1.1, HTTP/2, OPTIONS, ORIGIN, ORIGINS, PATCH, " +
            "POST, PUT, QUIC, REST, SESSION, SHOULD, SPDY, TRACE, TRACK";

    static Options options = new Options();
    static Logger logger = LogManager.getLogger(Launcher.class);
    static RunTimeSingleton global = null;

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
        global = RunTimeSingleton.instance().getClone(args); //get parameters from services.properties
        List<String> normalizedArguments = new ArrayList<>(Arrays.asList(args));

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption("debug")) debug = true;

        if (commandLine.hasOption("dialect")) {
            String compatibilityValue = commandLine.getOptionValue("dialect", "STANDARD");
            //TODO: add compatibility argument in args
            normalizedArguments.add("-" + I_ServiceRunMode.SERVER_DIALECT_PARAMETER);
            normalizedArguments.add(I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue).name());
        }

        Integer httpPort = Integer.parseInt(global.get(I_HttpServiceConfiguration.SERVER_HTTP_PORT, commandLine.getOptionValue(command_server_port, default_port), null, null));
        String httpHost = global.get(I_HttpServiceConfiguration.SERVER_HTTP_HOST, commandLine.getOptionValue(command_server_host, default_host), null, null);

//        InetSocketAddress socketAddress = new InetSocketAddress(httpHost, httpPort);

        logger.info("Server starting on host:" + httpHost + " port:" + httpPort);

        // === jetty.xml ===
        Integer maxThreads = Integer.parseInt(global.get(I_HttpServiceConfiguration.SERVER_THREAD_POOL_MAX_THREADS, "500", null, null));

        // Setup Threadpool
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(maxThreads);

        // Server
        server = new Server(threadPool);

        // Scheduler
        server.addBean(new ScheduledExecutorScheduler());

        // Handler Structure
        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        handlers.setHandlers(new Handler[]{contexts, new DefaultHandler()});
        server.setHandler(handlers);

        // === jetty-jmx.xml ===
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_JMX)) {
            MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.addBean(mbContainer);
        }

        // HTTP
        HttpConfiguration http_config = httpConfiguration();

        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));

        http.setPort(httpPort);
        http.setHost(httpHost);
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_IDLE_TIMEOUT))
            http.setIdleTimeout(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_IDLE_TIMEOUT, (String) null)));

//        server = new Server(socketAddress);

        server.addConnector(http);

        //SSL Configuration
        SslContextFactory sslContextFactory = null;

        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_SECURE_SCHEME)) {
            sslContextFactory = sslContext();
            HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());

//            SslConnectionFactory sslConnection = new SslConnectionFactory(sslContextFactory, "alpn");
//            ALPNServerConnectionFactory alpnServerConnection = new ALPNServerConnectionFactory("http/1.1");
//            alpnServerConnection.setDefaultProtocol("http/1.1");

            ServerConnector sslConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(https_config));

            Integer httpsPort = Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_PORT, (String) null));

            sslConnector.setPort(httpsPort);

            if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_HOST))
                sslConnector.setHost(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_HOST, (String) null));

            server.addConnector(sslConnector);
        }

        //low resources monitor
        lowResourceMonitor();

        //stats
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_STATS)) {
            StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(server.getHandler());
            server.setHandler(stats);
            ServerConnectionStatistics.addToAllConnectors(server);
        }

        //request log
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG)) {
            RequestLogHandler requestLogHandler = requestLogHandler();
            handlers.addHandler(requestLogHandler);
        }

        if (debug)
            server.setStopAtShutdown(true);

        ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);

        //add filter to allow CORS
        FilterHolder cors = cors();
//        FilterMapping filterMapping = new FilterMapping();
//        filterMapping.setFilterName("cross-origin");
//        filterMapping.setPathSpec("*");
        servletContextHandler.addFilter(cors, "/*", EnumSet.allOf(DispatcherType.class));

        //create and initialize vEhrHandler
        vEhrGateServlet = new VEhrGateServlet();
        vEhrGateServlet.init(normalizedArguments.toArray(new String[]{}));

        ServletHolder servletHolder = new ServletHolder(vEhrGateServlet);
        servletContextHandler.addServlet(servletHolder, "/");

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[]{servletContextHandler, new DefaultHandler()});
        server.setHandler(handlerList);

        try {
            server.start();

        } catch (BindException e) {
            logger.error("Could not bind to network interface (" + httpHost + ":" + httpPort + "), exception:" + e);
            throw new IllegalArgumentException("Could not bind to network interface  (" + httpHost + ":" + httpPort + "), exception:" + e);
        }
        logger.info("Server listening at:" + server.getURI().toString());
        if (!debug)
            server.join();

    }

    public void stop() throws Exception {
        server.stop();
        logger.info("Server successfully terminated...");
    }

    public RunTimeSingleton getGlobal() {
        return vEhrGateServlet.getGlobal();
    }

    public static void main(String[] args) throws Exception {
        startServer(args);
    }

    HttpConfiguration httpConfiguration() {
        HttpConfiguration httpConfiguration = new HttpConfiguration();

        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_SECURE_SCHEME))
            httpConfiguration.setSecureScheme(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTP_SECURE_SCHEME, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_PORT))
            httpConfiguration.setSecurePort(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_PORT, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_OUTPUT_BUFFER_SIZE))
            httpConfiguration.setOutputBufferSize(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTP_OUTPUT_BUFFER_SIZE, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_REQUEST_HEADER_SIZE))
            httpConfiguration.setRequestHeaderSize(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTP_REQUEST_HEADER_SIZE, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_SET_RESPONSE_HEADER_SIZE))
            httpConfiguration.setResponseHeaderSize(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTP_SET_RESPONSE_HEADER_SIZE, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_SEND_SERVER_VERSION))
            httpConfiguration.setSendServerVersion(Boolean.parseBoolean(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTP_SEND_SERVER_VERSION, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTP_SEND_DATE_HEADER))
            httpConfiguration.setSendDateHeader(Boolean.parseBoolean(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTP_SEND_DATE_HEADER, (String) null)));

        return httpConfiguration;
    }

    SslContextFactory sslContext() {
        SslContextFactory sslContext = new SslContextFactory();
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_KEY_STORE_PATH))
            sslContext.setKeyStorePath(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_KEY_STORE_PATH, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_KEY_STORE_PASSWORD))
            sslContext.setKeyStorePassword(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_KEY_STORE_PASSWORD, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_KEY_MANAGER_PASSWORD))
            sslContext.setKeyManagerPassword(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_KEY_MANAGER_PASSWORD, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_TRUST_STORE_PATH))
            sslContext.setTrustStorePath(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_TRUST_STORE_PATH, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_TRUST_STORE_PASSWORD))
            sslContext.setTrustStorePassword(global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_TRUST_STORE_PASSWORD, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_HTTPS_EXCLUDE_CIPHER_SUITES)) {
            String cipherSuites = global.getProperty().get(I_HttpServiceConfiguration.SERVER_HTTPS_EXCLUDE_CIPHER_SUITES, (String) null);
            if (cipherSuites != null && !cipherSuites.isEmpty())
                sslContext.setExcludeCipherSuites(cipherSuites.split(","));
        }

//        if (sslContextFactory != null) {
//            SSLEngine sslEngine = sslContextFactory.newSSLEngine();
//            ALPN.put(sslEngine, new ALPN.ServerProvider() {
//                @Override
//                public void unsupported() {
//                    ALPN.remove(sslEngine);
//                }
//
//                @Override
//                public String select(List<String> protocols) {
//                    ALPN.remove(sslEngine);
//                    return protocols.get(0);
//                }
//
//            });
//        }

        return sslContext;
    }

    FilterHolder cors() throws ServiceManagerException {
        allowHeaders = global.get(I_HttpServiceConfiguration.SERVER_CORS_ALLOW_HEADERS, allowHeaders, null, null);
        allowedMethods = global.get(I_HttpServiceConfiguration.SERVER_CORS_ALLOW_METHODS, allowedMethods, null, null);

        FilterHolder cors = new FilterHolder(CrossOriginFilter.class);
//        FilterHolder cors = servletContextHandler.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, allowedMethods);
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowHeaders);
        cors.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM, allowHeaders);

        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER, allowHeaders);
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER, allowHeaders);
        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, allowedMethods);

        cors.setName("cross-origin");

        return cors;
    }

    LowResourceMonitor lowResourceMonitor() {

        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR)) {
            LowResourceMonitor lowResourcesMonitor = new LowResourceMonitor(server);
            if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_PERIOD))
                lowResourcesMonitor.setPeriod(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_PERIOD, (String) null)));
            if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_IDLE_TIMEOUT))
                lowResourcesMonitor.setLowResourcesIdleTimeout(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_IDLE_TIMEOUT, (String) null)));
            if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_THREADS))
                lowResourcesMonitor.setMonitorThreads(Boolean.parseBoolean(global.getProperty().get(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_THREADS, (String) null)));
            if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_MAX_CONNECTIONS))
                lowResourcesMonitor.setMaxConnections(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_MAX_CONNECTIONS, (String) null)));
            if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_MAX_LOW_RESOURCES_TIME))
                lowResourcesMonitor.setMaxLowResourcesTime(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_LOW_RESOURCES_MONITOR_MAX_LOW_RESOURCES_TIME, (String) null)));
            server.addBean(lowResourcesMonitor);
            return lowResourcesMonitor;
        }
        return null;
    }

    RequestLogHandler requestLogHandler() {
        NCSARequestLog requestLog = new NCSARequestLog();
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_FILENAME))
            requestLog.setFilename(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_FILENAME, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_FILENAME_DATE_FORMAT))
            requestLog.setFilenameDateFormat(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_FILENAME_DATE_FORMAT, (String) null));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_RETAIN_DAY))
            requestLog.setRetainDays(Integer.parseInt(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_RETAIN_DAY, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_APPEND))
            requestLog.setAppend(Boolean.parseBoolean(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_APPEND, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_EXTENDED))
            requestLog.setExtended(Boolean.parseBoolean(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_EXTENDED, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_LOG_COOKIES))
            requestLog.setLogCookies(Boolean.parseBoolean(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_LOG_COOKIES, (String) null)));
        if (global.getProperty().propertyExists(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_LOG_TIMEZONE))
            requestLog.setLogTimeZone(global.getProperty().get(I_HttpServiceConfiguration.SERVER_REQUEST_LOG_LOG_TIMEZONE, (String) null));

        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(requestLog);

        return requestLogHandler;
    }
}

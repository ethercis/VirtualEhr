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

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.logonservice.session.ResponseHolder;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.I_SignalListener;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.SignalCatcher;
import com.ethercis.servicemanager.common.*;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.I_ServiceManagerExceptionHandler;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_RunlevelListener;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.runlevel.RunlevelManager;
import com.ethercis.servicemanager.service.ServiceRegistry;
import com.ethercis.vehr.parser.I_URIParser;
import com.ethercis.vehr.response.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.ServletResponseHttpWrapper;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//use Jetty servlet-api

/**
 * Main entry point Servlet
 *
 * @author Christian Chevalley
 */

//TODO: separate in a service controller container and the actual vEhrService

public class VEhrGateServlet extends HttpServlet implements
        ServletContextListener, I_RunlevelListener, I_SignalListener,
        I_ServiceManagerExceptionHandler {

    /**
     *
     */
    private String ME = "VEhrGate";
    private boolean initialized = false;
    private static final long serialVersionUID = 4350753857375153407L;
    private RunTimeSingleton global = null;
    private static Logger log = LogManager.getLogger(VEhrGateServlet.class);
    private boolean isAsynchQuery = false;
    // private final String header =
    // "<html><meta http-equiv='no-cache'><meta http-equiv='Cache-Control' content='no-cache'><meta http-equiv='expires' content='Wed, 26 Feb 1997 08:21:57 GMT'>";

    public final static String ENCODING = "UTF-8";
    private static boolean propertyRead = false;
    /**
     * Incarnation time of this object instance in millis
     */
    private long startupTime;

    private String panicErrorCodes = SysErrorCode.RESOURCE_UNAVAILABLE
            .getErrorCode()
            + ","
            + SysErrorCode.RESOURCE_UNAVAILABLE.getErrorCode();

    /**
     * Starts/stops ehrserver
     */
    private RunlevelManager runlevelManager = null;

    // private boolean inShutdownProcess = false;
    private SignalCatcher signalCatcher;

    /**
     * the dispatcher for servicing all queries
     */
    private AccessGateService controller;

    /**
     * used to service asynchronous queries
     */
    private ExecutorService executor;
    private int threadPoolSize = 3;
    private int callback_timeout = 60000;

    //select the URI parser depending on the runtime dialect
    private I_URIParser uriParser;

    /**
     * This method is invoked only once when the servlet is started.
     * <p>
     * Parameters:
     * <p>
     * <ul>
     * <li>threadpoolsize: max number of threads in pool for servicing
     * asynchronous queries
     * </ul>
     *
     * @param conf initializeSession parameter of the servlet
     */
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);

        log.info("Starting VEhrGate...");
        this.startupTime = System.currentTimeMillis();

        if (!propertyRead) {
            propertyRead = true;
            // Add the web.xml parameters to our environment settings:
            Enumeration<String> enumer = conf.getInitParameterNames();
            int count = 0;
            while (enumer.hasMoreElements()) {
                if (enumer.nextElement() == null)
                    continue;
                count++;
            }
            String[] args = new String[2 * count];

            count = 0;
            enumer = conf.getInitParameterNames();
            while (enumer.hasMoreElements()) {
                String name = (String) enumer.nextElement();
                if (name == null)
                    continue;
                if (!name.startsWith("-"))
                    args[count++] = "-" + name;
                else
                    args[count++] = name;
                args[count++] = conf.getInitParameter(name);

            }

            if (global == null)
                global = RunTimeSingleton.instance().getClone(args);
            else
                global.init(args);

            if (getInitParameter("threadpoolsize") != null)
                threadPoolSize = Integer.parseInt(getInitParameter("threadpoolsize"));
            else { //check in services.properties
                threadPoolSize = global.getProperty().get("server.threadpoolsize", 10);
            }
            log.info("Servlet thread pool size:" + threadPoolSize);

            if (getInitParameter("callback_timeout") != null)
                callback_timeout = Integer.parseInt(getInitParameter("callback_timeout"));
            else {
                callback_timeout = global.getProperty().get("server.callback_timeout", 60000);
            }
            log.info("Servlet time out:" + callback_timeout + " [ms]");

            isAsynchQuery = global.getProperty().get("server.query.asynchronous", false);

            if (isAsynchQuery)
                log.info("Server mode is set to ASYNCHRONOUS");

            executor = Executors.newFixedThreadPool(threadPoolSize);
        }


        log.debug("Initialize ...");

        initSystemProperties();

        log.info("VEhrGate initializeSession complete");
    }

    public void init(String[] args) {

        if (initialized) return;

        log.info("Starting VEhrGate...");
        this.startupTime = System.currentTimeMillis();

        global = RunTimeSingleton.instance().getClone(args);

        //setup the dialect parser
        String compatibilityValue = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_PARAMETER, I_ServiceRunMode.DialectSpace.STANDARD.toString());
        I_ServiceRunMode.DialectSpace dialectSpace = I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue);

//        switch (dialectSpace){
//            case EHRSCAPE:
//                uriParser = new EhrScapeURIParser(global);
//                break;
//            default:
//                uriParser = new DefaultURIParser(global);
//                break;
//        }


        log.debug("Initialize ...");

        initSystemProperties();

        // initialize run level manager...
        // Add us logonservice an I_ServiceManagerExceptionHandler ... (done again in
        // changeRunlevel() below, but this is too late logonservice first JDBC access can
        // be in RL0
        if (ServiceManagerException.getExceptionHandler() == null)
            ServiceManagerException.setExceptionHandler(this); // see public void
        // newException(ServiceManagerException
        // e);

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

        // resolve the dispatcher (or throws an exception since the service
        // cannot be provided...)

        ServiceRegistry registry = global.getServiceRegistry();

        this.controller = (AccessGateService) registry.getService("AccessGateService,1.0");

        if (controller == null) {
            log.error("AccessGateService (Active Enforcement Point) service is not loaded, please make sure the service has been started");
            System.exit(-1);
        }

        //get the URI parser from the service registry
        try {
            uriParser = ClusterInfo.getRegisteredService(global, "URIParser", "1.0");
        } catch (Exception e) {
            log.error("Could not bind to URIParser, please make sure the service has been started");
            System.exit(-1);
        }

        if (uriParser == null) {
            log.error("Could not bind to URIParser, please make sure the service has been started");
            System.exit(-1);
        }

        initialized = true;
        log.info("VEhrGate initializeSession complete");
    }

    public void destroy() {
        log.info("Shutdown requested by context, closing down...");
        // tell the runlevel manager to shutdown gracefully the services...
        try {
            runlevelManager.changeRunlevel(RunlevelManager.RUNLEVEL_HALTED, true);
            controller.shutdown();
            log.debug("controller shutdown completed...");
            global.shutdown();
            log.debug("global shutdown completed...");
            if (signalCatcher != null)
                signalCatcher.removeSignalCatcher();
        } catch (ServiceManagerException e) {
            log.error("Shutdown could not be done properly with exception:" + e);
        }

        if (executor != null)
            executor.shutdown();

        log.info("Shutdown completed...");
    }

    /**
     * Setting the system properties.
     * <p>
     * These may be overwritten in zone.properties, e.g.
     * servlets.default.initArgs
     * =servlets.default.initArgs=org.xml.sax.parser=org
     * .apache.crimson.parser.Parser2
     * <p>
     * We set the properties to choose JacORB and Suns XML parser logonservice a default.
     */
    static public final void initSystemProperties() {
        Properties props = System.getProperties();

        if (props.size() > 0) {
            System.setProperties(props);
        }
    }

    /**
     * manage the change in run level (in particular during startup)
     */
    public void runlevelChange(int from, int to, boolean force)
            throws ServiceManagerException {
        // if (log.isLoggable(Level.debugR)) log.call(ME,
        // "Changing from run level=" + from + " to level=" + to +
        // " with force=" + force);
        if (to == from)
            return;

        if (to > from) { // startup
            // if (to == RunlevelManager.RUNLEVEL_HALTED) {
            // log.error(ME, "DEBUG ONLY ........");
            // if (glob.getNodeId() == null)
            // glob.setUniqueNodeIdName(createNodeId());
            // }
            if (to == RunlevelManager.RUNLEVEL_HALTED_POST) {
                this.startupTime = System.currentTimeMillis();
                boolean useSignalCatcher = global.getProperty().get(
                        "useSignalCatcher", true);
                if (useSignalCatcher) {
                    try {
                        this.signalCatcher = SignalCatcher.instance();
                        this.signalCatcher.register(this);
                        this.signalCatcher.catchSignals();
                    } catch (Throwable e) {
                        log.warn("Can't register signal catcher: "
                                + e.toString());
                    }
                }
                // Add us logonservice an I_ServiceManagerExceptionHandler ...
                if (ServiceManagerException.getExceptionHandler() == null)
                    ServiceManagerException.setExceptionHandler(this); // see public
                // void
                // newException(ServiceManagerException
                // e);
            }
            if (to == RunlevelManager.RUNLEVEL_STANDBY) {
            }
            if (to == RunlevelManager.RUNLEVEL_STANDBY_POST) {
                ;
            }
            if (to == RunlevelManager.RUNLEVEL_CLEANUP) {
            }
            if (to == RunlevelManager.RUNLEVEL_RUNNING) {
            }
            if (to == RunlevelManager.RUNLEVEL_RUNNING_POST) {
                log.info(RunTimeSingleton.getMemoryStatistic());
                String duration = TimeStamp.millisToNice(System
                        .currentTimeMillis() - this.startupTime);
                // TEST
                // new ServiceManagerException(this.glob,
                // SysErrorCode.RESOURCE_DB_UNAVAILABLE, ME + ".getXBStore", "",
                // null);

                log.info("ehrserver is ready for requests " + duration);
            }
        }
        if (to <= from) { // shutdown
            if (to == RunlevelManager.RUNLEVEL_RUNNING_PRE) {

                log.debug("Shutting down ehrserver to runlevel "
                        + RunlevelManager.toRunlevelStr(to) + " ...");
            }
            if (to == RunlevelManager.RUNLEVEL_HALTED_PRE) {
                synchronized (this) {
                    if (this.global != null) {
                        this.global.shutdown();
                    }
                }
                log.info("EhrServer halted.");
            }

            if (to == RunlevelManager.RUNLEVEL_HALTED) {
                synchronized (this) {
                    if (this.signalCatcher != null) {
                        this.signalCatcher.removeSignalCatcher();
                        this.signalCatcher = null;
                    }
                }
            }
        }
    }

    public void newException(ServiceManagerException e) {
        boolean serverScope = (e.getRunTimeSingleton() != null && e
                .getRunTimeSingleton().getObjectEntry(
                        "org.ehrserver.engine.RunTimeSingleton") != null);
        if (!e.isServerSide() && !serverScope) // isServerSide checks if we are
            // RunTimeSingleton
            // implementation, serverScope
            // checks if we are a
            // common.RunTimeSingleton in the
            // context of a server
            return;
        // Typically if the DB is lost: SysErrorCode.RESOURCE_DB_UNKNOWN
        if (this.panicErrorCodes.indexOf(e.getErrorCodeStr()) != -1) {
            log.error("PANIC: Doing immediate shutdown caused by exception: "
                    + e.getMessage());
            e.printStackTrace();
            log.error(RunTimeSingleton.getStackTraceAsString(e));
            log.error("Complete stack trace (all threads at the time of shutdown: "
                    + ThreadLister.getAllStackTraces());
            SignalCatcher sc = this.signalCatcher;
            if (sc != null) {
                sc.removeSignalCatcher();
            }
            System.exit(1);
        }
    }

    /**
     * You will be notified when the runtime exits.
     *
     * @see I_SignalListener#shutdownHook()
     */
    public void shutdownHook() {
        destroy();
    }

    /**
     * GET request from the browser, usually to do a query. TODO: add session
     * info in the dispatch query
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        processRequest(MethodName.GET, req, res, null);

        if (res.containsHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)) { //ugly patch...
            res.setHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        } else //more ugly patch...
            res.addHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");

    }

    /**
     * POST request from the browser, usually to do a disconnect, submit data
     * (change, extend)
     * <p>
     * We should expect here a body. The real issue is that we cannot really set
     * security checks on the content. TODO: add session info in the dispatch
     * query
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        I_SessionClientProperties bodyprops = requestBodyAsProps(req);
        processRequest(MethodName.POST, req, res, bodyprops);

        if (res.containsHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)) { //ugly patch...
            res.setHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        } else //more ugly patch...
            res.addHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    }

    /**
     * read the request body logonservice sent from the client
     *
     * @param req
     * @return
     * @throws java.io.IOException
     */
    private String getRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader reader = null;
        // String content = "";

        try {
            InputStream inputStream = req.getInputStream();
            if (inputStream != null) {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = reader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        return stringBuilder.toString();
    }

    /**
     * set up request content properties:
     * <p>
     * <ul>
     * <li>x-request-content the actual content
     * <li>x-request-content-type the content MIME type
     * <li>x-request-content-length content length
     * </ul>
     *
     * @param req
     * @return SessionClientProperties or null if no content
     * @throws java.io.IOException
     */
    private I_SessionClientProperties requestBodyAsProps(HttpServletRequest req)
            throws IOException {
        SessionClientProperties bodyprops = null;
        int body_length = req.getContentLength();
        if (body_length > 0) {
            String content = getRequestBody(req);
            if (content != null && content.length() > 0) {
                bodyprops = new SessionClientProperties(global);
                bodyprops.addClientProperty(Constants.REQUEST_CONTENT, content);
                bodyprops.addClientProperty(Constants.REQUEST_CONTENT_TYPE, req.getContentType());
                bodyprops.addClientProperty(Constants.REQUEST_CONTENT_LENGTH, req.getContentLength());
            }
        }
        return bodyprops;
    }

    /**
     * DELETE request from the browser, usually to do a delete (surprise!) TODO:
     * add session info in the dispatch query
     */
    public void doDelete(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        processRequest(MethodName.DELETE, req, res, null);
        if (res.containsHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)) { //ugly patch...
            res.setHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        } else //more ugly patch...
            res.addHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    }

    /**
     * PUT request from the browser, usually to put a file on the server
     * <p>
     * At this stage, the put request should allow passing file inputstreams to
     * the method... TODO: add session info in the dispatch query
     */
    public void doPut(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        I_SessionClientProperties bodyprops = requestBodyAsProps(req);
        processRequest(MethodName.PUT, req, res, bodyprops);
        if (res.containsHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)) { //ugly patch...
            res.setHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
        } else //more ugly patch...
            res.addHeader(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
    }

    /**
     * Process request from the browser, usually to do a query. TODO: add
     * session info in the dispatch query
     */
    public void processRequest(MethodName action, HttpServletRequest servletRequest,
                               HttpServletResponse servletResponse, I_SessionClientProperties sessionClientProperties)
            throws ServletException, IOException {

        try {
            uriParser.parse(servletRequest);
        } catch (ServiceManagerException e1) {
            throw new ServletException("badly formed path:" + servletRequest.getRequestURI() + ", error:" + e1.getMessage());
        }

        // get the parameters
//		HttpParameters httpParameters = HttpParameters.getInstance(global, servletRequest.getParameterMap());
//		I_SessionClientProperties props = httpParameters.getProperties();

        I_SessionClientProperties props = uriParser.identifyParametersAsProperties();

        props.addClientProperty(I_SessionManager.CLIENT_IP, servletRequest.getRemoteAddr());

        // add the additional properties (f.ex. request body) if any
        if (sessionClientProperties != null)
            props.getClientProperties().putAll(sessionClientProperties.getClientProperties());

        // sets the meta-data from header
        I_SessionClientProperties hdrprops = HttpParameters.getInstanceFromHeader(global, servletRequest);
        MethodName method;
        String path = uriParser.identifyPath();

        try {
            method = MethodName.toMethodName(uriParser.identifyMethod());
        } catch (IllegalArgumentException e) {
            throw new ServletException("No service for path/method call:"
                    + servletRequest.getPathInfo() + ", error:" + e.getMessage());
        } catch (ServiceManagerException e2) {
            if (e2.getErrorCode() == SysErrorCode.INTERNAL_ILLEGALARGUMENT)
                throw new ServletException("No service for path/method call:"
                        + servletRequest.getPathInfo() + ", error:" + e2.getMessage());
            else // process the error and return it to the sender
            {
                log.info("Error trapped:" + e2.getErrorCodeStr());
                errorOutput(servletResponse, e2);
                return;
            }
        }

        if (controller.isMappedMethodAsync(action, path, method) || isAsynchQuery) {
            asyncExecute(action, hdrprops, path, method, props, servletRequest, servletResponse);
        } else {
            syncExecute(action, hdrprops, path, method, props, servletResponse);
        }
    }

    /**
     * execute the query in synchronous (blocking) mode
     * <p>
     * This is generally the case with most queries...
     *
     * @param action
     * @param header
     * @param path
     * @param method
     * @param parameters
     * @param res
     * @throws ServletException
     * @throws java.io.IOException
     */
    public void syncExecute(MethodName action, I_SessionClientProperties header, String path, MethodName method, I_SessionClientProperties parameters, HttpServletResponse res) throws ServletException, IOException {
        Object output;

        try {
            output = controller.queryHandler(action, header, path, method, parameters);
        } catch (ServiceManagerException e2) {
            if (e2.getErrorCode() == SysErrorCode.INTERNAL_ILLEGALARGUMENT)
                throw new ServletException("Could not complete query :" + path + ", error:" + e2);
            else // process the error and return it to the sender
            {
                log.info("Error trapped:" + e2.getRawMessage());
                errorOutput(res, e2);
                return;
            }
        }

        handleOutput(controller.getMappedMethodReturnType(action, path, method), output, res, path);
    }

    /**
     * performs an asynchronous method service (e.g. non blocking)
     *
     * @param action
     * @param header
     * @param path
     * @param method
     * @param parameters
     * @param req
     * @param res
     * @throws ServletException
     */
    public void asyncExecute(MethodName action, I_SessionClientProperties header,
                             String path, MethodName method, I_SessionClientProperties parameters,
                             HttpServletRequest req, HttpServletResponse res)
            throws ServletException {

        try {
            if (!req.isAsyncSupported()) {
                // set explicitly the async nature
                req.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
            }
        } catch (Exception e) {
            throw new ServletException("Could not set the servlet in async mode:" + e);
        }

        final AsyncContext context = req.startAsync();
        // set the timeout
        context.setTimeout(callback_timeout);

        // attach listener to respond to lifecycle events of this AsyncContext
        context.addListener(new AsyncListener() {
            /**
             * complete() has already been called on the async context, nothing
             * to do
             */
            public void onComplete(AsyncEvent event) throws IOException {
            }

            /** timeout has occurred in async task... handle it */
            public void onTimeout(AsyncEvent event) throws IOException {
                log.warn("onTimeout called:" + event.toString());
                context.complete();
                new TimeoutEventResponse(event).write();

            }

            /**
             * THIS NEVER GETS CALLED - error has occured in async task...
             * handle it
             */
            public void onError(AsyncEvent event) throws IOException {
                log.info("onError called:" + event.toString());
                context.complete();
                new RequestErrorEventResponse(event).write();
            }

            /** async context has started, nothing to do */
            public void onStartAsync(AsyncEvent event) throws IOException {
            }
        });

        // spawn some task to be run in executor
        enqueueTask(context, action, header, path, method, parameters, res);

//		context.complete();
    }

    /**
     * enqueue a request for asynchronous service
     *
     * @param ctx
     * @param action
     * @param header
     * @param path
     * @param method
     * @param parameters
     */
    private void enqueueTask(final AsyncContext ctx, final MethodName action,
                             final I_SessionClientProperties header, final String path,
                             final MethodName method, final I_SessionClientProperties parameters, HttpServletResponse response) {

        executor.execute(new Runnable() {
            Object output;

            public void run() {

                try {
                    output = controller.queryHandler(action, header, path, method, parameters);
                } catch (ServiceManagerException e2) {
                    if (e2.getErrorCode() == SysErrorCode.INTERNAL_ILLEGALARGUMENT)
                        log.warn("No service for path/method call:" + path
                                + ", error:" + e2.getMessage());
                    else // process the error and return it to the sender
                    {
                        log.info("Error trapped:" + e2.getMessage());
                        try {
                            errorOutput(response, e2);
                        } catch (ServletException e) {
                            e.printStackTrace();
                        }
                        if (ctx != null)
                            ctx.complete();
                        return;
                    }
                }

                try {
                    // response is null if the context has already timed out
                    // (at this point the app server has called the listener
                    // already)
                    ServletResponse response = ctx.getResponse();

                    if (response != null) {
                        handleOutput(controller.getMappedMethodReturnType(action, path, method), output, response, path);
                        if (ctx != null) //if AsyncContext is supported, otherwise ignore...
                            ctx.complete();
                    } else {
                        throw new IllegalStateException(
                                "Response object from context is null!");
                    }
                } catch (Exception e) {
                    log("Problem processing task", e);
//                    try {
//                        errorOutput(ctx.getResponse(), new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "request timeout"));
//                    } catch (ServletException e1) {
//                        ;
//                    }
//                    e.printStackTrace();
                }

            }
        });
    }


    /**
     * encode the response according to the method return type.
     *
     * @param returnType
     * @param output
     * @param res
     * @throws ServletException
     * @throws java.io.IOException
     */
    private void handleOutput(int returnType, Object output, Object res, String path) throws ServletException, IOException, IllegalArgumentException {

        if (getGlobal().getProperty().propertyExists(MethodName.RETURN_TYPE_PROPERTY)) {
            int changedType = getGlobal().getProperty().get(MethodName.RETURN_TYPE_PROPERTY, MethodName.RETURN_UNDEFINED);
            if (changedType != MethodName.RETURN_UNDEFINED)
                returnType = changedType;
        }

        switch (returnType) {
            case MethodName.RETURN_HTML:
                new HtmlHttpResponse(res).respond((String) output);
                break;
            case MethodName.RETURN_JSON:
                new JsonHttpResponse(res).respond(output, path);
                break;
            case MethodName.RETURN_XML:
                new XmlHttpResponse(res).respond(output, path);
                break;

            case MethodName.RETURN_XML_ARRAY:
                new XmlArrayHttpResponse(res).respond((XmlObject[]) output);
                break;

            case MethodName.RETURN_STRING:
                if (output instanceof String)
                    new TextHttpResponse(res).respond((String) output);
                else if (output instanceof ResponseHolder)
                    new TextHttpResponse(res).respond((String) ((ResponseHolder) output).getData());
                else
                    throw new IllegalArgumentException("Unhandled data type in response return");
                break;

            case MethodName.RETURN_STRINGARR:
                new TextArrayHttpResponse(res).respond((String[]) output);
                break;

            case MethodName.RETURN_PROPERTY:
                new PropertiesHttpResponse(res).respond((I_SessionClientProperties) output);
                break;

            case MethodName.RETURN_VOID:
                new VoidHttpResponse(res).respond((ResponseHolder) output);
                break;

            case MethodName.RETURN_DYNA:
                new DynamicHttpResponse(((ResponseHolder) output).getContentType(), res).respond((ResponseHolder) output);
                break;

            case MethodName.RETURN_NO_CONTENT:
                new NoContentHttpResponse(res).respond(output, path);
                break;

            default:
                new TextHttpResponse(res).respond("undefined or unhandled return type for method");
                break;
        }
        try {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_UNDEFINED);
        } catch (ServiceManagerException e) {
            throw new IllegalArgumentException("Could not reset global return type property");
        }

    }

    /**
     * encode and send nicely an error thrown in the backend
     *
     * @param response
     * @throws ServletException
     */
    public void errorOutput(HttpServletResponse response, ServiceManagerException exception) throws ServletException {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        int code = exception.getErrorCode().getHttpCode();

        // stuff in more details in the header...
        response.setHeader(I_SessionManager.ERROR_CODE, exception.getErrorCodeStr());
        response.setHeader(I_SessionManager.ERROR_MESSAGE, exception.getRawMessage());

        if (code == HttpServletResponse.SC_UNAUTHORIZED)
            response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Basic realm=\"Shiro-Authenticate\"");


        try {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_UNDEFINED);
        } catch (ServiceManagerException e) {
//			throw new IllegalArgumentException("Could not reset global return type property");
            //do nothing
        }

        try {
            response.sendError(code, exception.getRawMessage());
        } catch (IOException e) {
            log.warn("Could not deliver HTML page to browser:" + e.toString());
            throw new ServletException(e.toString());
        }

    }

    public void errorOutput(ServletResponse servletResponse, ServiceManagerException exception) throws ServletException {
        ServletResponseHttpWrapper response = new ServletResponseHttpWrapper(servletResponse);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        int code = exception.getErrorCode().getHttpCode();

        // stuff in more details in the header...
        response.setHeader(I_SessionManager.ERROR_CODE, exception.getErrorCodeStr());
        response.setHeader(I_SessionManager.ERROR_MESSAGE, exception.getRawMessage());

        if (code == HttpServletResponse.SC_UNAUTHORIZED)
            response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Basic realm=\"Shiro-Authenticate\"");


        try {
            global.getProperty().set(MethodName.RETURN_TYPE_PROPERTY, "" + MethodName.RETURN_UNDEFINED);
        } catch (ServiceManagerException e) {
//			throw new IllegalArgumentException("Could not reset global return type property");
            //do nothing
        }

        try {
            response.sendError(code, exception.getRawMessage());
        } catch (IOException e) {
            log.warn("Could not deliver HTML page to browser:" + e.toString());
            throw new ServletException(e.toString());
        }

    }

    /**
     * A human readable name of the listener for logging.
     * <p>
     * Enforced by I_RunlevelListener
     */
    public String getName() {
        return ME;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Context is about to be destroyed...");

    }

    public RunTimeSingleton getGlobal() {
        return global;
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

    }
}

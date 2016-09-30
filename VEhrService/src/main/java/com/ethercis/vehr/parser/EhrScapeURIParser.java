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
package com.ethercis.vehr.parser;

import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.HttpParameters;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/7/2015.
 */
@Service(id ="URIParser", version="1.0", system=true)

@RunLevelActions(value = {
        @RunLevelAction(onStartupRunlevel = 7, sequence = 1, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 7, sequence = 1, action = "STOP") })

public class EhrScapeURIParser extends URIParser {

    public static final String ME = "URIParser";
    final private String Version = "1.0";
    private Logger log = LogManager.getLogger(EhrScapeURIParser.class);

//    private Map<String, String> parameters;

    /**
     * create a new parser for path, check format
     *
     * @param global
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    public EhrScapeURIParser(RunTimeSingleton global)  {
        super(global);
     }

    public EhrScapeURIParser(){}


    private String[] subarray(String[] array, int from, int to){
        if (from > array.length)
            return null;
        if (to > array.length)
            to = array.length;
        if (from == array.length)
            return array;

        return Arrays.copyOfRange(array, from, to);
    }

    private String[] subarray(String[] array, int from){
        int to = array.length;
        if (from > array.length - 1)
            return null;

        return Arrays.copyOfRange(array, from, to);
    }

    @Override
    public void parse(HttpServletRequest servletRequest) throws ServiceManagerException {
        String requestURI;
        try {
            requestURI = URLDecoder.decode(servletRequest.getRequestURI(), "UTF-8");
        }catch (UnsupportedEncodingException e){
            throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, "URI could not be parsed:"+servletRequest.getRequestURI());
        }

        //tokenize the URI
        String[] tokens = requestURI.split(queryRoot);
        if (tokens.length < 2)
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, ME, "Badly formed query, could not identify resources:"+requestURI);

        if (requestURI.charAt(0) != delimiter.toCharArray()[0])
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, ME, "Badly formed query, invalid format, first char in path should be '/' in path:["+path+"]");

        queryMethod = MethodName.toMethodName(servletRequest.getMethod());

        HttpParameters httpParameters = null;
        try {
            httpParameters = HttpParameters.getInstance(global, servletRequest.getParameterMap());
        } catch (IOException e) {
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, ME, "Could not retrieve parameters in query:"+e);

        }
        parameters = httpParameters.getProperties();

        headers = HttpParameters.getInstanceFromHeader(global, servletRequest);

        //strip parameters
        if (tokens[1].contains("?"))
            tokens[1] = tokens[1].substring(0, tokens[1].indexOf("?"));

        tokens = tokens[1].split("/");

        resourceToken = tokens[1];

        tokens = subarray(tokens, 2);

        switch (resourceToken.toUpperCase()){
            case "SESSION":
                break;
            case "EHR":
                EhrQueryParser ehrQueryParser = new EhrQueryParser(queryMethod, resourceToken, tokens, parameters, headers);
                resourceToken = ehrQueryParser.getResource();
                parameters = ehrQueryParser.getParameters();
                break;
            case "COMPOSITION":
                CompositionQueryParser compositionQueryParser = new CompositionQueryParser(queryMethod, resourceToken, tokens, parameters, headers);
                resourceToken = compositionQueryParser.getResource();
                parameters = compositionQueryParser.getParameters();
                break;
            case "TEMPLATE":
                TemplateQueryParser templateQueryParser = new TemplateQueryParser(queryMethod, resourceToken, tokens, parameters, headers);
                resourceToken = templateQueryParser.getResource();
                parameters = templateQueryParser.getParameters();
                break;
            case "QUERY":
                break;
            case "DEMOGRAPHICS":
                break;
            case "GUIDE":
                break;
            case "SMART":
                break;
            case "IMPORT":
                break;
            default:
                //collate tokens
                if (tokens != null) {
                    List<String> remainder = Arrays.asList(tokens);
                    if (!remainder.isEmpty())
                        resourceToken = resourceToken + "/" + String.join("/", remainder);
                }
                break;


        }

        requestURI = requestURI.substring(1);

        this.pathitems = requestURI.split(delimiter);
        this.path = requestURI;

        if (pathitems.length < 2)
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, ME, "Badly formed query, invalid format, this should be at least /service/method in path:["+path+"]");
    }

    @Override
    public String identifyPath(){
//        if (pathitems.length > 1){
//            StringBuffer res = new StringBuffer();
//            for (int i = 0; i < pathitems.length; i++) {
//                res.append(pathitems[i]+"/");
//            }
//            //remove trailing '/'
//            String ret = res.substring(0, res.length() - 1);
//            return ret;
//        }
//        else if (pathitems.length == 1)
//            return pathitems[0];
//
//        return null; //no resource defined

        return queryRoot.substring(1)+"/"+ resourceToken;
    }

    @Override
    public String identifyMethod() throws ServiceManagerException {
        return queryMethod.getMethodName();
    }

    @Override
    public I_SessionClientProperties identifyParametersAsProperties() {
        return parameters;
    }

    @Override
    protected void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
        this.global = global;
        String compatibilityValue = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_PARAMETER, I_ServiceRunMode.DialectSpace.STANDARD.toString());
        dialectSpace =  I_ServiceRunMode.DialectSpace.valueOf(compatibilityValue);
        queryRoot = global.getProperty().get(I_ServiceRunMode.SERVER_DIALECT_ROOT, "/");
        log.info("EhrScape URI parser service started...");
    }
}

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

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import javax.servlet.http.HttpServletRequest;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/7/2015.
 */
public class EhrScapeURIParser extends URIParser {

    private MethodName queryMethod;

    /**
     * create a new parser for path, check format
     *
     * @param global
     * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     */
    public EhrScapeURIParser(RunTimeSingleton global)  {
        super(global);
    }

    @Override
    public void parse(HttpServletRequest servletRequest) throws ServiceManagerException {
        String requestURI = servletRequest.getRequestURI();
        if (requestURI.charAt(0) != delimiter.toCharArray()[0])
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, "Badly formed query, invalid format, first char in path should be '/' in path:["+path+"]");

        requestURI = requestURI.substring(1);

        this.pathitems = requestURI.split(delimiter);
        this.path = requestURI;

        if (pathitems.length < 2)
            throw new ServiceManagerException(global, SysErrorCode.USER_QUERY_INVALID, "Badly formed query, invalid format, this should be at least /service/method in path:["+path+"]");

        queryMethod = MethodName.toMethodName(servletRequest.getMethod());

    }

    @Override
    public String identifyPath(){
        if (pathitems.length > 1){
            StringBuffer res = new StringBuffer();
            for (int i = 0; i < pathitems.length; i++) {
                res.append(pathitems[i]+"/");
            }
            //remove trailing '/'
            String ret = res.substring(0, res.length() - 1);
            return ret;
        }
        else if (pathitems.length == 1)
            return pathitems[0];

        return null; //no resource defined
    }

    @Override
    public String identifyMethod() throws ServiceManagerException {
        return queryMethod.getMethodName();
    }
}

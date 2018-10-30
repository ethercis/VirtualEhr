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

package com.ethercis.vehr;

import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.servicemanager.common.MetaBuilder;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.server.Request;

import javax.servlet.AsyncEvent;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 10/29/2018.
 */
public abstract class FailedQueryEventResponse {

    AsyncEvent event;
    Integer code;
    String message;

    protected FailedQueryEventResponse(AsyncEvent event, Integer code, String message){
        this.event  = event;
        this.code = code;
        this.message = message;
    }

    public void write(){
        HttpServletResponse servletResponse = (HttpServletResponse) event.getAsyncContext().getResponse();
        servletResponse.setStatus(code);
        servletResponse.setHeader(I_SessionManager.ERROR_MESSAGE, message);
        servletResponse.setContentType("application/json;charset=UTF-8");
        Map<String, Object> retmap = new HashMap<>();
        retmap.put("action", "FAILED");

        String uri = ((Request) event.getSuppliedRequest()).getHttpURI().getDecodedPath();
        uri = uri.substring(StringUtils.ordinalIndexOf(uri, "/", 2)+1);
        String parms = ((Request) event.getSuppliedRequest()).getHttpURI().getQuery();

        Map<String, Map<String, String>> metaref = MetaBuilder.add2MetaMap(null, "href", uri + "?" + parms);
        retmap.putAll(metaref);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new ISO8601DateFormat());

        try {
            String bodyContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(retmap);
            servletResponse.setContentLength(bodyContent.getBytes().length);
            servletResponse.getWriter().write(bodyContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

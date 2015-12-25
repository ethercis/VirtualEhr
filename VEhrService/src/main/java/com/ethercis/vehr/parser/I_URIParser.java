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

import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/7/2015.
 */
public interface I_URIParser {
    void parse(HttpServletRequest servletRequest) throws ServiceManagerException;

    String identifyService() throws ServiceManagerException;

    String identifyResource();

    String identifyPath();

    String identifyMethod() throws ServiceManagerException;

//    Map<String, String> indentifyParameters();

    I_SessionClientProperties identifyParametersAsProperties();

}

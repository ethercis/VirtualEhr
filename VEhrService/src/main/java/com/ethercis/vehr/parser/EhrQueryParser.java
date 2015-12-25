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
import com.ethercis.servicemanager.common.def.MethodName;

/**
 * Created by christian on 12/9/2015.
 */
public class EhrQueryParser implements I_QueryParser {

    private I_SessionClientProperties parameters;
    private String resourceToken = null;

    public EhrQueryParser(MethodName methodName, String resourceToken, String[] tokens, I_SessionClientProperties parameters, I_SessionClientProperties headers){
        this.parameters = parameters;
        this.resourceToken = resourceToken;
        switch (methodName.getMethodName().toUpperCase()){
            case "GET":
                //add the ehrId in parameters
                if (tokens != null && tokens.length == 1) {
                    parameters.addClientProperty("ehrId", tokens[0]);
                    this.resourceToken = this.resourceToken + "/status";
                }
                else { //get status for subject
//                    this.resourceToken = this.resourceToken+"/status";
                }
                break;
            case "POST":
                break;
            case "DELETE":
                break;
            case "PUT": //update status
                this.resourceToken = this.resourceToken+"/"+tokens[0];
                parameters.addClientProperty("ehrId", tokens[1]);
                break;
        }
    }

    public I_SessionClientProperties getParameters(){
        return parameters;
    }

    public String getResource(){
        return resourceToken;
    }

}

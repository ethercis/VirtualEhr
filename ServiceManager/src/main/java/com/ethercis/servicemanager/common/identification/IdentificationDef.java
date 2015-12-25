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

package com.ethercis.servicemanager.common.identification;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;

/**
 * Created by christian on 12/18/2015.
 */
public class IdentificationDef {

    private String idendificationNamespace = null;
    private String idenficationScheme = null;
    private String idenficationType = "ANY";

    public IdentificationDef(RunTimeSingleton global){
        idendificationNamespace = global.getProperty().get("identification.default.namespace", global.getProperty().get("server.security.policy.namespace", "DEFAULT"));
        idenficationScheme = global.getProperty().get("identification.default.scheme", global.getProperty().get("server.security.policy.type", "DEFAULT"));
        idenficationType = global.getProperty().get("identification.default.type", "ANY");
    }

    public String getIdendificationNamespace() {
        return idendificationNamespace;
    }

    public String getIdenficationScheme() {
        return idenficationScheme;
    }

    public String getIdenficationType() {
        return idenficationType;
    }
}

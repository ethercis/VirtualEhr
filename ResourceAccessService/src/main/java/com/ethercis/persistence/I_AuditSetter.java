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

package com.ethercis.persistence;

import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;

import java.util.UUID;

/**
 * Created by christian on 6/6/2018.
 */
public interface I_AuditSetter {
    I_AuditSetter handleProperties(I_DomainAccess domainAccess, I_SessionClientProperties props) throws ServiceManagerException;

    //TODO: refactor into IdentificationService
    UUID getCommitter(I_DomainAccess domainAccess, String name, String id);

    String getSessionSubjectName(String sessionId) throws ServiceManagerException;

    String getSessionSubjectId(String sessionId) throws ServiceManagerException;

    //this is to avoid circular references
    UUID getOrCreateSystemId(I_DomainAccess domainAccess, String description, String systemAddress);

    String getSessionId();

    UUID getCommitterUuid();

    UUID getSystemUuid();

    String getDescription();
}

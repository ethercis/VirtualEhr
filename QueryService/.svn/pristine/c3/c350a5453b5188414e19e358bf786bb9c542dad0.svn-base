package com.ethercis.query;/*
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



import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.ehr.knowledge.I_KnowledgeCache;
import com.ethercis.query.I_QueryBodyHandler;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 9/18/2015.
 */
public abstract class QueryBodyHandler implements I_QueryBodyHandler {

    protected final I_KnowledgeCache knowledgeCache;
    protected String templateId;

    protected QueryBodyHandler(I_KnowledgeCache cache, String templateId){
        this.knowledgeCache = cache;
        this.templateId = templateId;
    }

    String defaultedTemplateId(String templateId){
        //check for extension
        return templateId.toLowerCase().endsWith("opt")?
                templateId:
                templateId.toLowerCase().endsWith("oet")?
                        templateId:
                        templateId+".opt"; //defaulted to opt
    }

    abstract Boolean update(RunTimeSingleton global, I_DomainAccess access, UUID compositionId, String content, UUID committerId, UUID systemId, String description) throws Exception;
}

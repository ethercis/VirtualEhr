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
//Copyright
package com.ethercis.compositionservice.handler;

import com.ethercis.dao.access.interfaces.I_CompositionAccess;
import com.ethercis.dao.access.interfaces.I_ConceptAccess;
import com.ethercis.dao.access.interfaces.I_DomainAccess;
import com.ethercis.dao.access.interfaces.I_EntryAccess;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.knowledge.I_KnowledgeCache;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.openehr.rm.composition.Composition;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 9/18/2015.
 */
public class CanonicalHandler extends QueryBodyHandler {

    public static final String ME = "CanonicalHandler";

    public CanonicalHandler(I_KnowledgeCache cache, String templateId, Properties properties){
        super(cache, templateId);
    }

    @Override
    public Composition build(RunTimeSingleton global, String content) throws Exception {
//        String template = defaultedTemplateId(templateId);
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        I_ContentBuilder contentBuilder = I_ContentBuilder.getInstance(null, I_ContentBuilder.OPT, this.knowledgeCache, "IMPLICIT");
        try {
            return contentBuilder.importCanonicalXML(inputStream);
        } catch (Exception e){
            throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, ME, "Could not parse supplied content:"+e);
        }
    }

    @Override
    public Boolean update(RunTimeSingleton global, I_DomainAccess access, UUID compositionId, String content) throws Exception {
        return update(global, access, compositionId, content, null, null, null);
    }

    @Override
    public Boolean update(RunTimeSingleton global, I_DomainAccess access, UUID compositionId, String content, UUID committerId, UUID systemId, String description) throws Exception {
        if (templateId == null)
            throw new ServiceManagerException(global, SysErrorCode.USER_ILLEGALARGUMENT, ME, "Template Id is required");
        Composition composition = build(global, content);
        I_CompositionAccess compositionAccess = I_CompositionAccess.retrieveInstance(access, compositionId);
        List<I_EntryAccess> contentList = new ArrayList<>();
        contentList.add(I_EntryAccess.getNewInstance(access, templateId, 0, compositionAccess.getId(), composition));
        compositionAccess.setContent(contentList);
        return compositionAccess.update(committerId, systemId, null, I_ConceptAccess.ContributionChangeType.modification, description);
    }
}

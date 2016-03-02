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


package com.ethercis.compositionservice.handler;

import com.ethercis.dao.access.interfaces.*;
import com.ethercis.ehr.building.I_ContentBuilder;
import com.ethercis.ehr.building.util.ContextHelper;
import com.ethercis.ehr.json.FlatJsonUtil;
import com.ethercis.ehr.knowledge.I_KnowledgeCache;
import com.ethercis.ehr.util.FlatJsonCompositionConverter;
import com.ethercis.ehr.util.I_FlatJsonCompositionConverter;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import org.joda.time.DateTime;
import org.openehr.rm.composition.Composition;
import org.openehr.rm.composition.EventContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.*;

/**
 * Utility class to factorize the implementation of composition update in particular
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 9/18/2015.
 */
public class FlatJsonHandler extends QueryBodyHandler {

    public static final String ME = "FlatJsonHandler";
    private final I_CompositionAccess compositionAccess;
    private final I_DomainAccess domainAccess;

    public FlatJsonHandler(I_DomainAccess domainAccess,  I_CompositionAccess compositionAccess, String templateId, Properties properties){
        super(domainAccess.getKnowledgeManager(), templateId);
        this.domainAccess = domainAccess;
        this.compositionAccess = compositionAccess;
    }

    @Override
    public Composition build(RunTimeSingleton global, String content) throws Exception {
        I_FlatJsonCompositionConverter flatJsonCompositionConverter = FlatJsonCompositionConverter.getInstance(knowledgeCache);
        Map flatMap = FlatJsonUtil.inputStream2Map(new StringReader(new String(content.getBytes())));
        Composition newComposition = flatJsonCompositionConverter.toComposition(templateId, flatMap);
        return newComposition;
    }

    @Override
    public Boolean update(RunTimeSingleton global, I_DomainAccess access, UUID compositionId, String content) throws Exception {
        return update(global, access, compositionId, content, null, null, null);
    }

    @Override
    public Boolean update(RunTimeSingleton global, I_DomainAccess access, UUID compositionId, String content, UUID committerId, UUID systemId, String description) throws Exception {
        boolean changed = false;
        boolean changedContext = false;

        UUID contextId = compositionAccess.getContextId();
        Timestamp updateTransactionTime = new Timestamp(DateTime.now().getMillis());

        I_ContextAccess contextAccess;

        if (compositionAccess.getContextId() == null){
            EventContext context = ContextHelper.createNullContext();
            contextAccess = I_ContextAccess.getInstance(domainAccess, context);
            contextAccess.commit(updateTransactionTime);
        }
        else
            contextAccess = I_ContextAccess.retrieveInstance(domainAccess, contextId);

        for (I_EntryAccess entryAccess: compositionAccess.getContent()) {
            //set the template Id
            templateId = entryAccess.getTemplateId();
            Composition newComposition = build(global, content);
            entryAccess.setCompositionData(newComposition.getArchetypeDetails().getTemplateId().getValue(), newComposition);
            changed = true;
            EventContext eventContext = newComposition.getContext();
            contextAccess.setRecordFields(contextId, eventContext);
            changedContext = true;
        }

        if (changedContext)
            contextAccess.update(updateTransactionTime);

        if (changed) {
            return compositionAccess.update(updateTransactionTime, committerId, systemId, null, I_ConceptAccess.ContributionChangeType.modification, description, true);
        }

        return true; //nothing to do...
    }
}

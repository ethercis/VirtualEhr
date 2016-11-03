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
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import org.openehr.rm.composition.Composition;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 10/5/2015.
 */
public interface I_QueryBodyHandler {
    Composition build(RunTimeSingleton global, String content) throws Exception;
    Boolean update(RunTimeSingleton global, I_DomainAccess access, UUID compositionId, String content) throws Exception;
}

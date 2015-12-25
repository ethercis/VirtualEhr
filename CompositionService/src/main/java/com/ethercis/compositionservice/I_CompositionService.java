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
package com.ethercis.compositionservice;

import java.util.UUID;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/3/2015.
 */
public interface I_CompositionService {
    String  TEMPLATE_ID = "templateId";
    String  EHR_ID = "ehrId";
    String  UID = "uid";
    String  FORMAT = "format";

    enum CompositionFormat {FLAT, STRUCTURED, RAW, XML, ECISFLAT };

    //returned attribute names
    String COMPOSITION_UID = "compositionUid";

    static UUID decodeUuid(String encodedUuid){
        return UUID.fromString(encodedUuid.substring(0, encodedUuid.indexOf("::")));
    }
}

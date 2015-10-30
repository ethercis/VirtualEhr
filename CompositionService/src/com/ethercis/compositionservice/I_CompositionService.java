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

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 7/3/2015.
 */
public interface I_CompositionService {
    public static final String COMMITTER_ID = "committerId" ;
    public static final String COMMITTER_NAME = "committerName";
    public static final String  TEMPLATE_ID = "templateId";
    public static final String  EHR_ID = "ehrId";
    public static final String  UID = "uid";
    public static final String  FORMAT = "format";

    public enum CompositionFormat { FLATJSON, STRUCTUREDJSON, RAWJSON, CANONICAL, ECISFLAT };

    //returned attribute names
    public static final String COMPOSITION_UID = "compositionUid";
}

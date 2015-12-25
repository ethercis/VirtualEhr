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

package com.ethercis.servicemanager.common;

import com.ethercis.servicemanager.common.def.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 11/13/2015.
 */
public class MetaBuilder {

    public static Map<String, Map<String, String>> add2MetaMap(Map<String, Map<String, String>> metaMap, String key, String value){

        Map<String, String> contentMap;

        if (metaMap == null) {
            metaMap = new HashMap<>();
            contentMap = new HashMap<>();
            metaMap.put("meta", contentMap);
        }
        else
            contentMap = metaMap.get("meta");

        contentMap.put(key, value);

        return metaMap;
     }

    public static Map<String, Map<String, String>> substituteVarMetaMap(Map<String, Map<String, String>> metaMap, String tag, String value){
        Map<String, String> contentMap = metaMap.get("meta");

        for (String key: contentMap.keySet()){

            String contentValue = contentMap.get(key);
            if (contentValue.contains(tag))
                contentMap.put(key, contentValue.replaceAll(tag, value));
        }

        return metaMap;

    }
}

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
package com.ethercis.servicemanager.annotation;

import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;

import java.lang.annotation.Annotation;

/**
 * ETHERCIS Project VirtualEhr
 * Created by Christian Chevalley on 6/25/2015.
 */
public class ParameterAnnotationHelper {

    public static String parameterName(I_ServiceRunMode.DialectSpace dialectSpace, String parameter, Annotation[] annotations){
        if (annotations == null)
            return null;

        ParameterSetting setting = null;

        for (Annotation annotation: annotations){
            if (annotation instanceof ParameterSetting){
                setting = (ParameterSetting)annotation;
                break;
            }
        }

        if (setting == null)
            return null;

        for (ParameterIdentification parameterIdentification: setting.identification()){
            if (parameterIdentification.id().equals(parameter)){
                for (ParameterDefinition parameterDefinition: parameterIdentification.definition()){
                    if (parameterDefinition.mode().compareTo(dialectSpace) == 0){
                        return parameterDefinition.name();
                    }
                }
            }
        }

        return null;
    }
}

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
package com.ethercis.servicemanager.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The generic class for marshall/unmarshall list of object 
 * @author Noppadol N
 * #
 * @param <T>
 */

@XmlRootElement(name="List")
public class JaxbList<T> {
	protected List<T> list;

    public JaxbList(){}

    public JaxbList(List<T> list){
        this.list=list;
    }

    @XmlElement(name="Item")
    public List<T> getList(){
        return list;
    }
}

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
package com.ethercis.authenticate;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.ethercis.servicemanager.common.security.I_Authenticate;


/**
 * The generic class for marshall/unmarshall list of object
 * 
 * @author Noppadol N #
 * @param <T>
 */

@XmlRootElement
@XmlType(name = "subjects", propOrder = { "subjects" })
public class SubjectList {
	protected List<I_Authenticate> subjects=new ArrayList<I_Authenticate>();

	public SubjectList() {
	}

	public SubjectList(List<I_Authenticate> subjects) {
		this.subjects = subjects;
	}

	@XmlElement(name = "subjects")
	public List<I_Authenticate> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<I_Authenticate> subjects) {
		this.subjects = subjects;
	}


	@XmlAttribute(name = "total")
	public int getTotal() {
		if (subjects == null) {
			return 0;
		} else {
			return subjects.size();
		}
	}

	@Override
	public String toString() {
		return "SubjectList [Subjects=" + subjects + "]";
	}

	
}

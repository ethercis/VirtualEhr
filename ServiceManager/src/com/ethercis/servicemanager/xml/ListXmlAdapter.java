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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ListXmlAdapter extends
		XmlAdapter<ListElements[], List<Object>> {

	@Override
	public List<Object> unmarshal(ListElements[] v) throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		for (ListElements e : v) {
			list.add(e.value);
		}
		return list;
	}

	@Override
	public ListElements[] marshal(List<Object> v) throws Exception {
		ListElements[] e = new ListElements[v.size()];
		
		for (int i = 0; i < v.size(); i++) {
			e[i]= new ListElements(v.get(i));
		}
		return e;
	}

}

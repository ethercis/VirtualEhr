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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter extends XmlAdapter<MapElements[], Map<String, Object>> {
	public MapElements[] marshal(Map<String, Object> arg0) throws Exception {
		MapElements[] mapElements = new MapElements[arg0.size()];
		int i = 0;
		for (Map.Entry<String, Object> entry : arg0.entrySet())
			mapElements[i++] = new MapElements(entry.getKey(), entry.getValue());

		return mapElements;
	}

	public Map<String, Object> unmarshal(MapElements[] arg0) throws Exception {
		Map<String, Object> r = new HashMap<String, Object>();
		for (MapElements mapelement : arg0)
			r.put(mapelement.key, mapelement.value);
		return r;
	}
}
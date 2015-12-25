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

import java.util.List;

public class DoW {
	private Boolean stat;
	private List<Period> periods;

	public DoW() {
		super();
	}

	public DoW(Boolean stat, List<Period> periods) {
		super();
		this.stat = stat;
		this.periods = periods;
	}

	public Boolean getStat() {
		return stat;
	}

	public Boolean isStat() {
		return stat;
	}

	public void setStat(Boolean stat) {
		this.stat = stat;
	}

	public List<Period> getPeriods() {
		return periods;
	}

	public void setPeriods(List<Period> periods) {
		this.periods = periods;
	}

}

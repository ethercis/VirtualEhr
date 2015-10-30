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
package com.ethercis.servicemanager.common.security;

import com.ethercis.servicemanager.common.DoW;

public interface I_WeekPlan {

	public abstract DoW getSunday();

	public abstract void setSunday(DoW sunday);

	public abstract DoW getMonday();

	public abstract void setMonday(DoW monday);

	public abstract DoW getTuesday();

	public abstract void setTuesday(DoW tuesday);

	public abstract DoW getWednesday();

	public abstract void setWednesday(DoW wednesday);

	public abstract DoW getThursday();

	public abstract void setThursday(DoW thursday);

	public abstract DoW getFriday();

	public abstract void setFriday(DoW friday);

	public abstract DoW getSaturday();

	public abstract void setSaturday(DoW saturday);

	public abstract String toJsonString();

}
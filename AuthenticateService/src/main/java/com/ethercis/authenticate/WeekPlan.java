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

import com.ethercis.servicemanager.common.DoW;
import com.ethercis.servicemanager.common.security.I_WeekPlan;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

@XmlRootElement
@XmlType(propOrder={"sunday","monday","tuesday","wednesday","thursday","friday","saturday"} )
public class WeekPlan implements Serializable, I_WeekPlan{

	private static final long serialVersionUID = -4841692082536322240L;
	private DoW sunday;
	private DoW monday;
	private DoW tuesday;
	private DoW wednesday;
	private DoW thursday;
	private DoW friday;
	private DoW saturday;
	
	public WeekPlan() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getSunday()
	 */
	@Override
	public DoW getSunday() {
		return sunday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setSunday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setSunday(DoW sunday) {
		this.sunday = sunday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getMonday()
	 */
	@Override
	public DoW getMonday() {
		return monday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setMonday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setMonday(DoW monday) {
		this.monday = monday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getTuesday()
	 */
	@Override
	public DoW getTuesday() {
		return tuesday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setTuesday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setTuesday(DoW tuesday) {
		this.tuesday = tuesday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getWednesday()
	 */
	@Override
	public DoW getWednesday() {
		return wednesday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setWednesday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setWednesday(DoW wednesday) {
		this.wednesday = wednesday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getThursday()
	 */
	@Override
	public DoW getThursday() {
		return thursday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setThursday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setThursday(DoW thursday) {
		this.thursday = thursday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getFriday()
	 */
	@Override
	public DoW getFriday() {
		return friday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setFriday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setFriday(DoW friday) {
		this.friday = friday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#getSaturday()
	 */
	@Override
	public DoW getSaturday() {
		return saturday;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#setSaturday(com.ethercis.common.common.DoW)
	 */
	@Override
	public void setSaturday(DoW saturday) {
		this.saturday = saturday;
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_WeekPlan#toJsonString()
	 */
	@Override
	public String toJsonString() {
//		try {
////			return JaxbUtils.marshalJson(this);
//            //TODO: use GSON
//            return null;
//		} catch (JAXBException e) {
//			e.printStackTrace();
//		}
		return "{}";
	}
	public static I_WeekPlan fromJsonString(String json) {
//		try {
            //TODO: use GSON
			return null;
//            return (I_WeekPlan) JaxbUtils.unmarshalJson(new ByteArrayInputStream(json.getBytes()),WeekPlan.class);
//		} catch (JAXBException e) {
//			e.printStackTrace();
//		}
//		return null;
	}
	
	public static void main(String[] args) {
		I_WeekPlan wp=new WeekPlan();
		wp.setSunday(new DoW(true,null));
		wp.setMonday(new DoW(true,null));
		wp.setTuesday(new DoW(true,null));
		wp.setWednesday(new DoW(true,null));
		wp.setThursday(new DoW(true,null));
		wp.setFriday(new DoW(true,null));
		wp.setSaturday(new DoW(true,null));
		System.out.println(wp.toJsonString());
		
	}
	
	
}

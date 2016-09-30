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

import com.ethercis.servicemanager.common.Parameter;
import com.ethercis.servicemanager.common.security.I_FilteredObject;
import com.ethercis.servicemanager.common.security.I_Parameter;
import com.ethercis.servicemanager.common.security.I_Target;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlType(propOrder={"action","target","parameters"} )
public class FilteredObject implements Serializable, I_FilteredObject {
	private static Logger log = LogManager.getLogger(FilteredObject.class);
	private static final long serialVersionUID = -1861462482031156758L;
	private String action;
	private I_Target target;
	private List<I_Parameter> parameters;
	
	
	public FilteredObject() {
		super();
	}
	public FilteredObject(String action, I_Target target,
			List<I_Parameter> parameters) {
		super();
		this.action = action;
		this.target = target;
		this.parameters = parameters;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#getAction()
	 */
	@Override
	public String getAction() {
		return action;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#setAction(java.lang.String)
	 */
	@Override
	public void setAction(String action) {
		this.action = action;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#getTarget()
	 */
	@Override
	public I_Target getTarget() {
		return target;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#setTarget(com.ethercis.common.common.Target)
	 */
	@Override
	public void setTarget(I_Target target) {
		this.target = target;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#getParameters()
	 */
	@Override
	public List<I_Parameter> getParameters() {
		return parameters;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#setParameters(java.common.List)
	 */
	@Override
	public void setParameters(List<I_Parameter> parameters) {
		this.parameters = parameters;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#toString()
	 */
	@Override
	public String toString() {
		return "FilteredObject [action=" + action + ", target=" + target
				+ ", parameters=" + parameters + "]";
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.logonservice.security.policy.model.I_FilteredObject#toJsonString()
	 */
	@Override
	public String toJsonString() {
//		try {
//			return JaxbUtils.marshalJson(this);
//		} catch (JAXBException e) {
//			e.printStackTrace();
//		}
		return "{}";
	}
	public static I_FilteredObject fromJsonString(String json) throws JAXBException {
		return null;
//        return (I_FilteredObject) JaxbUtils.unmarshalJson(new ByteArrayInputStream(json.getBytes()),FilteredObject.class);

	}	
	public static void main(String[] args) {
		List<I_Parameter> params=new ArrayList<I_Parameter>();
		params.add(new Parameter("SSS","XXXX"));
		params.add(new Parameter("DDD","YYYY"));
		//FilteredObject obj=new FilteredObject("connect",new Target("xxx","Open"),params);
		I_FilteredObject obj=new FilteredObject("connect",null,params);
		System.out.println(obj.toJsonString());
	
	}
		
	
	
	
}

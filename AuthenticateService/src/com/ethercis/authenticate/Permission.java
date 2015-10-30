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

import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.security.I_Permission;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Permission and checking. 
 * Normally specialized according to the policy support.<br>
 * This class is responsible to handle the implication of a permission from another
 * and also to return granted and revoked rights
 * @author C.Chevalley
 *
 */
public abstract class Permission implements Serializable, Cloneable, Comparable, I_Permission{

	private static final long serialVersionUID = 8803268741289303139L;
	private static final Logger logger = Logger.getLogger(Constants.LOGGER_SECURITY);

	protected String name;
	protected MethodName action;
	protected String objectName;
	protected Pattern pattern;
	protected Map<String, List<String>> parameters = new HashMap<String, List<String>>();
	protected List<String> granted = new LinkedList<String>();
	protected List<String> revoked = new LinkedList<String>();
	
	public Permission() {
		super();
	}
	
	protected void setParameterMap(Map<String, List<String>> map, String key, String value){
		if (map.containsKey(key)){
			List<String> list = map.get(key);
			list.add(value);
		}
		else{
			List<String> list = new ArrayList<String>();
			list.add(value);
			map.put(key, list);
		}		
	}

	/**
	 * setup a permission<p>
	 * @param name permission name
	 * @param action method or right for this permission
	 * @param objectName target name (ex. RESOURCE)
	 * @param pattern pattern associated to the target
	 * @param parameters a map of key,value pair parameters
	 */
	public Permission(String name, MethodName action, String objectName, String pattern, Map<String, String> parameters){
		this.name = name;
		this.action = action;
		this.objectName = objectName;
		if (pattern == null)
			this.pattern = null;
		else
			this.pattern = Pattern.compile(pattern);
		if (parameters != null)
			for (String key: parameters.keySet())
				setParameterMap(this.parameters, key, parameters.get(key));
	}
	/**
	 * compare this permission to another<p>
	 * @param o
	 * @return
	 */
	public int compareTo(Object o) {
		Permission compareTo = (Permission)o;
		int ret = -1;
		
		if (this.equals(compareTo))
			return 0;
		ret = this.name.compareTo(compareTo.name);
		return ret;
	}
	/**
	 * evaluate a pattern with a list of regular expressions<p>
	 * @param thisOne
	 * @param regexplist
	 * @return true if one regexp matches the pattern
	 */
	private boolean evaluatePattern(Pattern thisOne, List<String> regexplist){
		for (String withThisRegEx: regexplist){
			Matcher m = thisOne.matcher(withThisRegEx);
			logger.debug("pattern to check access="+withThisRegEx);
			logger.debug(" path to check:"+thisOne);
			boolean b = m.matches();
			logger.debug("access decision="+b);
			if (b)
				return b;
		}
		return false;
	}
	/**
	 * evaluate if two regular expressions match<p>
	 * @param regexp1
	 * @param regexp2
	 * @return true if match
	 */
	private boolean evaluatePattern(String regexp1, String regexp2){
		return Pattern.matches(regexp1, regexp2);
	}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#implies(com.ethercis.party.I_Permission)
	 */
	@Override
	public boolean implies(I_Permission another){
		if (this.name.compareTo(another.getName())==0)
				return true;
		//if an action is defined, it must be the same
		if (action != null){
			if (another.getAction()==null){
				logger.debug("no action defined for permission");
				return false;
			}
			else{
				if (action.compareTo(another.getAction())!= 0){
					logger.debug("Actions do not match:"+action+" with:"+another.getAction());
					return false;					
				}
			}
		}
		//check if this permission has the same object logonservice a target
		if (getObjectName() != null){
			String objectNamePattern1 = getObjectName();
			String objectName2compare = another.getObjectName();
			
			if (objectName2compare == null) //f.ex. when we test permission name only, no object defined 
				return false; 
			
			if (!evaluatePattern(objectNamePattern1, objectName2compare))
				return false;
			
			//CASE 1: policy pattern is null, assume matches any
			if (pattern == null){
				return impliesParameters(another);
			}
			//CASE 2: policy pattern is non null, compare with a null one assume false
			if (another.getFilter() == null)
				return false;
			List<String> alist = new ArrayList<String>();
			alist.add(another.getFilter());
			if (evaluatePattern(pattern, alist))
				return impliesParameters(another);
			else
				return false;
		}
		else
			return impliesParameters(another);
	}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#impliesParameters(com.ethercis.party.I_Permission)
	 */
	@Override
	public boolean impliesParameters(I_Permission another){
		if (parameters.size() > 0 && another.getParameters() == null){
			logger.debug("Missing required parameters in other permission");
			return false;
		}
		//loop into the defined parameters (if any) and check patterns
		for (String key: parameters.keySet()){
			//find out the same key in the other permission
			List<String> otherParameterRegex = another.getParameters().get(key);
			if (otherParameterRegex == null)
			{
				logger.debug("Parameter is required in evaluation: "+key);
				return false;
			}
			boolean bresult = false;
			for (String s: parameters.get(key)){
				Pattern p = Pattern.compile(s);
				bresult |= evaluatePattern(p, otherParameterRegex);
				if (bresult)
					break;
			}
			if ( !bresult)
			{
				logger.debug("Parameter regexp mismatch: "+ parameters.get(key)+" with:"+otherParameterRegex);
				return false;
			}			
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getUserId()
	 */
	@Override
	public String getName(){return name;}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getObjectName()
	 */
	@Override
	public String getObjectName() {return objectName;}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getFilter()
	 */
	@Override
	public String getFilter() {return pattern.pattern();}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getParameters()
	 */
	@Override
	public Map<String, List<String>> getParameters() {return parameters;}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getAction()
	 */
	@Override
	public MethodName getAction() {return action;}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getGranted()
	 */
	@Override
	public List<String> getGranted(){
		return granted;
	}
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getRevoked()
	 */
	@Override
	public List<String> getRevoked(){
		return revoked;
	}
	
	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#getPattern()
	 */
	@Override
	public Pattern getPattern() {
		return pattern;
	}

	/* (non-Javadoc)
	 * @see com.ethercis.party.I_Permission#toString()
	 */
	@Override
	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" name:   "+this.name);
        sb.append("\n object: "+this.objectName);
        sb.append("\n pattern: "+((this.pattern!=null)?this.pattern.pattern():""));
        for (String g: getGranted())
        	sb.append("\n granted:"+g);
        for (String r: getRevoked())
        	sb.append("\n revoked:"+r);
        sb.append("\n");
        return sb.toString();
	}	

}

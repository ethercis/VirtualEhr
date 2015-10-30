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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.service.I_Service;


/**
 * handles variable substition<p>
 * A variable is defined logonservice a string with the following format:<p>
 * <code>_$[.....]<code><p>
 * A variable is a call to a plugin defined or class method:
 * <ul>
 * <li>a plugin method is defined logonservice: <code>plugin,version.method(<i>args...</i>)</code>
 * <li>a class method is defined logonservice: <code>class.name.method(<i>args...</i>)</code>
 * <ul>
 * Examples:<p>
 * <code>TreeStringFormatter.format({24}, ,3;5)<code> is a class method
 * <code>TestSubVar,1.0.helloMethod(TestSubVar)</code> is a plugin method<p>
 * LIMITATIONS:<p>
 * <ul>
 * <li>embedded variables are not supported
 * <li>complex methods are not supported (eg. getInstance().getTime().toString())
 * <li>only String method parameters are supported
 * <li>only method returning string are supported
 * </ul>
 * @author christian
 *
 */
public class VarSubstitute {
	private static String LEFT_DELIM = "_$[";
	private static String RIGHT_DELIM = "]";
	
	private static Logger log = Logger.getLogger(VarSubstitute.class);
	
	/**
	 * resolve the variables contained in a string<p>
	 * Use this call from a plugin
	 * @param parent
	 * @param glob
	 * @param string
	 * @return
	 */
	public static String resolveString(I_Service parent, RunTimeSingleton glob, String string){
		
		while (string.contains(LEFT_DELIM)){
			int start = string.indexOf(LEFT_DELIM);
			int stop = string.indexOf(RIGHT_DELIM, start);
			String var = string.substring(start+3, stop );
			String substituted = substitute(parent, glob, var);
			string = string.substring(0, start)+substituted+string.substring(stop+1);
		}		
		return string;
	}
	
	/**
	 * resolve the variables contained in a string<p>
	 * @param parent
	 * @param glob
	 * @param string
	 * @return
	 */	
	public static String resolveString(String string){
		
		while (string.contains(LEFT_DELIM)){
			int start = string.indexOf(LEFT_DELIM);
			int stop = string.indexOf(RIGHT_DELIM, start);
			String var = string.substring(start+3, stop );
			String substituted = substitute(var);
			string = string.substring(0, start)+substituted+string.substring(stop+1);
		}		
		return string;
	}	
	/**
	 * perform variable substitution<p>
	 * Use this call from a plugin
	 * @param parent
	 * @param glob
	 * @param variable
	 * @return
	 */
	public static String substitute(I_Service parent, RunTimeSingleton glob, String variable){
		
		//unwrapp string
//		String unwrapped = unwrap(variable);
		String unwrapped = variable;
		String cname = getRootName(unwrapped);
		String methodName = getMethodName(unwrapped);
		String parameters = getParameters(unwrapped);
		
		if (isServiceRef(cname))
			return invokeServiceMethod(glob, parent, cname, methodName, parameters);
		else
			return invokeStaticClassMethod(cname, methodName, parameters);
	}

	/**
	 * perform variable substitution<p>
	 * @param parent
	 * @param glob
	 * @param variable
	 * @return
	 */
	public static String substitute(String variable){
		
		//unwrapp string
//		String unwrapped = unwrap(variable);
		String unwrapped = variable;
		String cname = getRootName(unwrapped);
		String methodName = getMethodName(unwrapped);
		String parameters = getParameters(unwrapped);
		
		return invokeStaticClassMethod(cname, methodName, parameters);
	}
	/**
	 * invoke a class method<p>
	 * @param className
	 * @param methodName
	 * @param parameters
	 * @return
	 */
	private static String invokeStaticClassMethod(String className, String methodName, String parameters){
		Class<?> clazz;
		try {
			clazz = Class.forName(className);
			Class[] signature = getMethodSignature(parameters);
			Method method = clazz.getDeclaredMethod(methodName, signature);
			//invoke wrapper
			Object[] args = getArguments(parameters);
			return (String)method.invoke(null, args);
		} catch (InvocationTargetException ite){
			log.error("Wrapper invocation failed:"+ite);
		} catch (IllegalAccessException iae){
			log.error("Wrapper invocation failed:"+iae);
		} catch (ClassNotFoundException e) {
			log.error("Invalid class name:"+e);			
		} catch (NoSuchMethodException nsme){
			log.error("Method could not be resolved:"+nsme);
			return "";
		}
		return "";
		
	}
	/**
	 * invoke a plugin method<p>
	 * @param glob
	 * @param parent
	 * @param plugin_raw
	 * @param method_name
	 * @param parameters
	 * @return
	 */
	private static String invokeServiceMethod(RunTimeSingleton glob, I_Service parent, String plugin_raw, String method_name, String parameters){
		I_Service plugin;
		//compare with this plugin
		if (plugin_raw.equals(parent.getType() +","+parent.getVersion()))
			plugin = parent;
		else		
			plugin = glob.getServiceRegistry().getService(plugin_raw);
		
		try {
			Class[] signature = getMethodSignature(parameters);
			Method method = plugin.getClass().getDeclaredMethod(method_name, signature);
			//invoke wrapper
			Object[] args = getArguments(parameters);
			return (String)method.invoke(plugin, args);
		} catch (InvocationTargetException ite){
			log.error("Wrapper invocation failed:",ite);
		} catch (IllegalAccessException iae){
			log.error("Wrapper invocation failed:",iae);
		} catch (NoSuchMethodException nsme){
			log.error("Method could not be resolved:",nsme);
			return "";
		}
		return "";
	}
	/**
	 * get the class or plugin part of a variable<p>
	 * @param cname
	 * @return
	 */
	private static String getRootName(String cname){
		//get occurence of last '.' (dot) and return the substring up to this dot
		int pos = getMethodNameStart(cname);
		return cname.substring(0,pos);
	}
	/**
	 * get method<p>
	 * A method name starts with the first name containing '()'
	 * @param cname
	 * @return
	 */
	private static String getMethodName(String cname){
		String mraw = cname.substring(getMethodNameStart(cname)+1);
		//trim parameters if any
		if (mraw.contains("("))
			return mraw.substring(0, mraw.indexOf("("));
	
		return mraw;
	}
	/**
	 * get the parameters string of a method<p>
	 * @param variable
	 * @return
	 */
	private static String getParameters(String variable){
		//strip the variable from the first '(' to the last ')'
		return variable.substring(variable.indexOf("(")+1, variable.length() - 1);		
	}
	/**
	 * returns true if this root is a plugin<p>
	 * @param root
	 * @return
	 */
	private static boolean isServiceRef(String root){
		//check for a format like 'abcd[n.m]'
		//string can be split with '['
		//the last char of the second string is ']'
		//the first string does not contain dot
		return (root.contains(","));
	}
	
	private static String unwrap(String var){
		int start = var.indexOf("_$[");
		int stop = var.indexOf("]", start);
		return  var.substring(start+3, stop );		
	}
	/**
	 * create a method signature<p>
	 * @param parameters
	 * @return
	 */
	private static Class[] getMethodSignature(String parameters){
		if (parameters.length() == 0)
			return new Class[]{};
		
		if (!parameters.contains(","))
			return new Class[]{String.class};
		
		//more than one args
		int cardinal = parameters.split(",").length;
		ArrayList<Class> cal = new ArrayList<Class>();
		for (int i = 0; i < cardinal; i++){
			cal.add(String.class);
		}
		return cal.toArray(new Class[0]);
	}
	/**
	 * return the list of arguments logonservice Objects<p>
	 * @param parameters
	 * @return
	 */
	private static Object[] getArguments(String parameters){
		if (parameters.length() == 0)
			return new Object[]{};
		
		if (!parameters.contains(","))
			return new Object[]{strip_quotes(parameters)};

		ArrayList<Object> oal = new ArrayList<Object>();
		for (String s: parameters.split(",")){
			oal.add(strip_quotes(s));
		}
		return oal.toArray(new Object[0]);
	}
	/**
	 * removed both single and double quotes
	 * @param aparm
	 * @return
	 */
	private static String strip_quotes(String aparm){
		String result = aparm.replaceAll("\\'", "");
		return result.replaceAll("\"", "");
	}
	
	private static int getMethodNameStart(String cname){
		int loc = cname.indexOf("(");
//		int pos = cname.lastIndexOf(".");
		int pos = -1;
		for (int i = loc; i >=0 ; i--){
			if (cname.charAt(i)=='.'){
				pos = i;
				break;
			}
		}		
		return pos;
	}
	
	private class MethodDef {
		String name;
		String parameters;
		Class[] signature;
		Object[] arguments;
		public MethodDef(String name, String parameters) {
			super();
			this.name = name;
			this.parameters = parameters;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getParameters() {
			return parameters;
		}
		public void setParameters(String parameters) {
			this.parameters = parameters;
		}
		public Object[] getArguments() {
			return arguments;
		}
		public void setArguments(Object[] arguments) {
			this.arguments = arguments;
		}
		public Class[] getSignature() {
			return signature;
		}
		public void setSignature(Class[] signature) {
			this.signature = signature;
		}
	}
}

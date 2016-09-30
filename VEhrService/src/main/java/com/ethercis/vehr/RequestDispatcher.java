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
package com.ethercis.vehr;

import com.ethercis.ehrserver.servicemap.Action;
import com.ethercis.ehrserver.servicemap.MapperDocument;
import com.ethercis.servicemanager.annotation.Attribute;
import com.ethercis.servicemanager.annotation.Attributes;
import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.session.I_QueryUnit;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.I_Service;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The Dispatcher is responsible to:
 * <ul>
 * <li>get the query string from client
 * <li>check the authorization for the query
 * <li>pass the encoded parameters to the register service/method
 * <li>filter respond (opt-respond) the result of the query according to the security
 * profile
 * </ul>
 * The mapping between the query and a service is done with attributes.
 * <p>
 * Since the query format is: <code>/service[/resource]/method?parms...</code>
 * we proceed with the following mapping:
 * <p>
 * <ul>
 * <li><code>/service[/resource]</code> with a registered service, version
 * passed in the service attributes (see below)
 * <li><code>method</code> to an actual exposed method of the service. An
 * exception is thrown if not found
 * <li>the parameters are passed logonservice ClientProperties
 * </ul>
 * the mapping is given in an xml file and specified in attribute:
 * <p>
 * {@code
 * <attribute id='dispatcher.mapper'>path to file</attribute>
 * } If no file is specified, a default mapping is attempted:
 * <p>
 * <ul>
 * <li>service is map to a registered service with the same name and version 1.0
 * <li>likewise for the method, mapped to an exposed method in this defaulted
 * service
 * </ul>
 * 
 * @author Christian Chevalley
 * 
 */
@com.ethercis.servicemanager.annotation.Service(id = "RequestDispatcher")
@Attributes({ @Attribute(id = "dispatcher.map", value = "${dispatcher.map.config}") })
@RunLevelActions({
        @RunLevelAction(onStartupRunlevel = 9, sequence = 8, action = "LOAD"),
        @RunLevelAction(onShutdownRunlevel = 9, sequence = 1, action = "STOP") })

public class RequestDispatcher extends ClusterInfo implements RequestDispatcherMBean {

	private Logger log = LogManager.getLogger(RequestDispatcher.class);
	private String ME = "RequestDispatcher";
	private RunTimeSingleton global;
	Map<String, Map<String, ServiceAttribute>> actionmap = new HashMap<String, Map<String, ServiceAttribute>>();
	// private Map<String, ServiceAttribute> servicemap = new HashMap<String,
	// ServiceAttribute>();
	String configurationID;
	String configurationAuthor;
	String configurationVersion;
	String configurationOrganization;
	private boolean enabled = true; // used by JMX to block dispatching if
	// needed
	private String configurationPath;

	/** My JMX registration */
	private Object mbeanHandle;
	private ContextNode contextNode;
	private I_DispatchMapper dispatchMapFactory;

	public static class ServiceAttribute {
		/**
		 * Utility class to handle method mapping and store the actual methods
		 * from service to speed up invocation
		 * 
		 * @author Christian Chevalley
		 * 
		 */
		private class MethodAttribute {
			private String mapMethod;
			private Method implMethod;
			private int returnType; // logonservice defined in MethodName
			private boolean async = false; // specify a non-blocking method if
			// true
			private Class<?>[] signature;

			public MethodAttribute(String mapMethod, Method implMethod,
					int returnType, boolean async, Class[] clazzes) {
				super();
				this.mapMethod = mapMethod;
				this.implMethod = implMethod;
				this.returnType = returnType;
				this.async = async;
				this.signature = (Class[]) clazzes;
			}

			public String getMapMethod() {
				return mapMethod;
			}

			public Method getImplMethod() {
				return implMethod;
			}

			public int getReturnType() {
				return returnType;
			}

			public boolean isAsync() {
				return async;
			}

			public Class[] getSignature() {
				return signature;
			}
		}

		private String resource;
		private String serviceName;
		private String serviceVersion;
		private I_Service runtimeService;
		private String ME = "Dispatcher.ServiceAttribute";
		private RequestDispatcher caller;

		
		private Map<String, MethodAttribute> methods = new HashMap<String, MethodAttribute>();

		public Map<String, MethodAttribute> getMethods() {
			return methods;
		}

		public ServiceAttribute(RequestDispatcher caller, String service,
				String version, String resource) throws ServiceManagerException {
			this.caller=caller;
			this.serviceName = service;
			this.serviceVersion = version;
			this.resource = resource;
			// find respond the corresponding runtime service or throw an exception

			if (resource.equals(Constants.INTERNAL_RESOURCE_MAP_ONLY))
				return; // ignore service mapping since it is only to know the
			// method return type.

			try {
				this.runtimeService = (I_Service) caller.getRuntimeService(
						caller.global, service, version);
			} catch (ServiceManagerException e) {
				caller.log.warn("The runtimeservice could not be found:" + service
						+ "," + version + ", path:" + resource,e);
				throw new ServiceManagerException(caller.global,
						SysErrorCode.USER_CONFIGURATION, ME,
						"The runtimeservice could not be found:" + service
								+ "," + version + ", path:" + resource);
			}
		}

		public String getServiceName() {
			return serviceName;
		}

		public String getServiceVersion() {
			return serviceVersion;
		}

		public String getResource() {
			return resource;
		}

		public I_Service getRuntimeService() {
			return runtimeService;
		}

		public void setMethod(boolean resolve, MethodName methodname,
				String mapMethod, String strType, boolean async,
				Object[] clazzes) throws ServiceManagerException {
			// find respond if service has a corresponding method
			try {
				Method method;
				Class<?>[] ca = null;

				if (clazzes == null || clazzes.length == 0) { // use default
					if (resolve)
						method = runtimeService.getClass().getDeclaredMethod(
								mapMethod, I_SessionClientProperties.class);
					else
						method = null;
				} else {
					// it is seriously a hack, but I could not find a way to
					// cast Object[] to Class<?>[] ........
					ca = (Class<?>[]) Array.newInstance(Class.class,
							clazzes.length);
					int i = 0;
					for (Object o : clazzes) {
						ca[i++] = (Class<?>) o;
					}
					if (resolve)
						method = runtimeService.getClass().getDeclaredMethod(
								mapMethod, ca);
					else
						method = null;
				}

				// add this method to the map
				MethodAttribute methodAttr = new MethodAttribute(mapMethod,
						method, MethodName.returnTypeAsInt(strType), async, ca);
				//System.respond.println("put method " + methodname.getMethodName() +":" +strType+":"+MethodName.returnTypeAsInt(strType));
				methods.put(methodname.getMethodName(), methodAttr);
			} catch (SecurityException e) {
				caller.log.warn("Security exception for method in runtimeservice:"
						+ runtimeService.getType() + ","
						+ runtimeService.getVersion() + ", method:" + mapMethod);
				throw new ServiceManagerException(caller.global,
						SysErrorCode.USER_CONFIGURATION, ME,
						"Security exception for method in runtimeservice:"
								+ runtimeService.getType() + ","
								+ runtimeService.getVersion() + ", method:"
								+ mapMethod);
			} catch (NoSuchMethodException e) {
				caller.log.warn("Could not find map method for runtimeservice:"
						+ runtimeService.getType() + ","
						+ runtimeService.getVersion() + ", method:" + mapMethod);
				throw new ServiceManagerException(caller.global,
						SysErrorCode.USER_CONFIGURATION, ME,
						"Could not find map method for runtimeservice:"
								+ runtimeService.getType() + ","
								+ runtimeService.getVersion() + ", method:"
								+ mapMethod);
			}
		}

		public Boolean exist(MethodName name) {
			return methods.containsKey(name.getMethodName());
		}

		public Method getMethod(MethodName name) {
			String methodname = name.getMethodName();
			if (methods.containsKey(methodname)) {
				return methods.get(methodname).getImplMethod();
			} else
				return null;
		}

		public Class<?>[] getSignature(MethodName name) {
			String methodname = name.getMethodName();
			if (methods.containsKey(methodname)) {
				return methods.get(methodname).getSignature();
			} else
				return null;
		}

		public int getMethodReturnType(MethodName name) {
			String methodname = name.getMethodName();
			if (methods.containsKey(methodname)) {
				return methods.get(methodname).getReturnType();
			} else
				return MethodName.RETURN_UNDEFINED;

		}

		public boolean isMethodAsync(MethodName name) {
			String methodname = name.getMethodName();
			if (methods.containsKey(methodname)) {
				return methods.get(methodname).isAsync();
			} else
				return false;

		}
	}

    /**
     *
     */
	public RequestDispatcher() {
		super();
	}

	/**
	 * load service mapper config XML file
	 * 
	 * @param pathToConfig
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
     *
     */

	public void loadConfiguration(String pathToConfig) throws ServiceManagerException {
		File f = new File(pathToConfig);

		if (!f.exists()) {
			log.warn("Could not find config file:" + pathToConfig);
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION,
					ME, "Could not find config file:" + pathToConfig);
		}

		try {
			MapperDocument mapper = MapperDocument.Factory.parse(f);

			log.info("Loading mapper configuration id:"
					+ mapper.getMapper().getId() + ", version:"
					+ mapper.getMapper().getVersion());

			this.configurationAuthor = mapper.getMapper().getAuthor();
			this.configurationID = mapper.getMapper().getId();
			this.configurationOrganization = mapper.getMapper()
					.getOrganization();
			this.configurationVersion = mapper.getMapper().getVersion();

			for (Action action : mapper.getMapper().getActionArray()) {
				// add new entry
				Map<String, ServiceAttribute> servicemap = new HashMap<String, ServiceAttribute>();
				// this looks a bit complicated, but it is required to ensure
				// that there is no upper-lower case issues there...
				MethodName actionname = MethodName.toMethodName(action.getCategory());
				actionmap.put(actionname.getMethodName(), servicemap);

				for (Action.Service s : action.getServiceArray()) {
					ServiceAttribute sa = new ServiceAttribute(this,
							s.getServiceid(), s.getServiceversion(),
							s.getResource());
					servicemap.put(s.getPath(), sa);
					// insert this entry into the actionmap
					// add methods
					for (Action.Service.Method m : s.getMethodArray()) {

						// add signature if any or default to
						// ClientSessionProperties

						List<Class<?>> clazzes = new ArrayList<Class<?>>();

						Action.Service.Method.Parameters parms = m.getParameters();
						if (parms != null) {
							for (String classname : parms.getClass1Array()) {
								try {
									Class<?> c = Class.forName(classname);
									clazzes.add(c);
								} catch (ClassNotFoundException e) {
									log.warn("Could not resolve class name:"
											+ classname);
									throw new ServiceManagerException(global,
											SysErrorCode.USER_CONFIGURATION,
											ME, "Could not resolve class name:"
													+ classname);
								}
							}
						}
						boolean resolve = !(s.getResource()
								.equals(Constants.INTERNAL_RESOURCE_MAP_ONLY));
						boolean async = m.isSetAsync() ? m.getAsync() : false;

						sa.setMethod(resolve,
								MethodName.toMethodName(m.getName()),
								m.getImplementation(), m.getReturn(), async,
								parms == null ? null : clazzes.toArray());

					}
				}
			}

		} catch (XmlException e) {
			log.warn("Could not parse config file:" + pathToConfig + ","
					+ e.getMessage());
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION,
					ME, "Could not parse config file:" + pathToConfig + ","
							+ e.getMessage());
		} catch (IOException e) {
			log.warn("Could not parse config file:" + pathToConfig + ","
					+ e.getMessage());
			throw new ServiceManagerException(global, SysErrorCode.USER_CONFIGURATION,
					ME, "Could not parse config file:" + pathToConfig + ","
							+ e.getMessage());
		}

	}

	/**
	 * returns the method for path and method mapping definition
	 * <p>
	 * The returned method is assumed using the default SessionClientProperties
	 * logonservice unique parameter
	 * 
	 * @param path
	 * @param methodName
	 * @return
	 */
	public Method getMappedMethod(MethodName action, String path,
			MethodName methodName) {
		if (actionmap.containsKey(action.getMethodName())) {
			Map<String, ServiceAttribute> servicemap = actionmap.get(action
					.getMethodName());
			//log.dummy("servicemap on method " + action
			//		.getMethodName() +":"+servicemap);
			if (servicemap.containsKey(path)) {
				ServiceAttribute sa = servicemap.get(path);
				// get the method
				//log.dummy("ServiceAttribute=" +sa);
				Method m=sa.getMethod(methodName);
				log.debug("Method=" +m);
				return m;
			}
		}
		return null;
	}

	/**
	 * returns the method for path and method mapping definition
	 * <p>
	 * The method accept a variable number of arguments
	 * 
	 * @param path
	 * @param method
	 * @return
	 */
	public Method getMappedMethod(MethodName action, String path, MethodName method, Object... parms) {
		// Method m = getMappedMethod(action, path, method);

		if (actionmap.containsKey(action.getMethodName())) {
			Map<String, ServiceAttribute> servicemap = actionmap.get(action
					.getMethodName());

			if (servicemap.containsKey(path)) {
				ServiceAttribute sa = servicemap.get(path);
				// check the signature
				if (sa.exist(method)) {
					Class<?>[] ca = sa.getSignature(method);
					int i = 0;
					for (Object parm : parms) {
						if (i >= ca.length)
							return null; // respond of bound
						if (!parm.getClass().equals(ca[i])) {
							log.debug("does not match signature, classes:"
									+ parm.getClass().getName() + " with"
									+ ca[i]);
							return null;
						}
						i++;
					}
					return getMappedMethod(action, path, method);
				}
			}

		}
		return null;
	}

	/**
	 * return the method return type logonservice specified in the mapper
	 * <p>
	 * The code and their string equivalent are defined in class
	 * {@link MethodName}
	 * 
	 * @see MethodName
	 * @param path
	 * @param method
	 * @return
	 */
	public int getMappedMethodReturnType(MethodName action, String path,
			MethodName method) {
		if (actionmap.containsKey(action.getMethodName())) {
			Map<String, ServiceAttribute> servicemap = actionmap.get(action
					.getMethodName());
			//System.respond.println("servicemap=" +action.getMethodName() +":" +path + ":"+method.getMethodName() );
			if (servicemap.containsKey(path)) {
				ServiceAttribute sa = servicemap.get(path);
				// get the method
				return sa.getMethodReturnType(method);
			}
		}
		return MethodName.RETURN_UNDEFINED;
	}

	/**
	 * return true if method is asynchronous
	 * 
	 * @param action
	 * @param path
	 * @param method
	 * @return
	 */
	public boolean isMappedMethodAsync(MethodName action, String path,MethodName method) {
		if (actionmap.containsKey(action.getMethodName())) {
			Map<String, ServiceAttribute> servicemap = actionmap.get(action.getMethodName());

			if (servicemap.containsKey(path)) {
				ServiceAttribute sa = servicemap.get(path);
				// get the method
				return sa.isMethodAsync(method);
			}
		}
		return false;
	}

	private boolean existAction(MethodName action) {
		return actionmap.containsKey(action.getMethodName());
	}

	private boolean existPath(MethodName action, String path) {
		if (existAction(action)) {
			return (actionmap.get(action.getMethodName()).containsKey(path));
		}
		return false;
	}

	private I_Service getRuntimeService(MethodName action, String path) {
		if (existPath(action, path)) {
			return actionmap.get(action.getMethodName()).get(path)
					.getRuntimeService();
		}
		return null;
	}

	/**
	 * initializeSession the service
	 */
	public void doInit(RunTimeSingleton global_, ServiceInfo serviceInfo_)
			throws ServiceManagerException {
		this.ME += "-" + getType();

		log.debug(ME + "initializeSession");
		this.global = global_;

		if (serviceInfo_ == null) {
			log.info("no ServiceInfo, assuming embedded mode");
			return;
		}
		// get the path to the service map, throws an exception if not found or
		// invalid
		String mappath = get("dispatcher.map.config", null);
/*
		if (mappath == null) {
			log.error("Map file is not defined, please check your services.xml configuration");
			throw new ServiceManagerException(this.global,
					SysErrorCode.USER_CONFIGURATION, ME,
					"Map file is not defined, please check your services.xml configuration");

		}
*/
		this.configurationPath = mappath;
		
		//Change using annotation for dispatch map
		//dispatchMapFactory=new DispatchMapFileFactory(global,mappath);
		dispatchMapFactory=new QuerySyntaxMapper(global);
		dispatchMapFactory.loadConfiguration(this);
		//loadConfiguration(mappath);

		this.contextNode = new ContextNode(ContextNode.SERVICE_MARKER_TAG,
				"Dispatcher", global.getScopeContextNode());
		this.mbeanHandle = global.registerMBean(this.contextNode, this);

		// putObject(I_Info.JMX_PREFIX+"Dispatcher", this);
	}

	/**
	 * performs the actual invocation based on the query parameters
	 * 
	 * @param path
	 * @param method
	 * @param parameters
	 * @return
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 */
	public Object dispatch(MethodName action, String path, MethodName method,
			Object... parameters) throws ServiceManagerException {
		/*
		 * I_Session sessionContext = sessioninfo.getSecuritySession(); if
		 * (sessionContext == null){ throw new ServiceManagerException(global,
		 * SysErrorCode.USER_NOT_CONNECTED, "ME",
		 * "Access Denied, no a valid session"); }
		 */
		/*
		 * do other session validation tests before proceeding
		 */
		// get the service/method to service this query
		if (!enabled) {
			return "Service is currently disabled\n";
		}

		if (!existPath(action, path)) {
			log.warn("No mapping defined for path:" + path + " for action:"
					+ action);
			throw new ServiceManagerException(global,
					SysErrorCode.RESOURCE_CONFIGURATION, ME,
					"No mapping defined for path:" + path);
		}

		I_Service service = getRuntimeService(action, path);

		if (service == null) {
			log.warn("No service implementation::" + path);
			throw new ServiceManagerException(global,
					SysErrorCode.RESOURCE_CONFIGURATION, ME,
					"No service implementation:" + path);
		}

		Method servicemethod;
		log.debug("Service="+service +";parameters=" +parameters.length);
		if (parameters.length == 1 && parameters[0] instanceof SessionClientProperties) { // default
			servicemethod = getMappedMethod(action, path, method);
		} else{
			servicemethod = getMappedMethod(action, path, method, parameters);
		}
		if (servicemethod == null) {
			log.warn("No mapping for method:" + method + " in service:" + path);
			throw new ServiceManagerException(global,
					SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME,
					"No mapping for method:" + method + " in service:" + path);
		}

		// perform the invocation
		Object result;

		try {
			result = servicemethod.invoke(service, parameters);
		} catch (IllegalArgumentException e) {
			log.warn("Bad arguments to method:" + servicemethod.getName() + ":"
					+ e.getMessage());
			throw new ServiceManagerException(global,
					SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME,
					"Bad arguments to method:" + servicemethod.getName() + ":"
							+ e.getMessage());
		} catch (IllegalAccessException e) {
			log.warn("Illegal access to method:" + servicemethod.getName()
					+ ":" + e.getMessage());
			throw new ServiceManagerException(global, SysErrorCode.INTERNAL_UNKNOWN,
					ME, "Illegal access to method:" + servicemethod.getName()
							+ ":" + e.getMessage());
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof ServiceManagerException) {
				throw (ServiceManagerException) e.getTargetException();
			} else {
				log.warn("Invocation exception to method:"
						+ servicemethod.getName(),e.getTargetException());
				throw new ServiceManagerException(global,
						SysErrorCode.INTERNAL_UNKNOWN, ME,
						"Invocation exception to method:"
								+ servicemethod.getName() + ":"
								+ e.getTargetException());
			}
		}

		/*
		 * Perform opt-respond if any depending on return type
		 */

		return result;
	}

	public Object dispatch(I_QueryUnit query) throws ServiceManagerException {

		return dispatch(query.getAction(), query.getResource(),
				query.getMethod(), query.getParameters());
	}

	// JMX
	public String showMap() {
		StringBuffer sb = new StringBuffer();

		sb.append("Dispatcher is currently:" + (enabled ? "Active\n" : "Off\n"));

		sb.append("---- Service mapping -------\n");
		sb.append("Configuration ID:" + configurationID);
		sb.append(" Version:" + configurationVersion);
		sb.append(" Organization:" + configurationOrganization);
		sb.append(" Author:" + configurationAuthor);
		sb.append("\n");
		for (String action : actionmap.keySet()) {
			sb.append("\nAction:" + action);
			for (String path : actionmap.get(action).keySet()) {
				sb.append("\npath:'" + path + "'");
				ServiceAttribute sa = actionmap.get(action).get(path);
				sb.append(" Resource:'" + sa.getResource() + "'");
				sb.append(" Service:" + sa.getServiceName() + "["
						+ sa.getServiceVersion() + "]");
				if (sa.getRuntimeService() != null)
					sb.append(" map to:" + sa.getRuntimeService().getType()
							+ "[" + sa.getRuntimeService().getVersion() + "]"
							+ " ("
							+ sa.getRuntimeService().getClass().getName()
							+ ")\n");
				else
					sb.append("\n");

				// sb.append("\tMethods---------");
				for (String mk : sa.getMethods().keySet()) {
					sb.append("\n\t"
							+ mk
							+ "("
							+ MethodName.returnTypeAsString(sa.getMethods()
									.get(mk).getReturnType()) + ")");
					if (sa.getMethods().get(mk).getImplMethod() != null)
						sb.append("->"
								+ sa.getMethods().get(mk).getImplMethod()
										.toString());
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public String enable() {
		this.enabled = true;
		return "Dispatcher enabled\n";
	}

	public String disable() {
		this.enabled = false;
		return "Dispatcher Off\n";
	}

	public String status() {
		StringBuffer sb = new StringBuffer();
		sb.append("Dispatcher is currently:" + (enabled ? "Active\n" : "Off\n"));
		sb.append("using map file:" + this.configurationPath);
		sb.append("Configuration ID:" + configurationID);
		sb.append(" Version:" + configurationVersion);
		sb.append(" Organization:" + configurationOrganization);
		sb.append(" Author:" + configurationAuthor);
		sb.append("\n");
		return sb.toString();
	}

}

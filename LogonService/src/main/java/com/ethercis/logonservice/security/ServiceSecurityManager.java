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

/*
This code is a refactoring and adaptation of the original
work provided by the XmlBlaster project (see http://xmlblaster.org)
for more details.
This code is therefore supplied under LGPL 2.1
 */
//Copyright
package com.ethercis.logonservice.security;

import com.ethercis.logonservice.session.Session;
import com.ethercis.servicemanager.annotation.*;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.SysErrorCode;
import com.ethercis.servicemanager.common.security.I_Manager;
import com.ethercis.servicemanager.common.security.I_Session;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.jmx.AnnotatedMBean;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.util.Factory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * SecurityManager service, manages access and permissions.
 * @author christian
 *
 */

@Service(id = "ServiceSecurityManager",system=true)
@Attributes({ @Attribute(id = "server.security.policy.type", value = "SHIRO") ,
              @Attribute(id = "server.security.shiro.inipath", value = "resources/authenticate.ini")
            })
@RunLevelActions(value = {
		@RunLevelAction(onStartupRunlevel = 6, sequence = 1, action = "LOAD"),
		@RunLevelAction(onShutdownRunlevel = 8, sequence = 5, action = "STOP") })

public class ServiceSecurityManager extends ClusterInfo implements I_Manager, ServiceSecurityManagerMBean {
	private static Logger log = Logger.getLogger(Constants.LOGGER_SECURITY);
	private static String ME = ServiceSecurityManager.class.getName();
//	private Policy policy;
	private RunTimeSingleton global;
	private String policypath;
	protected Map<String, I_Session> sessions = new HashMap<String, I_Session>();
	private int error = 0;
	private int policyMode = Constants.POLICY_UNDEF;

    private final String serviceId = "ServiceSecurityManager";
	
	public ServiceSecurityManager(){}
	/**
	 * used by the message bus<p>
	 */
	public I_Session reserveSession(String sessionId) throws ServiceManagerException {
		log.debug("reserveSession(sessionId="+sessionId+")");
		Session session = new Session(this, sessionId);
		synchronized(sessions) {
			sessions.put(sessionId, session);
		}
		
		return session;
	}

	/**
	 * used by the message bus<p>
	 */
	public void releaseSession(String sessionId, String qos_literal) throws ServiceManagerException {
		synchronized(sessions) {
			if (sessions.get(sessionId)!= null){
				//adjust quota
				String ipaddress = ((Session)sessions.get(sessionId)).getIPAddress();
			}
			sessions.remove(sessionId);
			
		}	
	}

	/**
	 * used by the message bus<p>
	 */
	public void changeSecretSessionId(String oldSessionId, String newSessionId) throws ServiceManagerException {
	      synchronized(sessions) {
	         Session session = (Session)sessions.get(oldSessionId);
	         if (session == null) throw new ServiceManagerException(this.global, SysErrorCode.INTERNAL_CONNECTIONFAILURE, ME+".unknownSessionId", "Unknown sessionId!");
	         if (sessions.get(newSessionId) != null) throw new ServiceManagerException(this.global, SysErrorCode.INTERNAL_CONNECTIONFAILURE, ME+".invalidSessionId", "This sessionId is already in use!");
	         sessions.put(newSessionId, session);
	         sessions.remove(oldSessionId);
	      }
	}
	
	/**
	 * used by the message bus<p>
	 */
	public I_Session getSessionById(String id) throws ServiceManagerException {
		synchronized(sessions) {
			return (I_Session)sessions.get(id);
		}
	}

	/**
	 * initialize the service<p>
	 * Service initialization consists in:<p>
	 * <ul>
	 * <li>loading the policy file</li>
	 * </ul>
	 */
	public void doInit(RunTimeSingleton glob, ServiceInfo serviceInfo) throws ServiceManagerException {
		this.global = (glob == null) ? RunTimeSingleton.instance() : glob;

        String policyType = get(Constants.POLICY_TYPE_TAG, "DEBUG");

//        if (serviceInfo != null && serviceInfo.getParameters().containsKey(Constants.POLICY_TYPE_TAG))
//            policyType = (String) serviceInfo.getParameters().get(Constants.POLICY_TYPE_TAG);
//        else //look in environment or default
//		    policyType = global.getProperty().get(Constants.POLICY_TYPE_TAG, "DEBUG");

        switch (policyType){ //Java 1.8 !
            case "XML":
                policyMode = Constants.POLICY_XML;
                break;
            case "LDAP":
                policyMode = Constants.POLICY_LDAP;
                break;
            case "JDBC":
                policyMode = Constants.POLICY_JDBC;
                break;
            case "DEBUG":
                policyMode = Constants.POLICY_DEBUG;
                break;
            case "SHIRO":
                policyMode = Constants.POLICY_SHIRO;
                //initialize Shiro security manager with the specified policy
                try {
                    String inipath = (String) serviceInfo.getParameters().get("server.security.shiro.inipath");
                    if (inipath != null){
                        inipath = global.getProperty().get("server.security.shiro.inipath", "");
                        if (inipath.length() == 0){
                            throw new ServiceManagerException(global, SysErrorCode.INTERNAL_ILLEGALARGUMENT, ME, "No ini path supplied for Shiro configuration, please set server.security.shiro.inipath");

                        }
                    }
                    Ini configuration = new Ini();
                    InputStream inputStream = new FileInputStream(inipath);
                    configuration.load(inputStream);
                    Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory(configuration);
                    org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
                    SecurityUtils.setSecurityManager(securityManager);
                } catch (Exception e){
                    throw new ServiceManagerException(glob, SysErrorCode.RESOURCE_CONFIGURATION, ME, "Could not initialize Shiro framework:"+e);
                }
                break;
            default:
                throw new IllegalArgumentException("Supplied policy mode is not supported:"+policyType);

        }

		AnnotatedMBean.RegisterMBean(serviceId, ServiceSecurityManagerMBean.class, this);
	}

	public String getType() {
		return serviceId;
	}

	public String getVersion() {
		return "1.0";
	}

	public void shutdown() throws ServiceManagerException {
		// TODO Auto-generated method stub
		
	}
	
	public RunTimeSingleton getGlobal(){
		return global;
	}

	public int getPolicyMode(){
		return policyMode;
	}

}

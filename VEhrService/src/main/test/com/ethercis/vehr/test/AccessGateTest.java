//Copyright
package com.ethercis.vehr.test;

import com.ethercis.logonservice.LogonService;
import com.ethercis.logonservice.security.ServiceSecurityManager;
import com.ethercis.logonservice.session.I_SessionManager;
import com.ethercis.logonservice.session.ResponseHolder;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.common.Base64;
import com.ethercis.servicemanager.common.HttpParameters;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import com.ethercis.servicemanager.common.def.MethodName;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.runlevel.I_ServiceRunMode;
import com.ethercis.servicemanager.service.I_Service;
import com.ethercis.vehr.AccessGateService;
import com.ethercis.vehr.I_DispatchMapper;
import com.ethercis.vehr.FileDispatchMapper;
import com.ethercis.vehr.RequestDispatcher;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AccessGateTest extends TestCase {
	private Logger log = Logger.getLogger(AccessGateTest.class);
	private AccessGateService accessGateService;
	private RunTimeSingleton global;
	private SessionClientProperties hdrprops = new SessionClientProperties(global); //empty fake http header props
	
	protected void setUp() throws Exception {
		log.info("Fixture initialized...");
		
		//create a new Authenticate instance
		global = RunTimeSingleton.instance().getClone((new String[]{"-server.security.policy.xml.path","test/resources/policy.xml",
													"-dispatcher.map", "test/resources/dummymap.xml"}));

		//load the security plugin
		ServiceSecurityManager manager = new ServiceSecurityManager();
		manager.init(global, null);

		global.getRunlevelManager().initServiceManagers();
		global.getServiceRegistry().register("ServiceSecurityManager,1.0", manager);

		//create a logon service:
		LogonService logonsrv = new LogonService();
		global.getServiceRegistry().register("LogonService,1.0", logonsrv);
		
		//create the dispatcher service...
		I_Service dummy = new DummyService();
		global.getServiceRegistry().register("DUMMY,1.0", dummy);

		RequestDispatcher dispatch = new RequestDispatcher();
		global.getServiceRegistry().register("RequestDispatcher,1.0", dispatch);
		
		logonsrv.doInit(global, null);
		dispatch.doInit(global, null);
		I_DispatchMapper dispatchMapFactory=new FileDispatchMapper(global,"test/resources/dummymap.xml");
		dispatchMapFactory.loadConfiguration(dispatch);
		//dispatch.loadConfiguration("src/test/resources/dummymap.xml");

		accessGateService = new AccessGateService();
		accessGateService.doInit(global, null);
		
		global.getServiceRegistry().register("AccessGateService,1.0", accessGateService);
		
	}

	@Test
		public void testAccessGateService2() throws ServiceManagerException {
			SessionClientProperties props = new SessionClientProperties(global);
			
			//simple login for user Joe
			props.addClientProperty(I_SessionManager.USER_ID, "joe");
			props.addClientProperty(I_SessionManager.USER_PASSWORD, "");

            ResponseHolder responseHolder = (ResponseHolder) accessGateService.queryHandler(MethodName.GET,hdrprops, "vehr", MethodName.CONNECT, props);
            SessionClientProperties retprops = responseHolder.getSessionClientProperties();
			
			String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
			assertNotNull(sessionName);
			
			String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
			
			//let's do some real queries now...
			
			SessionClientProperties hdrprops = new SessionClientProperties(global);
			
			//add the session id to validate a query
			hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
			
			//perform a dummy query (should fail on unauthorized...)
			SessionClientProperties parms = new SessionClientProperties(global);
	//		try {
	//			accessGateService.queryHandler(hdrprops, "dummy", "get", parms);
	//			fail("hmmm... the policy check did not throw an exception?");
	//		} catch (ServiceManagerException e){
	//			;
	//		}
			try {
				accessGateService.queryHandler(MethodName.GET, hdrprops, "ReportQuery", MethodName.GET, parms);
				fail("hmmm... the policy check did not throw an exception?");
			} catch (ServiceManagerException e){
				;
			}
			
			//and disconnect nicely...
			retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		}

	@Test
	public void testAccessGateService() throws ServiceManagerException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user Joe
		props.addClientProperty(I_SessionManager.USER_ID, "joe");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		ResponseHolder responseHolder = (ResponseHolder) accessGateService.queryHandler(MethodName.GET,hdrprops, "vehr", MethodName.CONNECT, props);

        SessionClientProperties retprops = responseHolder.getSessionClientProperties();

		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		SessionClientProperties parms = new SessionClientProperties(global);
//		try {
//			accessGateService.queryHandler(hdrprops, "dummy", "get", parms);
//			fail("hmmm... the policy check did not throw an exception?");
//		} catch (ServiceManagerException e){
//			;
//		}
		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "ReportQuery", MethodName.GET, parms);
			fail("hmmm... the policy check did not throw an exception?");
		} catch (ServiceManagerException e){
			;
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
	}
	
	@Test
	public void testCheckPermission() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
        ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET,hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>(); 
		hp.put("action", new String[]{"view"});
		hp.put("resource", new String[]{"button"});
		hp.put("pattern", new String[]{"SendNow"});
		
		
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();
		
		try {
			String s = (String) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.EXIST, parms);
			assertEquals("SendNow", s);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//change the pattern
		parms.addClientProperty("pattern", "WhatIsIt");
		
		try {
			String s = (String) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.EXIST, parms);
			assertEquals("", s);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	
	/**
	 * test multiple patterns passed into an array
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
	@Test
	public void testMultiCheckPermission() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("action", new String[]{"view"});
		hp.put("resource", new String[]{"button"});
		hp.put("pattern", new String[]{"SendNow|SendLater"});
		
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();

		try {
			String s = (String) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.EXIST, parms);
			assertEquals("SendNow|SendLater", s);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	
	/**
	 * test multiple patterns passed into an array, check the return value has the disallowed patterns removed
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
	@Test
	public void testMultiCheckPermission2() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("action", new String[]{"view"});
		hp.put("resource", new String[]{"button"});
		hp.put("pattern", new String[]{"SendWhenReady|SendNow|SendLater|SendAfterLunch"});
		
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();

		try {
			String s = (String) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.EXIST, parms);
			assertEquals("SendNow|SendLater", s);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}

	/**
	 * test multiple patterns passed into an array, check the return value has the disallowed patterns removed<p>
	 * Same as 2 above but the patterns are passed in an array (redundant parameters in the URL)
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
	@Test
	public void testMultiCheckPermission3() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("action", new String[]{"view"});
		hp.put("resource", new String[]{"button"});
		hp.put("pattern", new String[]{"SendWhenReady","SendNow","SendLater","SendAfterLunch"});
		
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();

		try {
			String s = (String) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.EXIST, parms);
			assertEquals("SendNow|SendLater", s);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	
	/**
	 * test a normal get config transaction e.g.<p>
	 * edit the config file<p>
	 * get a config object<p>
	 * release (unlock) the config file<p>
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
//	@Test
	public void _testConfigAccess() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"true"});
		
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();

		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
//		hp.put("xpath", new String[]{"/authenticate[@id='JOE']"});
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='JOE']/ns:publicCredential/ns:realName".getBytes()))});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.VIEW, parms);
			if (ret instanceof XmlString[]){
				XmlString fragment = ((XmlString[])ret)[0]; 
				String realName = fragment.getStringValue();
				assertEquals("joe doe", realName);
			} 
			else
				fail("another return type:"+ret.getClass());
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//unlock the file
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"false"});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);;
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	
	/**
	 * test a normal get config transaction e.g.<p>
	 * edit the config file<p>
	 * get a config object<p>
	 * release (unlock) the config file<p>
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
//	@Test
	public void _testChangeConfig() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"true"});
	
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();
		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);

		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}		
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
//		hp.put("xpath", new String[]{"/authenticate[@id='JOE']"});
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='JOE']/ns:publicCredential/ns:realName".getBytes()))});
		
		//the value is in the body
		hp.put(Constants.REQUEST_CONTENT, new String[]{"foo bar"});
		
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.POST, hdrprops, "vehr/policy", MethodName.CHANGE, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//retrieve the element and check the value changed!
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='JOE']/ns:publicCredential/ns:realName".getBytes()))});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.VIEW, parms);
			if (ret instanceof XmlString[]){
				XmlString fragment = ((XmlString[])ret)[0]; 
				String realName = fragment.getStringValue();
				assertEquals("foo bar", realName);
			} 
			else
				fail("another return type:"+ret.getClass());
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//unlock the file
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"false"});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);			
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	
	/**
	 * test a normal get config transaction e.g.<p>
	 * edit the config file<p>
	 * get a config object<p>
	 * release (unlock) the config file<p>
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
//	@Test
	public void _testAddNew() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"true"});
	
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();
		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);
			
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}		
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
//		hp.put("xpath", new String[]{"/authenticate[@id='JOE']"});
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:policy".getBytes()))});
		String xmlValue = 
	"<authenticate xmlns=\"http://ehrserver.ethercis.com/policy/1.0\" id=\"FooBar\">"+
		"<publicCredential>"+
			"<realName>foo bar user</realName>"+
		"</publicCredential>"+
		"<privateCredential>"+
			"<id>foobar</id>"+
			"<password>yZEPH6exqDoqk</password>"+
			"<accessManagement>"+
				"<locked></locked>"+
				"<disabled></disabled>"+
			"</accessManagement>"+
			"<passwordManagement></passwordManagement>"+
		"</privateCredential>"+
		"<principal>SYSTEM</principal>"+
	"</authenticate>";
		hp.put(Constants.REQUEST_CONTENT, new String[]{xmlValue});
		
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.POST, hdrprops, "vehr/policy", MethodName.EXTEND, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//retrieve the element and check the value changed!
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='FooBar']/ns:publicCredential/ns:realName".getBytes()))});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.VIEW, parms);
			if (ret instanceof XmlString[]){
				XmlString fragment = ((XmlString[])ret)[0]; 
				String realName = fragment.getStringValue();
				assertEquals("foo bar user", realName);
			} 
			else
				fail("another return type:"+ret.getClass());
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//unlock the file
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"false"});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);
			
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	
	/**
	 * test a normal get config transaction e.g.<p>
	 * edit the config file<p>
	 * get a config object<p>
	 * release (unlock) the config file<p>
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
//	@Test
	public void _testDeleteConfig() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		     ResponseHolder responseHolder = (ResponseHolder)  accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
        SessionClientProperties retprops = responseHolder.getSessionClientProperties();
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//perform a dummy query (should fail on unauthorized...)
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"true"});
	
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();
		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}		
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
//		hp.put("xpath", new String[]{"/authenticate[@id='JOE']"});
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='testUserLocked']".getBytes()))});
		
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.DELETE, parms);
			log.debug("return object of class:"+ret.getClass());
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//retrieve the element and check it's deleted (e.g. not found)
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='testUserLocked']".getBytes()))});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			//we expect an XmlString
			Object ret = accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.VIEW, parms);
			assertTrue(ret instanceof XmlObject[]);
			XmlObject[] retcast = (XmlObject[])ret;
			assertEquals(0, retcast.length);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//unlock the file
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"false"});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	

}

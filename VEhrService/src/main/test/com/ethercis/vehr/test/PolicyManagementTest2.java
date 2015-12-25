//Copyright
package com.ethercis.vehr.test;

import com.ethercis.logonservice.LogonService;
import com.ethercis.logonservice.security.ServiceSecurityManager;
import com.ethercis.logonservice.session.I_SessionManager;
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
import org.apache.xmlbeans.XmlString;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PolicyManagementTest2 extends TestCase {
	private Logger log = Logger.getLogger(PolicyManagementTest2.class);
	private AccessGateService accessGateService;
	private RunTimeSingleton global;
	private SessionClientProperties hdrprops = new SessionClientProperties(global); //empty fake http header props

	
	protected void setUp() throws Exception {
		log.info("Fixture initialized...");
		
		//create a new Authenticate instance
		global = RunTimeSingleton.instance().getClone((new String[]{"-server.security.policy.xml.path","src/test/resources/policy.xml",
																		  "-dispatcher.map", "src/test/resources/dummymap.xml"}));

		//load the security plugin
		ServiceSecurityManager manager = new ServiceSecurityManager();
		manager.init(global, null);

		global.getRunlevelManager().initServiceManagers();
		global.getServiceRegistry().register(Constants.DEFAULT_SERVICE_SECURITY_MANAGER_ID+","+Constants.DEFAULT_SERVICE_SECURITY_MANAGER_VERSION, manager);

		//create a logon service:
		LogonService logonsrv = new LogonService();
		global.getServiceRegistry().register("LogonService,1.0", logonsrv);
		
		//create the dispatcher service...
		I_Service dummy = new DummyService();
		global.getServiceRegistry().register("DUMMY,1.0", dummy);

		RequestDispatcher dispatch = new RequestDispatcher();
		global.getServiceRegistry().register("Dispatcher,1.0", dispatch);
		
		logonsrv.doInit(global, null);
		dispatch.doInit(global, null);
		I_DispatchMapper dispatchMapFactory=new FileDispatchMapper(global,"src/test/resources/dummymap.xml");
		dispatchMapFactory.loadConfiguration(dispatch);
		//dispatch.loadConfiguration("src/test/resources/dummymap.xml");

		accessGateService = new AccessGateService();
		accessGateService.doInit(global, null);
		
		global.getServiceRegistry().register("AEPController,1.0", accessGateService);
		
	}


	/**
	 * test a normal get config transaction e.g.<p>
	 * edit the config file<p>
	 * get a config object<p>
	 * release (unlock) the config file<p>
	 * @throws com.ethercis.servicemanager.exceptions.ServiceManagerException
	 * @throws java.io.IOException
	 */
	@Test
	public void testChangeCommitReverseConfig() throws ServiceManagerException, IOException {
		SessionClientProperties props = new SessionClientProperties(global);
		
		//simple login for user tester (testClient)
		props.addClientProperty(I_SessionManager.USER_ID, "tester");
		props.addClientProperty(I_SessionManager.USER_PASSWORD, "");
		
		SessionClientProperties retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr", MethodName.CONNECT, props);
		
		String sessionName = retprops.getClientProperty(I_SessionManager.SESSION_NAME, (String)null);
		assertNotNull(sessionName);
		
		String sessionid = retprops.getClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), (String)null);
		
		//let's do some real queries now...
		
		SessionClientProperties hdrprops = new SessionClientProperties(global);
		
		//add the session id to validate a query
		hdrprops.addClientProperty(I_SessionManager.SECRET_SESSION_ID(I_ServiceRunMode.DialectSpace.STANDARD), sessionid);
		
		//1. EDIT THE POLICY ----------------------------------------------------------------------
		Map<String, String[]> hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"true"});
	
		HttpParameters util = HttpParameters.getInstance(global, hp);
		SessionClientProperties parms = util.getProperties();
		try {
			accessGateService.queryHandler(MethodName.GET, hdrprops, "vehr/policy", MethodName.GET, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}	
		//2. CHANGE THE POLICY ----------------------------------------------------------------------
		hp = new HashMap<String, String[]>();
//		hp.put("xpath", new String[]{"/authenticate[@id='JOE']"});
		hp.put("xpath", new String[]{new String(Base64.encode("//ns:authenticate[@id='JOE']/ns:publicCredential/ns:realName".getBytes()))});
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
	
		//3. COMMIT THE POLICY ----------------------------------------------------------------------
		try {
			accessGateService.queryHandler(MethodName.toMethodName("GET"), hdrprops, "vehr/policy", MethodName.EXECUTE, new SessionClientProperties(global));
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}		
		
		//4. REVERSE TO THE ORIGINAL POLICY ----------------------------------------------------------------------
		hp = new HashMap<String, String[]>();
		hp.put("command", new String[]{"reverse"});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();
		
		try {
			accessGateService.queryHandler(MethodName.toMethodName("GET"), hdrprops, "vehr/policy", MethodName.EXECUTE, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}		
		
		//5. END TRANSACTION ----------------------------------------------------------------------------
		//unlock the file
		//get an object in config (authenticate user joe, public credential
		hp = new HashMap<String, String[]>();
		hp.put("edit", new String[]{"false"});
		util = HttpParameters.getInstance(global, hp);
		parms = util.getProperties();

		try {
			accessGateService.queryHandler(MethodName.toMethodName("GET"), hdrprops, "vehr/policy", MethodName.GET, parms);
		} catch (ServiceManagerException e){
			fail("The policy check did throw an exception!");
		}
		
		//and disconnect nicely...
		retprops = (SessionClientProperties) accessGateService.queryHandler(MethodName.toMethodName("GET"), hdrprops, "vehr", MethodName.DISCONNECT, parms);
		
	}
	

}

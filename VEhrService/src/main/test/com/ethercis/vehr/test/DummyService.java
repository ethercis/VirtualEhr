//Copyright
package com.ethercis.vehr.test;


import com.ethercis.servicemanager.annotation.RunLevelAction;
import com.ethercis.servicemanager.annotation.RunLevelActions;
import com.ethercis.servicemanager.annotation.Service;
import com.ethercis.servicemanager.cluster.RunTimeSingleton;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.cluster.ContextNode;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;
import com.ethercis.servicemanager.common.I_SessionClientProperties;
import com.ethercis.servicemanager.common.SessionClientProperties;
import com.ethercis.servicemanager.common.def.Constants;
import org.apache.log4j.Logger;

/**
 * Dummy service for Dispatcher test
 * @author christian
 *
 */

@Service(id = "DUMMY",system=true)
@RunLevelActions(value = {
		@RunLevelAction(onStartupRunlevel = 7, sequence = 3, action = "LOAD"),
		@RunLevelAction(onShutdownRunlevel = 8, sequence = 5, action = "STOP") })


public class DummyService extends ClusterInfo {

	private static Logger log = Logger.getLogger(Constants.LOGGER_SYSTEM);

	private ContextNode contextNode;
	private RunTimeSingleton glob;
	private int errors = 0;
	private String lasterror;	
	
	private String ME="DUMMY";
	
	private boolean initiated = false;
	
	public void doInit(RunTimeSingleton global, ServiceInfo serviceInfo) throws ServiceManagerException {
		this.glob = global;
        //add JMX details...
//        putObject(I_Info.JMX_PREFIX+"DummyProvider", this);
	}

	public String method1(I_SessionClientProperties parms){
		return "method1";
	}

	public String method2(I_SessionClientProperties parms){
		//check for content (x-request-content)
		String content = parms.getClientProperty("x-request-content", (String)null);
		String type = parms.getClientProperty("x-request-content-type", "");
		int contentLength = parms.getClientProperty("x-request-content-length", 0);
		System.out.println("Got content type:"+type+", length:"+contentLength);
		if (content != null)
			return content;
		else
			return "method2";
	}
	
	public String method3(I_SessionClientProperties parms){
		//expected to receive a string and base64 encoded integer
		String s = parms.getClientProperty("s", "");
		Integer i = parms.getClientProperty("i", 0);
		
		return s+i;
	}
	
	public String method4(String s, Integer i, Long l){
		return "method4";
	}
	
	public String method5(SessionClientProperties props){
		String ret;
		
		ret = props.writePropertiesXml("");
		
		return ret;
	}
	
	public String getType() {
		return ME;
	}

	public String getVersion() {
		return "1.0";
	}

}

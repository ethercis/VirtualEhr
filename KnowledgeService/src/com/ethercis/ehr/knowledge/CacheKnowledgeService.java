package com.ethercis.ehr.knowledge;

import com.ethercis.servicemanager.cluster.ClusterController;
import com.ethercis.servicemanager.cluster.ClusterInfo;
import com.ethercis.servicemanager.exceptions.ServiceManagerException;
import com.ethercis.servicemanager.service.ServiceInfo;
import org.apache.log4j.Logger;

public class CacheKnowledgeService extends ClusterInfo implements I_CacheKnowledgeService {
	
	private String ME="CacheKnowledgeService";
	private String version="1.0";
	private Logger log = Logger.getLogger(ME);
	private ClusterController global;
	private I_KnowledgeCache cache;

	public CacheKnowledgeService() {

	}	
	
	/* (non-Javadoc)
	 * @see com.ethercis.ehr.cache.I_ResourceService#getKnowledgeCache()
	 */
	@Override
	public I_KnowledgeCache getKnowledgeCache(){
		return cache;
	}

	@Override
	protected void doInit(ClusterController global, ServiceInfo serviceInfo)
			throws ServiceManagerException {
        log.info("Starting "+ME+" version="+version);
		this.global = global;
		
		//initialize the ArchetypeRepository controller
		this.cache = new KnowledgeCache(serviceInfo.getParameters());

        log.info(ME+" successfully started");
	}
	
}

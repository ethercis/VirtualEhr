//Copyright
package com.ethercis.servicemanager.cluster;

import org.junit.Test;

public class ClusterLoaderTest {
	
	@Test
	public void testInit() {
		String args[] = {"-propertyFile","services.properties",
						 "-servicesFile","services.xml",
						 "-property.verbose", "2"};
		RunTimeSingleton ctrl = new RunTimeSingleton(args);
		ctrl.setId("4Test");
		ClusterLoader loader = new ClusterLoader(ctrl);
        loader.getName();
	}

	
}
